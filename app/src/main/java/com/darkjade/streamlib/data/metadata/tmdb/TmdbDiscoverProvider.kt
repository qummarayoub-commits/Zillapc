package com.darkjade.streamlib.data.metadata.tmdb

import android.content.Context
import android.util.Log
import com.darkjade.streamlib.data.db.entity.MediaType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
 * One poster-sized entry in a release row: a movie, a brand-new series, or a
 * newly released season of an existing series.
 *
 * Provider-agnostic on purpose (no TMDB DTOs leak out), same idea as
 * MetadataResult — UI never talks to TMDB types directly.
 */
data class NewReleaseItem(
    val key: String,            // stable unique key, e.g. "movie-123" / "tv-45-s2"
    val tmdbId: String,
    val type: MediaType,        // MOVIE or SERIES
    val title: String,
    val releaseDate: String?,   // yyyy-MM-dd, null when only the year is known
    val year: Int?,
    val seasonNumber: Int? = null, // set for series entries (season 1 = "New Series")
    val label: String? = null,     // "New Series" / "Season 3" — null for movies
    val posterUrl: String?,
    val overview: String?,
    val rating: Double?,
) {
    /** Sortable "newest first" key that also works when only the year is known. */
    val sortKey: String get() = releaseDate ?: "${year ?: 0}-00-00"
}

/** A themed row, e.g. "New on Netflix" or "New Family Fun". */
data class ReleaseCollection(
    val id: String,
    val title: String,
    val items: List<NewReleaseItem>,
)

data class NewReleasesBundle(
    val all: List<NewReleaseItem> = emptyList(),          // everything this year + last year
    val collections: List<ReleaseCollection> = emptyList(),
)

/** When a season of a series premiered — used to find new seasons of shows the user already owns. */
data class SeasonStamp(val number: Int, val airDate: String)

private data class NewReleasesCache(
    val fetchedAtMs: Long = 0L,
    val windowStart: String = "",
    val bundle: NewReleasesBundle? = null,
)

private data class SeasonLookup(
    val fetchedAtMs: Long = 0L,
    val seasons: List<SeasonStamp> = emptyList(),
)

/** How to build one themed row. */
private data class CollectionSpec(
    val id: String,
    val title: String,
    val daysBack: Int,
    val movieExtra: Map<String, String> = emptyMap(),
    val tvExtra: Map<String, String> = emptyMap(),
    val moviePages: Int = 1,
    val seriesPages: Int = 1,
    val seasonLookups: Int = 0,   // how many continuing shows to check for freshly premiered seasons
    val maxItems: Int = 24,
)

/**
 * Pulls everything released in the CURRENT year and the PREVIOUS year from
 * TMDB (movies, brand-new series, new seasons of older series) plus themed
 * rows (this week, streaming services, family, anime...).
 *
 * Deliberately a separate class from [TmdbMetadataProvider] so the working
 * per-title metadata pipeline stays untouched. Results are cached on disk;
 * if the network fails we fall back to the stale cache (or an empty bundle)
 * — this must never crash the Home screen. The API key is never logged.
 */
class TmdbDiscoverProvider(context: Context) {

    private val cacheFile = File(context.cacheDir, "new_releases_cache_v2.json")
    private val seasonsCacheFile = File(context.cacheDir, "local_series_seasons_v1.json")
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

    /** Newest first. Empty bundle if there's no API key and no cache. */
    suspend fun fetchBundle(forceRefresh: Boolean = false): NewReleasesBundle =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val windowStart = LocalDate.of(today.year - 1, 1, 1).toString()
            val cached = readCache()
            val cachedBundle = cached?.takeIf { it.windowStart == windowStart }?.bundle

            if (cachedBundle != null && !forceRefresh &&
                System.currentTimeMillis() - (cached?.fetchedAtMs ?: 0L) < CACHE_TTL_MS
            ) {
                return@withContext cachedBundle
            }

            val key = TmdbConfig.apiKey
            if (key.isBlank()) return@withContext cachedBundle ?: NewReleasesBundle()

            try {
                val fresh = loadBundle(key, windowStart, today)
                if (fresh.all.isEmpty() && fresh.collections.isEmpty()) {
                    // Every call failed or returned nothing — don't overwrite a good cache with emptiness.
                    return@withContext cachedBundle ?: fresh
                }
                writeCache(NewReleasesCache(System.currentTimeMillis(), windowStart, fresh))
                fresh
            } catch (e: Exception) {
                Log.w(TAG, "fetchBundle failed (${e.javaClass.simpleName}); using ${if (cachedBundle != null) "stale cache" else "empty bundle"}")
                cachedBundle ?: NewReleasesBundle()
            }
        }

    /**
     * For series the user already owns: when did each of their seasons premiere?
     * Lets the app mark "Season 3 (2026)" as available even if that season is
     * nowhere in TMDB's popularity-sorted discover lists. Cached per series.
     */
    suspend fun seasonStampsFor(tmdbIds: List<String>): Map<String, List<SeasonStamp>> =
        withContext(Dispatchers.IO) {
            if (tmdbIds.isEmpty()) return@withContext emptyMap()
            val cache = readSeasonsCache().toMutableMap()
            val now = System.currentTimeMillis()
            val key = TmdbConfig.apiKey

            val stale = tmdbIds.distinct().filter { id ->
                val c = cache[id]
                c == null || now - c.fetchedAtMs > SEASONS_TTL_MS
            }.take(MAX_SEASON_LOOKUPS_PER_RUN)

            if (stale.isNotEmpty() && key.isNotBlank()) {
                val gate = Semaphore(PARALLELISM)
                coroutineScope {
                    stale.map { id ->
                        async {
                            val intId = id.toIntOrNull() ?: return@async
                            try {
                                val detail = gate.withPermit { api.getSeriesSeasons(intId, key) }
                                val stamps = detail.seasons.orEmpty()
                                    .filter { it.season_number > 0 && !it.air_date.isNullOrBlank() }
                                    .map { SeasonStamp(it.season_number, it.air_date!!) }
                                synchronized(cache) { cache[id] = SeasonLookup(now, stamps) }
                            } catch (e: Exception) {
                                Log.w(TAG, "season stamps lookup failed (${e.javaClass.simpleName})")
                            }
                        }
                    }.forEach { it.await() }
                }
                writeSeasonsCache(cache)
            }
            tmdbIds.distinct().mapNotNull { id -> cache[id]?.let { id to it.seasons } }.toMap()
        }

    // ------------------------------------------------------------------ loading

    private suspend fun loadBundle(apiKey: String, windowStart: String, today: LocalDate): NewReleasesBundle =
        coroutineScope {
            val gate = Semaphore(PARALLELISM)
            val to = today.toString()

            val allJob = async {
                loadWindow(
                    apiKey, windowStart, to, emptyMap(), emptyMap(),
                    moviePages = 3, seriesPages = 2, seasonLookups = 40, gate = gate,
                )
            }
            val specJobs = specs().map { spec ->
                spec to async {
                    loadWindow(
                        apiKey, today.minusDays(spec.daysBack.toLong()).toString(), to,
                        spec.movieExtra, spec.tvExtra,
                        spec.moviePages, spec.seriesPages, spec.seasonLookups, gate,
                    ).take(spec.maxItems)
                }
            }

            val all = allJob.await()
            val built = specJobs.map { (spec, job) -> ReleaseCollection(spec.id, spec.title, job.await()) }

            // "This week" is carved out of the 30-day row so it costs no extra requests.
            val weekStart = today.minusDays(7).toString()
            val month = built.firstOrNull { it.id == "this_month" }?.items.orEmpty()
            val thisWeek = ReleaseCollection(
                "this_week", "New Releases This Week",
                month.filter { (it.releaseDate ?: "") >= weekStart },
            )

            NewReleasesBundle(
                all = all,
                collections = (listOf(thisWeek) + built).filter { it.items.isNotEmpty() },
            )
        }

    private fun specs(): List<CollectionSpec> {
        fun streaming(providerId: Int) = mapOf(
            "watch_region" to "US",
            "with_watch_providers" to providerId.toString(),
            "with_watch_monetization_types" to "flatrate",
        )
        return listOf(
            CollectionSpec("this_month", "New This Month", daysBack = 30, moviePages = 2, seriesPages = 1, seasonLookups = 15, maxItems = 60),
            CollectionSpec("netflix", "New on Netflix", 180, streaming(8), streaming(8), seasonLookups = 8),
            CollectionSpec("prime", "New on Prime Video", 180, streaming(9), streaming(9), seasonLookups = 8),
            CollectionSpec("disney", "New on Disney+", 180, streaming(337), streaming(337), seasonLookups = 8),
            CollectionSpec("apple", "New on Apple TV+", 180, streaming(350), streaming(350), seasonLookups = 6),
            CollectionSpec(
                "family", "New Family Fun", 365,
                movieExtra = mapOf("with_genres" to "10751"),
                tvExtra = mapOf("with_genres" to "10751|10762"),
            ),
            CollectionSpec(
                "anime", "New Anime", 365,
                movieExtra = mapOf("with_genres" to "16", "with_original_language" to "ja"),
                tvExtra = mapOf("with_genres" to "16", "with_original_language" to "ja"),
                seasonLookups = 8,
            ),
            CollectionSpec(
                "action", "New Action & Adventure", 365,
                movieExtra = mapOf("with_genres" to "28|12"),
                tvExtra = mapOf("with_genres" to "10759"),
            ),
        )
    }

    /** One date window of releases: movies + brand-new series + freshly premiered seasons. */
    private suspend fun loadWindow(
        apiKey: String,
        from: String,
        to: String,
        movieExtra: Map<String, String>,
        tvExtra: Map<String, String>,
        moviePages: Int,
        seriesPages: Int,
        seasonLookups: Int,
        gate: Semaphore,
    ): List<NewReleaseItem> = coroutineScope {
        // Each piece fails independently — one bad request must not blank the whole row.
        suspend fun <T> safely(block: suspend () -> List<T>): List<T> = try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "discover request failed (${e.javaClass.simpleName})")
            emptyList()
        }

        val moviesJob = async {
            (1..moviePages).flatMap { page ->
                safely { gate.withPermit { api.discoverMovies(apiKey, from, to, page = page, extra = movieExtra).results } }
            }
        }
        val newSeriesJob = async {
            (1..seriesPages).flatMap { page ->
                safely { gate.withPermit { api.discoverNewSeries(apiKey, from, to, page = page, extra = tvExtra).results } }
            }
        }
        val airingJob = async {
            if (seasonLookups <= 0) emptyList()
            else (1..seriesPages).flatMap { page ->
                safely { gate.withPermit { api.discoverAiringSeries(apiKey, from, to, page = page, extra = tvExtra).results } }
            }
        }

        val movies = moviesJob.await()
        val newSeries = newSeriesJob.await()
        val airing = airingJob.await()

        val newSeriesIds = newSeries.map { it.id }.toSet()
        val continuing = airing.filter { it.id !in newSeriesIds }.distinctBy { it.id }.take(seasonLookups)

        val seasonItems = continuing.map { show ->
            async {
                safely {
                    val detail = gate.withPermit { api.getSeriesSeasons(show.id, apiKey) }
                    detail.seasons.orEmpty()
                        .filter { it.season_number > 0 && it.air_date != null && it.air_date >= from && it.air_date <= to }
                        .map { season -> show.toSeasonItem(season) }
                }
            }
        }.flatMap { it.await() }

        (movies.mapNotNull { it.toReleaseItem() } + newSeries.mapNotNull { it.toNewSeriesItem() } + seasonItems)
            .distinctBy { it.key }
            .sortedByDescending { it.sortKey }
    }

    // ------------------------------------------------------------------ mapping

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

    // ------------------------------------------------------------------ disk caches

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

    private fun readSeasonsCache(): Map<String, SeasonLookup> = try {
        if (seasonsCacheFile.exists()) {
            val type = object : TypeToken<Map<String, SeasonLookup>>() {}.type
            gson.fromJson<Map<String, SeasonLookup>>(seasonsCacheFile.readText(), type) ?: emptyMap()
        } else emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }

    private fun writeSeasonsCache(cache: Map<String, SeasonLookup>) {
        try {
            seasonsCacheFile.writeText(gson.toJson(cache))
        } catch (e: Exception) {
            Log.w(TAG, "could not write seasons cache (${e.javaClass.simpleName})")
        }
    }

    private companion object {
        const val TAG = "TmdbDiscover"
        const val CACHE_TTL_MS = 12L * 60 * 60 * 1000          // 12 hours
        const val SEASONS_TTL_MS = 3L * 24 * 60 * 60 * 1000    // 3 days
        const val MAX_SEASON_LOOKUPS_PER_RUN = 80
        const val PARALLELISM = 8
    }
}
