package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records that a profile "opened" a piece of media in an external player.
 * We deliberately do NOT store playback position — since playback happens
 * outside our app, we only know that the user launched the file, not how
 * far they got, unless a future integration reports it back.
 */
@Entity(
    tableName = "watch_history",
    indices = [Index("profileId"), Index("mediaItemId"), Index(value = ["profileId", "mediaItemId"], unique = false)]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val mediaItemId: Long,
    val episodeId: Long? = null, // null for movies
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val watched: Boolean = false,
    val lastKnownPositionMs: Long? = null, // reserved for future player integration
)
