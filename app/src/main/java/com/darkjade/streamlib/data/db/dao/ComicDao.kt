package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkjade.streamlib.data.db.entity.ComicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(comic: ComicEntity): Long

    @Update
    suspend fun update(comic: ComicEntity)

    @Query("SELECT * FROM comics WHERE localFileUri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): ComicEntity?

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getById(id: Long): ComicEntity?

    @Query("SELECT * FROM comics WHERE id = :id")
    fun observeById(id: Long): Flow<ComicEntity?>

    @Query("SELECT * FROM comics ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<ComicEntity>>

    @Query("SELECT COUNT(*) FROM comics")
    fun observeCount(): Flow<Int>

    @Query("""
        SELECT * FROM comics 
        WHERE title LIKE '%' || :query || '%' OR seriesName LIKE '%' || :query || '%'
        ORDER BY sortTitle ASC
    """)
    suspend fun search(query: String): List<ComicEntity>

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE comics SET lastReadPage = :page WHERE id = :id")
    suspend fun updateLastReadPage(id: Long, page: Int)
}
