package com.darkjade.streamlib.data.metadata

import com.darkjade.streamlib.data.db.entity.MediaType

/** Provider-agnostic metadata result. UI/DB never talk to TMDB directly. */
data class MetadataResult(
    val remoteId: String? = null,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double?,
    val ageRating: String?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val director: String? = null,
    val cast: List<String> = emptyList(),
    val seasons: List<SeasonMetadata> = emptyList(),
)

data class SeasonMetadata(
    val seasonNumber: Int,
    val name: String?,
    val posterUrl: String?,
    val episodes: List<EpisodeMetadata> = emptyList(),
)

data class EpisodeMetadata(
    val episodeNumber: Int,
    val title: String?,
    val overview: String?,
    val thumbnailUrl: String?,
    val runtimeMinutes: Int?,
)

/**
 * Abstraction over any metadata backend (TMDB, OMDb, custom, offline).
 * Swap implementations without touching repositories, DB, or UI.
 */
interface MetadataProvider {
    suspend fun searchMovie(title: String, year: Int?): MetadataResult?
    suspend fun searchSeries(title: String, year: Int?): MetadataResult?
    suspend fun getSeasonDetails(seriesRemoteId: String, seasonNumber: Int): SeasonMetadata?
}

/** Metadata lookups can legitimately fail (offline, rate-limited, no match). */
sealed class MetadataOutcome {
    data class Success(val result: MetadataResult) : MetadataOutcome()
    object NotFound : MetadataOutcome()
    data class Error(val message: String) : MetadataOutcome()
}

fun MediaType.isSeriesLike(): Boolean = this == MediaType.SERIES || this == MediaType.ANIME
