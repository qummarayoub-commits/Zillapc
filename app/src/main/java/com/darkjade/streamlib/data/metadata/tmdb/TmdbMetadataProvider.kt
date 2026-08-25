package com.darkjade.streamlib.data.metadata.tmdb

import com.darkjade.streamlib.data.metadata.EpisodeMetadata
import com.darkjade.streamlib.data.metadata.MetadataProvider
import com.darkjade.streamlib.data.metadata.MetadataResult
import com.darkjade.streamlib.data.metadata.CastMember
import com.darkjade.streamlib.data.metadata.SeasonMetadata
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * TMDB-backed implementation of [MetadataProvider].
 *
 * The API key is loaded from local.properties (TMDB_API_KEY) at build time and
 * injected into TmdbConfig.apiKey during Application.onCreate() — see
 * StreamLibApp.kt and app/build.gradle.kts. It is never hardcoded here or
 * committed to git. If the key is ever missing, every call safely returns
 * null and the app falls back to the local-only card, per Phase 5
 * requirements — the app must never crash when metadata is unavailable.
 */
object TmdbConfig {
    const val BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w780"
    // "original" for backdrops — hero banners/details backgrounds are shown
    // large, and w1280 was noticeably softer than Crunchyroll-style HD art.
    const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/original"
    // Cast profile photos — w185 is plenty for a small circular avatar; no need for full HD here.
    const val PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w185"

    /** Set this at app startup (e.g. from Settings > Metadata) once you have a key. */
    var apiKey: String = ""
}

class TmdbMetadataProvider : MetadataProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(TmdbConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(TmdbApi::class.java)

    private fun extractDirector(credits: TmdbCreditsDto?): String? =
        credits?.crew?.firstOrNull { it.job.equals("Director", ignoreCase = true) }?.name

    private fun extractCast(credits: TmdbCreditsDto?, limit: Int = 6): List<String> =
        credits?.cast.orEmpty().sortedBy { it.order }.take(limit).map { it.name }

    private fun extractCastMembers(credits: TmdbCreditsDto?, limit: Int = 12): List<CastMember> =
        credits?.cast.orEmpty().sortedBy { it.order }.take(limit).map {
            CastMember(
                name = it.name,
                character = it.character?.takeIf { c -> c.isNotBlank() },
                photoUrl = it.profile_path?.let { p -> TmdbConfig.PROFILE_BASE_URL + p },
            )
        }

    private fun extractTrailerKey(videos: TmdbVideosDto?): String? =
        videos?.results.orEmpty()
            .filter { it.site == "YouTube" && it.type == "Trailer" }
            .sortedByDescending { it.official }
            .firstOrNull()?.key

    // Prefer the viewer's likely region (US) certification since it's the
    // most widely recognized rating system; fall back to any country that
    // has one rather than showing nothing.
    private fun extractMovieCertification(releaseDates: TmdbReleaseDatesDto?): String? {
        val countries = releaseDates?.results.orEmpty()
        val us = countries.firstOrNull { it.iso_3166_1 == "US" }
        val chosen = us ?: countries.firstOrNull { it.release_dates.any { d -> d.certification.isNotBlank() } }
        return chosen?.release_dates?.firstOrNull { it.certification.isNotBlank() }?.certification
    }

    private fun extractSeriesCertification(contentRatings: TmdbContentRatingsDto?): String? {
        val countries = contentRatings?.results.orEmpty()
        val us = countries.firstOrNull { it.iso_3166_1 == "US" && it.rating.isNotBlank() }
        return (us ?: countries.firstOrNull { it.rating.isNotBlank() })?.rating
    }

    private fun extractAlternatePosters(images: TmdbImagesDto?, limit: Int = 5): List<String> =
        images?.posters.orEmpty().take(limit).map { TmdbConfig.IMAGE_BASE_URL + it.file_path }

    override suspend fun searchMovie(title: String, year: Int?): MetadataResult? {
        if (TmdbConfig.apiKey.isBlank()) return null
        return runCatching {
            val response = api.searchMovie(TmdbConfig.apiKey, title, year)
            val match = response.results.firstOrNull() ?: return null
            val full = api.getMovie(match.id, TmdbConfig.apiKey)
            movieDtoToResult(full, title)
        }.getOrNull()
    }

    override suspend fun searchSeries(title: String, year: Int?): MetadataResult? {
        if (TmdbConfig.apiKey.isBlank()) return null
        return runCatching {
            val response = api.searchSeries(TmdbConfig.apiKey, title, year)
            val match = response.results.firstOrNull() ?: return null
            val full = api.getSeries(match.id, TmdbConfig.apiKey)
            seriesDtoToResult(full, title)
        }.getOrNull()
    }

    override suspend fun searchCandidates(query: String, isSeries: Boolean): List<com.darkjade.streamlib.data.metadata.SearchCandidate> {
        if (TmdbConfig.apiKey.isBlank() || query.isBlank()) return emptyList()
        return runCatching {
            if (isSeries) {
                api.searchSeries(TmdbConfig.apiKey, query, null).results.take(15).map {
                    com.darkjade.streamlib.data.metadata.SearchCandidate(
                        remoteId = it.id.toString(),
                        title = it.name ?: query,
                        year = it.first_air_date?.take(4)?.toIntOrNull(),
                        posterUrl = it.poster_path?.let { p -> TmdbConfig.IMAGE_BASE_URL + p },
                    )
                }
            } else {
                api.searchMovie(TmdbConfig.apiKey, query, null).results.take(15).map {
                    com.darkjade.streamlib.data.metadata.SearchCandidate(
                        remoteId = it.id.toString(),
                        title = it.title ?: query,
                        year = it.release_date?.take(4)?.toIntOrNull(),
                        posterUrl = it.poster_path?.let { p -> TmdbConfig.IMAGE_BASE_URL + p },
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun getByRemoteId(remoteId: String, isSeries: Boolean): MetadataResult? {
        if (TmdbConfig.apiKey.isBlank()) return null
        val id = remoteId.toIntOrNull() ?: return null
        return runCatching {
            if (isSeries) {
                seriesDtoToResult(api.getSeries(id, TmdbConfig.apiKey), null)
            } else {
                movieDtoToResult(api.getMovie(id, TmdbConfig.apiKey), null)
            }
        }.getOrNull()
    }

    private fun movieDtoToResult(full: TmdbMovieDto, fallbackTitle: String?): MetadataResult = MetadataResult(
        remoteId = full.id.toString(),
        title = full.title ?: fallbackTitle ?: "Untitled",
        originalTitle = full.original_title,
        overview = full.overview,
        posterUrl = full.poster_path?.let { TmdbConfig.IMAGE_BASE_URL + it },
        backdropUrl = full.backdrop_path?.let { TmdbConfig.BACKDROP_BASE_URL + it },
        rating = full.vote_average,
        ageRating = extractMovieCertification(full.release_dates),
        runtimeMinutes = full.runtime,
        genres = full.genres?.map { it.name } ?: emptyList(),
        director = extractDirector(full.credits),
        cast = extractCast(full.credits),
        castMembers = extractCastMembers(full.credits),
        alternatePosterUrls = extractAlternatePosters(full.images),
        trailerYoutubeKey = extractTrailerKey(full.videos),
        imdbId = full.external_ids?.imdb_id,
        productionCountry = full.production_countries?.firstOrNull()?.name,
        originalLanguage = full.original_language?.let { code -> runCatching { java.util.Locale(code).displayLanguage.takeIf { it.isNotBlank() && !it.equals(code, true) } }.getOrNull() },
    )

    private fun seriesDtoToResult(full: TmdbSeriesDto, fallbackTitle: String?): MetadataResult = MetadataResult(
        remoteId = full.id.toString(),
        title = full.name ?: fallbackTitle ?: "Untitled",
        originalTitle = full.original_name,
        overview = full.overview,
        posterUrl = full.poster_path?.let { TmdbConfig.IMAGE_BASE_URL + it },
        backdropUrl = full.backdrop_path?.let { TmdbConfig.BACKDROP_BASE_URL + it },
        rating = full.vote_average,
        ageRating = extractSeriesCertification(full.content_ratings),
        runtimeMinutes = null,
        genres = full.genres?.map { it.name } ?: emptyList(),
        director = extractDirector(full.credits),
        cast = extractCast(full.credits),
        castMembers = extractCastMembers(full.credits),
        alternatePosterUrls = extractAlternatePosters(full.images),
        trailerYoutubeKey = extractTrailerKey(full.videos),
        imdbId = full.external_ids?.imdb_id,
        productionCountry = full.production_countries?.firstOrNull()?.name,
        originalLanguage = full.original_language?.let { code -> runCatching { java.util.Locale(code).displayLanguage.takeIf { it.isNotBlank() && !it.equals(code, true) } }.getOrNull() },
        seasonCount = full.number_of_seasons,
        episodeCount = full.number_of_episodes,
        status = full.status,
        seasons = emptyList(), // fetched lazily via getSeasonDetails to save API calls
    )

    override suspend fun getSeasonDetails(seriesRemoteId: String, seasonNumber: Int): SeasonMetadata? {
        if (TmdbConfig.apiKey.isBlank()) return null
        val id = seriesRemoteId.toIntOrNull() ?: return null
        return runCatching {
            val season = api.getSeason(id, seasonNumber, TmdbConfig.apiKey)
            SeasonMetadata(
                seasonNumber = season.season_number,
                name = season.name,
                posterUrl = season.poster_path?.let { TmdbConfig.IMAGE_BASE_URL + it },
                episodes = season.episodes.map {
                    EpisodeMetadata(
                        episodeNumber = it.episode_number,
                        title = it.name,
                        overview = it.overview,
                        thumbnailUrl = it.still_path?.let { p -> TmdbConfig.IMAGE_BASE_URL + p },
                        runtimeMinutes = it.runtime,
                    )
                }
            )
        }.getOrNull()
    }
}
