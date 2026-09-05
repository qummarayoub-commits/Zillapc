package com.darkjade.streamlib.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<MediaItemEntity>): List<Long>

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun observeById(id: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE normalizedTitle = :normalizedTitle AND type = :type AND year IS :year LIMIT 1")
    suspend fun findByTitleTypeYear(normalizedTitle: String, type: MediaType, year: Int?): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE normalizedTitle = :normalizedTitle AND type = :type LIMIT 1")
    suspend fun findByNormalizedTitleAndType(normalizedTitle: String, type: MediaType): MediaItemEntity?

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY sortTitle ASC")
    fun observeByType(type: MediaType): Flow<List<MediaItemEntity>>

    // "No Info" — items TMDB couldn't match (or that haven't been looked
    // up yet), surfaced so the user can manually Add Info later instead of
    // the mismatch/miss silently sitting invisible in the library.
    @Query("SELECT * FROM media_items WHERE metadataMissing = 1 OR metadataFetched = 0 ORDER BY dateAdded DESC")
    fun observeMissingMetadata(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY sortTitle ASC")
    fun pagingByType(type: MediaType): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items ORDER BY sortTitle ASC")
    fun pagingAll(): PagingSource<Int, MediaItemEntity>

    @Query("""
        SELECT * FROM media_items 
        WHERE title LIKE '%' || :query || '%' 
           OR originalTitle LIKE '%' || :query || '%'
        ORDER BY sortTitle ASC
    """)
    suspend fun search(query: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE genres LIKE '%' || :genre || '%' ORDER BY sortTitle ASC")
    fun observeByGenre(genre: String): Flow<List<MediaItemEntity>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun observeCount(): Flow<Int>

    @Query("SELECT DISTINCT genres FROM media_items WHERE genres != ''")
    suspend fun getAllGenreStrings(): List<String>

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE folderSourceId = :folderSourceId")
    suspend fun deleteByFolderSource(folderSourceId: Long)
}
