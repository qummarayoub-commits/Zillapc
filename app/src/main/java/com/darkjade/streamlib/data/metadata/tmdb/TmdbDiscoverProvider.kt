package com.darkjade.streamlib.data.metadata.tmdb

import android.content.Context
import android.util.Log
import com.darkjade.streamlib.data.db.entity.MediaType
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * One poster-sized entry in the "New Releases" rail: a movie, a brand-new
 * series, or a newly released season of an existing series.
 *
 * Provider-agnostic on purpose (no TMDB DTOs leak out), same idea as
 * MetadataResult — UI never talks to TMDB types directly.
 */
data class NewReleaseItem(
    val key: String,            // stable unique key, e.g. "movie-123" / "tv-45-s2"
    val tmdbId: String,
    val type: MediaType,        // MOVIE or SERIES
    val title: String,
    val releaseDate: String?,   // yyyy-MM-dd
    val year: Int?,
    val seasonNumber: Int? = null, // set for series entries (season 1 = "New Series")
    val label: String? = null,     // "New Series" / "Season 3" — null for movies
    val posterUrl: String?,
    val overview: String?,
    val rating: Double?,
)

/** What we persist to disk so the rail still shows when offline / on a slow start. */
private data class NewReleasesCache(
    val fetchedAtMs: Long = 0L,
    val windowStart: String = "",
    val items: List<NewReleaseItem> = emptyList(),
)

/**
 * Pulls everything released in the CURRENT year and the PREVIOUS year from
 * TMDB: movies, brand-new series, and new seasons of older series.
 *
 * Deliberately a separate class from [TmdbMetadataProvider] so the working
 * per-title metadata pipeline stays untouched. Results are cached on disk
 * for [CACHE_TTL_MS]; if the network call fails we fall back to the stale
 * cache (or an empty list) — this must never crash the Home screen.
 */
class TmdbDiscoverProvider(context: Context) {

    private val cacheFile = File(context.cacheDir, "new_releases_cache.json")
    private val gson = Gson()

    private val api: TmdbApi = Retrofit.Builder()
        .baseUrl(TmdbConfig.BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    /** Newest first. Empty list if there's no API key and no cache. */
    suspend fun fetchNewReleases(forceRefresh: Boolean = false): List<NewReleaseItem> =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val windowStart = LocalDate.of(today.year - 1, 1, 1).toString()
            val cached = readCache()

            val cacheUsable = cached != null && cached.windowStart == windowStart
            if (cacheUsable && !forceRefresh && System.currentTimeMillis() - cached!!.fetchedAtMs < CACHE_TTL_MS) {
                return@withContext cached.items
            }

            val key = TmdbConfig.apiKey
            if (key.isBlank()) return@withContext if (cacheUsable) cached!!.items else emptyList()

            try {
                val fresh = loadFromNetwork(key, windowStart, today.toString())
                writeCache(NewReleasesCache(System.currentTimeMillis(), windowStart, fresh))
                fresh
            } catch (e: Exception) {
                // Never expose the API key: log only the exception type/message from our own code path.
                Log.w(TAG, "fetchNewReleases failed (${e.javaClass.simpleName}); using ${if (cacheUsable) "stale cache" else "empty list"}")
                if (cacheUsable) cached!!.items else emptyList()
            }
        }

    private suspend fun loadFromNetwork(apiKey: String, from: String, to: String): List<NewReleaseItem> =
        coroutineScope {
            // Three independent lookups run in parallel.
            val moviesJob = async {
                (1..MOVIE_PAGES).flatMap { page ->
                    api.discoverMovies(apiKey, from, to, page = page).results
                }
            }
            val newSeriesJob = async {
                (1..SERIES_PAGES).flatMap { page ->
                    api.discoverNewSeries(apiKey, from, to, page = page).results
                }
            }
            val airingJob = async {
                (1..SERIES_PAGES).flatMap { page ->
                    api.discoverAiringSeries(apiKey, from, to, page = page).results
                }
            }

            val movies = moviesJob.await()
            val newSeries = newSeriesJob.await()
            val airing = airingJob.await()

            val movieItems = movies.mapNotNull { it.toReleaseItem() }

            val newSeriesItems = newSeries.mapNotNull { it.toNewSeriesItem() }
            val newSeriesIds = newSeries.map { it.id }.toSet()

            // Older shows that aired something inside the window: ask TMDB which
            // of their SEASONS actually premiered in the window. Capped parallelism
            // so we don't hammer the API (and TMDB's rate limit).
            val continuing = airing.filter { it.id !in newSeriesIds }.distinctBy { it.id }
            val gate = Semaphore(SEASON_LOOKUP_PARALLELISM)
            val seasonItems = continuing.map { show ->
                async {
                    gate.withPermit {
                        try {
                            val detail = api.getSeriesSeasons(show.id, apiKey)
                            detail.seasons.orEmpty()
                                .filter { it.season_number > 0 && it.air_date != null && it.air_date >= from && it.air_date <= to }
                                .map { season -> show.toSeasonItem(season) }
                        } catch (e: Exception) {
                            Log.w(TAG, "season lookup failed for tv/${show.id} (${e.javaClass.simpleName})")
                            emptyList()
                        }
                    }
                }
            }.flatMap { it.await() }

            (movieItems + newSeriesItems + seasonItems)
                .distinctBy { it.key }
                .sortedByDescending { it.releaseDate.orEmpty() }
        }

    private fun TmdbMovieDto.toReleaseItem(): NewReleaseItem? {
        val displayTitle = title ?: original_title ?: return null
        val date = release_date?.takeIf { it.isNotBlank() }
        return NewReleaseItem(
            key = "movie-$id",
            tmdbId = id.toString(),
            type = MediaType.MOVIE,
            title = displayTitle,
            releaseDate = date,
            year = date?.take(4)?.toIntOrNull(),
            posterUrl = poster_path?.let { TmdbConfig.IMAGE_BASE_URL_SMALL + it },
            overview = overview,
            rating = vote_average,
        )
    }

    private fun TmdbSeriesDto.toNewSeriesItem(): NewReleaseItem? {
        val displayTitle = name ?: original_name ?: return null
        val date = first_air_date?.takeIf { it.isNotBlank() }
        return NewReleaseItem(
            key = "tv-$id-s1",
            tmdbId = id.toString(),
            type = MediaType.SERIES,
            title = displayTitle,
            releaseDate = date,
            year = date?.take(4)?.toIntOrNull(),
            seasonNumber = 1,
            label = "New Series",
            posterUrl = poster_path?.let { TmdbConfig.IMAGE_BASE_URL_SMALL + it },
            overview = overview,
            rating = vote_average,
        )
    }

    private fun TmdbSeriesDto.toSeasonItem(season: TmdbSeriesSeasonDto): NewReleaseItem {
        val displayTitle = name ?: original_name ?: "Unknown"
        val date = season.air_date
        return NewReleaseItem(
            key = "tv-$id-s${season.season_number}",
            tmdbId = id.toString(),
            type = MediaType.SERIES,
            title = displayTitle,
            releaseDate = date,
            year = date?.take(4)?.toIntOrNull(),
            seasonNumber = season.season_number,
            label = "Season ${season.season_number}",
            // Season poster when TMDB has one, otherwise the show's poster.
            posterUrl = (season.poster_path ?: poster_path)?.let { TmdbConfig.IMAGE_BASE_URL_SMALL + it },
            overview = overview,
            rating = vote_average,
        )
    }

    private fun readCache(): NewReleasesCache? = try {
        if (cacheFile.exists()) gson.fromJson(cacheFile.readText(), NewReleasesCache::class.java) else null
    } catch (e: Exception) {
        null // corrupt cache — just refetch
    }

    private fun writeCache(cache: NewReleasesCache) {
        try {
            cacheFile.writeText(gson.toJson(cache))
        } catch (e: Exception) {
            Log.w(TAG, "could not write new-releases cache (${e.javaClass.simpleName})")
        }
    }

    private companion object {
        const val TAG = "TmdbDiscover"
        const val CACHE_TTL_MS = 12L * 60 * 60 * 1000 // 12 hours
        const val MOVIE_PAGES = 3          // ~60 most popular movies in the window
        const val SERIES_PAGES = 2         // ~40 per series query
        const val SEASON_LOOKUP_PARALLELISM = 6
    }
}
