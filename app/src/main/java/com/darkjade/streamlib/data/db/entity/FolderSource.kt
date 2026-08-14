package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_sources")
data class FolderSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String, // SAF persisted tree URI
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastScannedAt: Long? = null,
    val itemCount: Int = 0,
)
