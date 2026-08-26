package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.PlaybackProgressEntity

/** Fraction of duration at which a movie/episode is considered "completed" — matches spec (~90%). */
private const val COMPLETION_THRESHOLD = 0.9f

class PlaybackRepository(context: Context) {
    private val db = StreamLibDatabase.getInstance(context)
    private val dao = db.playbackProgressDao()
    private val episodeDao = db.episodeDao()

    suspend fun getProgress(mediaItemId: Long, episodeId: Long?): PlaybackProgressEntity? =
        dao.find(mediaItemId, episodeId)

    fun observeProgress(mediaItemId: Long, episodeId: Long?) = dao.observe(mediaItemId, episodeId)

    suspend fun getAllForMedia(mediaItemId: Long) = dao.getAllForMedia(mediaItemId)

    /** All progress rows, live — used to drive the Home screen's Continue
     * Watching progress bars. */
    fun observeAllProgress() = dao.observeAll()

    /** Used by "Play from Beginning" — discards saved position so the player starts fresh. */
    suspend fun clearProgress(mediaItemId: Long, episodeId: Long?) = dao.clear(mediaItemId, episodeId)

    /**
     * Called from auto-save (periodic), on pause, on exit, on Back, on
     * backgrounding, and on playback finishing — every meaningful save point
     * per the spec. Marks completed at ~90% and keeps the final position
     * (never resets progress once saved).
     */
    /** Manual "Mark as Watched" action — forces completed=true at full duration. */
    suspend fun markAsWatched(mediaItemId: Long, episodeId: Long?, durationMsFallback: Long) {
        val existing = dao.find(mediaItemId, episodeId)
        val durationMs = existing?.durationMs?.takeIf { it > 0 } ?: durationMsFallback
        if (durationMs <= 0) return
        dao.upsert(
            PlaybackProgressEntity(
                mediaItemId = mediaItemId,
                episodeId = episodeId,
                durationMs = durationMs,
                positionMs = durationMs,
                watchedPercentage = 1f,
                completed = true,
                lastPlayedAt = System.currentTimeMillis(),
            )
        )
        if (episodeId != null) {
            episodeDao.setWatched(episodeId, true)
        }
    }

    suspend fun saveProgress(mediaItemId: Long, episodeId: Long?, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        val pct = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val completed = pct >= COMPLETION_THRESHOLD

        dao.upsert(
            PlaybackProgressEntity(
                mediaItemId = mediaItemId,
                episodeId = episodeId,
                durationMs = durationMs,
                positionMs = positionMs,
                watchedPercentage = pct,
                completed = completed,
                lastPlayedAt = System.currentTimeMillis(),
            )
        )

        // Keep the existing Episode "watched" checkmark (already used elsewhere
        // in the app) in sync with real playback completion, without touching
        // any existing schema/behavior beyond this one flag.
        if (completed && episodeId != null) {
            episodeDao.setWatched(episodeId, true)
        }
    }
}
