package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seasonId"), Index("mediaItemId")]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    val seasonId: Long,
    val episodeNumber: Int,
    val title: String? = null,
    val overview: String? = null,
    val thumbnailUrl: String? = null,
    val durationMinutes: Int? = null,
    val localFileUri: String,
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0,
    val quality: String? = null, // e.g. "1080p"
    val watched: Boolean = false,
    val fileMissing: Boolean = false,
)
