package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkjade.streamlib.data.db.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity): Long

    @Query("SELECT * FROM playback_progress WHERE mediaItemId = :mediaItemId AND episodeId IS :episodeId LIMIT 1")
    suspend fun find(mediaItemId: Long, episodeId: Long?): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE mediaItemId = :mediaItemId AND episodeId IS :episodeId LIMIT 1")
    fun observe(mediaItemId: Long, episodeId: Long?): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE mediaItemId = :mediaItemId")
    suspend fun getAllForMedia(mediaItemId: Long): List<PlaybackProgressEntity>

    @Query("DELETE FROM playback_progress WHERE mediaItemId = :mediaItemId AND episodeId IS :episodeId")
    suspend fun clear(mediaItemId: Long, episodeId: Long?)
}
