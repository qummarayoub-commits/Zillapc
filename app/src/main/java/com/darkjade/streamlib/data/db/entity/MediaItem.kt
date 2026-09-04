package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType { MOVIE, SERIES, ANIME }

/**
 * Root entity representing a single title in the library: a movie, or a
 * series/anime (which owns Seasons -> Episodes).
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalTitle: String? = null,
    val sortTitle: String,
    // Lowercased, punctuation-stripped title used to group episodes of the
    // same show under one entry even when filenames vary slightly (e.g.
    // "Spider Man" vs "Spider-Man"). See TitleNormalization.kt.
    val normalizedTitle: String = "",
    val type: MediaType,
    val year: Int? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val ageRating: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: String = "", // comma-separated, kept simple (avoids extra join tables)
    val director: String? = null,
    val cast: String = "", // comma-separated top cast names
    val alternatePosterUrls: String = "", // comma-separated — TMDB has multiple posters per title
    val alternateBackdropUrls: String = "", // comma-separated — TMDB has multiple backdrops per title, for the Change Backdrop picker
    val titleLogoUrl: String? = null, // TMDB's official stylized title-logo artwork, when available
    // Cast with photo+character, serialized as "name|character|photoUrl;;name2|..." —
    // kept as a simple string (like genres/cast) rather than a join table.
    val castMembers: String = "",
    val trailerYoutubeKey: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val status: String? = null, // e.g. "Ended", "Returning Series"
    val productionCountry: String? = null,
    val originalLanguage: String? = null,
    // Genuine IMDb/Rotten Tomatoes data from OMDb — never confused with
    // TMDB's own vote_average. omdbFetched guards against refetching on
    // every detail-page open once we've already tried once (success or not).
    val imdbId: String? = null,
    val imdbRating: Double? = null,
    val rottenTomatoesPercent: Int? = null,
    val metacriticScore: Int? = null,
    val omdbFetched: Boolean = false,
    val userRating: Int? = null, // 1-5 stars — the user's own rating (new "Rate" action)
    val tmdbId: String? = null, // remote TMDB id, used to fetch per-episode metadata
    val localFileUri: String? = null, // set for MOVIE only; series use Episode entities
    val localFilePath: String? = null,
    val metadataFetched: Boolean = false,
    val metadataMissing: Boolean = false, // true => show fallback card, never crash
    val dateAdded: Long = System.currentTimeMillis(),
    val folderSourceId: Long? = null,
)
