package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Real playback progress from the internal Media3 player — separate from
 * WatchHistoryEntity (which just records "opened at time X" for Continue
 * Watching / History UI). This table is the source of truth for resume
 * position, duration, and completed status per movie/episode.
 *
 * episodeId is null for movies. The unique index on (mediaItemId, episodeId)
 * means every episode of a series gets its own independent progress row —
 * a series never has one shared position.
 */
@Entity(
    tableName = "playback_progress",
    indices = [Index(value = ["mediaItemId", "episodeId"], unique = true)]
)
data class PlaybackProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    val episodeId: Long? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val watchedPercentage: Float = 0f,
    val completed: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)
