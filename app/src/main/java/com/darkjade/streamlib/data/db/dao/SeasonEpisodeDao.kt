package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(season: SeasonEntity): Long

    @Query("SELECT * FROM seasons WHERE mediaItemId = :mediaItemId AND seasonNumber = :seasonNumber LIMIT 1")
    suspend fun find(mediaItemId: Long, seasonNumber: Int): SeasonEntity?

    @Query("SELECT * FROM seasons WHERE mediaItemId = :mediaItemId ORDER BY seasonNumber ASC")
    fun observeForMedia(mediaItemId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE mediaItemId = :mediaItemId ORDER BY seasonNumber ASC")
    suspend fun getForMedia(mediaItemId: Long): List<SeasonEntity>
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(episode: EpisodeEntity): Long

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("SELECT * FROM episodes WHERE localFileUri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE seasonId = :seasonId ORDER BY episodeNumber ASC")
    fun observeForSeason(seasonId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE mediaItemId = :mediaItemId ORDER BY episodeNumber ASC")
    suspend fun getForMedia(mediaItemId: Long): List<EpisodeEntity>

    @Query("""
        SELECT * FROM episodes WHERE mediaItemId = :mediaItemId 
        ORDER BY watched ASC, episodeNumber ASC LIMIT 1
    """)
    suspend fun getNextUnwatched(mediaItemId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Query("""
        SELECT * FROM episodes 
        WHERE title LIKE '%' || :query || '%'
        ORDER BY episodeNumber ASC
    """)
    suspend fun search(query: String): List<EpisodeEntity>

    @Query("UPDATE episodes SET watched = :watched WHERE id = :id")
    suspend fun setWatched(id: Long, watched: Boolean)

    @Query("UPDATE episodes SET fileMissing = :missing WHERE id = :id")
    suspend fun setFileMissing(id: Long, missing: Boolean)
}
