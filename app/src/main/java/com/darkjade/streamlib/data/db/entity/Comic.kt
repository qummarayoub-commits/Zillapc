package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single comic issue file (.cbz/.cbr/.cb7/.pdf). Kept as its own table
 * rather than folded into MediaItemEntity since comics have a distinct
 * shape of metadata (issue number, publisher, volume/series name) that
 * doesn't map cleanly onto movies/shows.
 */
@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // display title, e.g. "Amazing Spider-Man #45"
    val seriesName: String,
    val issueNumber: String? = null,
    val sortTitle: String,
    val publisher: String? = null,
    val overview: String? = null,
    val coverUrl: String? = null,
    val releaseDate: String? = null,
    val localFileUri: String,
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0,
    val fileExtension: String = "", // cbz/cbr/cb7 — stored explicitly at scan time, not parsed from the URI later
    val metadataFetched: Boolean = false,
    val metadataMissing: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val folderSourceId: Long? = null,
)
