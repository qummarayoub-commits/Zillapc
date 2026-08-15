package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkjade.streamlib.data.db.entity.FolderSourceEntity
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderSourceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: FolderSourceEntity): Long

    @Update
    suspend fun update(source: FolderSourceEntity)

    @Delete
    suspend fun delete(source: FolderSourceEntity)

    @Query("SELECT * FROM folder_sources ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<FolderSourceEntity>>

    @Query("SELECT * FROM folder_sources ORDER BY addedAt ASC")
    suspend fun getAll(): List<FolderSourceEntity>

    @Query("SELECT * FROM folder_sources WHERE treeUri = :treeUri LIMIT 1")
    suspend fun findByUri(treeUri: String): FolderSourceEntity?
}

@Dao
interface ScanStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: ScanStatusEntity)

    @Query("SELECT * FROM scan_status WHERE id = 0")
    fun observe(): Flow<ScanStatusEntity?>

    // id = 1 is used for comic scans — kept separate from video scan status
    // (id = 0) so scanning videos and comics never show each other's progress.
    @Query("SELECT * FROM scan_status WHERE id = 1")
    fun observeComicScan(): Flow<ScanStatusEntity?>
}
