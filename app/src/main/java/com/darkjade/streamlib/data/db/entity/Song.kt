package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localFileUri: String, // SAF content:// URI — used to dedupe on rescan
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val durationMs: Long,
    // Embedded album art extracted from the file's own tags, cached to a
    // local file (not stored as a DB blob) — path under the app's cache dir.
    val artworkPath: String? = null,
    val folderSourceId: Long? = null,
    val lastPositionMs: Long = 0, // for "continue from previous position"
    val dateAdded: Long = System.currentTimeMillis(),
)
