package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.WatchHistoryEntity
import com.darkjade.streamlib.data.db.entity.WatchlistEntity

/**
 * Tracks watch history and watchlist. Because playback happens in an
 * external player (Phase 12), we only record that the user *opened* a
 * title/episode — never a fabricated playback position (Phase 13).
 */
class WatchRepository(context: Context) {
    private val db = StreamLibDatabase.getInstance(context)
    private val historyDao = db.watchHistoryDao()
    private val watchlistDao = db.watchlistDao()
    private val episodeDao = db.episodeDao()

    fun observeContinueWatching(profileId: Long) = historyDao.observeContinueWatching(profileId)
    fun observeHistory(profileId: Long) = historyDao.observeHistory(profileId)

    suspend fun recordOpened(profileId: Long, mediaItemId: Long, episodeId: Long?) {
        historyDao.insert(
            WatchHistoryEntity(
                profileId = profileId,
                mediaItemId = mediaItemId,
                episodeId = episodeId,
                lastOpenedAt = System.currentTimeMillis(),
            )
        )
        if (episodeId != null) {
            episodeDao.setWatched(episodeId, true)
        }
    }

    suspend fun clearHistory(profileId: Long) = historyDao.clearHistory(profileId)

    fun observeWatchlist(profileId: Long) = watchlistDao.observeWatchlist(profileId)
    fun observeIsInWatchlist(profileId: Long, mediaItemId: Long) =
        watchlistDao.observeIsInWatchlist(profileId, mediaItemId)

    suspend fun addToWatchlist(profileId: Long, mediaItemId: Long) =
        watchlistDao.add(WatchlistEntity(profileId = profileId, mediaItemId = mediaItemId))

    suspend fun removeFromWatchlist(profileId: Long, mediaItemId: Long) =
        watchlistDao.remove(profileId, mediaItemId)
}
