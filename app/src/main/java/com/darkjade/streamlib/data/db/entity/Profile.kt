package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarRes: String? = null, // resource name or content uri
    val bannerRes: String? = null, // content uri of a user-picked cover/banner image
    val bannerOffsetX: Float = 0f, // pan/zoom adjustment for how the banner is displayed
    val bannerOffsetY: Float = 0f,
    val bannerScale: Float = 1f,
    val pinHash: String? = null, // null = no PIN set
    val isDefault: Boolean = false,
    val contentRestriction: String = "None", // e.g. "16+", "None"
    val audioLanguage: String = "English",
    val subtitleLanguage: String = "English",
)
