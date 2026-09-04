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
    val castMembers: List<CastMember> = emptyList(),
    val alternatePosterUrls: List<String> = emptyList(),
    val alternateBackdropUrls: List<String> = emptyList(),
    val titleLogoUrl: String? = null, // TMDB's official stylized title-logo artwork, when available
    val trailerYoutubeKey: String? = null,
    val imdbId: String? = null,
    val productionCountry: String? = null,
    val originalLanguage: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val status: String? = null,
    val seasons: List<SeasonMetadata> = emptyList(),
)

data class CastMember(
    val name: String,
    val character: String?,
    val photoUrl: String?,
)

/** A lightweight search result for the manual "fix metadata" picker — just
 * enough to show in a list and let the user pick the right match. */
data class SearchCandidate(
    val remoteId: String,
    val title: String,
    val year: Int?,
    val posterUrl: String?,
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
    /** Manual "fix metadata" flow: search returns candidates for the user to pick from. */
    suspend fun searchCandidates(query: String, isSeries: Boolean): List<SearchCandidate>
    /** Fetches full details for a specific already-known remote id (the user's chosen candidate). */
    suspend fun getByRemoteId(remoteId: String, isSeries: Boolean): MetadataResult?
}

/** Metadata lookups can legitimately fail (offline, rate-limited, no match). */
sealed class MetadataOutcome {
    data class Success(val result: MetadataResult) : MetadataOutcome()
    object NotFound : MetadataOutcome()
    data class Error(val message: String) : MetadataOutcome()
}

fun MediaType.isSeriesLike(): Boolean = this == MediaType.SERIES || this == MediaType.ANIME
