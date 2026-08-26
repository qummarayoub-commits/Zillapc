package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long, // MediaStore.Audio.Media._ID — used to dedupe on rescan
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val durationMs: Long,
    val localFileUri: String,
    val albumId: Long? = null, // MediaStore.Audio.Media.ALBUM_ID — used to load embedded/album art
    val dateAdded: Long = System.currentTimeMillis(),
)
