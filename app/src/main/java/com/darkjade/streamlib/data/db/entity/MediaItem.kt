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
    val type: MediaType,
    val year: Int? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val ageRating: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: String = "", // comma-separated, kept simple (avoids extra join tables)
    val localFileUri: String? = null, // set for MOVIE only; series use Episode entities
    val localFilePath: String? = null,
    val metadataFetched: Boolean = false,
    val metadataMissing: Boolean = false, // true => show fallback card, never crash
    val dateAdded: Long = System.currentTimeMillis(),
    val folderSourceId: Long? = null,
)
