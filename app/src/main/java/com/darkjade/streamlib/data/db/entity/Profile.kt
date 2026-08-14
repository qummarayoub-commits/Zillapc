package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarRes: String? = null, // resource name or content uri
    val pinHash: String? = null, // null = no PIN set
    val isDefault: Boolean = false,
    val contentRestriction: String = "None", // e.g. "16+", "None"
    val audioLanguage: String = "English",
    val subtitleLanguage: String = "English",
)
