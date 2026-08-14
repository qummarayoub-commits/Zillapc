package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.ProfileEntity
import com.darkjade.streamlib.data.db.entity.WatchHistoryEntity
import com.darkjade.streamlib.data.db.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntity): Long

    @Query("""
        SELECT mi.* FROM media_items mi
        INNER JOIN (
            SELECT mediaItemId, MAX(lastOpenedAt) AS lastOpenedAt
            FROM watch_history WHERE profileId = :profileId
            GROUP BY mediaItemId
        ) latest ON mi.id = latest.mediaItemId
        ORDER BY latest.lastOpenedAt DESC
        LIMIT :limit
    """)
    fun observeContinueWatching(profileId: Long, limit: Int = 20): Flow<List<MediaItemEntity>>

    @Query("""
        SELECT wh.* FROM watch_history wh
        WHERE wh.profileId = :profileId AND wh.mediaItemId = :mediaItemId
        ORDER BY wh.lastOpenedAt DESC LIMIT 1
    """)
    suspend fun getLatestForMedia(profileId: Long, mediaItemId: Long): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastOpenedAt DESC")
    fun observeHistory(profileId: Long): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: Long)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteEntry(id: Long)
}

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: WatchlistEntity): Long

    @Query("DELETE FROM watchlist WHERE profileId = :profileId AND mediaItemId = :mediaItemId")
    suspend fun remove(profileId: Long, mediaItemId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE profileId = :profileId AND mediaItemId = :mediaItemId)")
    fun observeIsInWatchlist(profileId: Long, mediaItemId: Long): Flow<Boolean>

    @Query("""
        SELECT mi.* FROM media_items mi
        INNER JOIN watchlist w ON mi.id = w.mediaItemId
        WHERE w.profileId = :profileId
        ORDER BY w.addedAt DESC
    """)
    fun observeWatchlist(profileId: Long): Flow<List<MediaItemEntity>>
}
