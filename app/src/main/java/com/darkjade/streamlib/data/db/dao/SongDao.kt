package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkjade.streamlib.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class AlbumSummary(
    val album: String,
    val artist: String,
    val artworkPath: String?,
    val trackCount: Int,
)

data class ArtistSummary(
    val artist: String,
    val trackCount: Int,
)

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<SongEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Query("SELECT * FROM songs WHERE localFileUri = :uri")
    suspend fun getByUri(uri: String): SongEntity?

    @androidx.room.Update
    suspend fun update(song: SongEntity)

    /** Proper upsert: new file -> insert; already-known file (by its stable
     * URI) -> update its metadata in place, keeping the same id (so
     * playlist references and lastPositionMs survive a rescan) rather than
     * silently doing nothing or creating a duplicate row. */
    @androidx.room.Transaction
    suspend fun upsert(song: SongEntity) {
        val existing = getByUri(song.localFileUri)
        if (existing != null) {
            update(song.copy(id = existing.id, lastPositionMs = existing.lastPositionMs, dateAdded = existing.dateAdded))
        } else {
            insert(song)
        }
    }

    @Query("SELECT localFileUri FROM songs WHERE folderSourceId = :folderSourceId")
    suspend fun getUrisForFolder(folderSourceId: Long): List<String>

    @Query("DELETE FROM songs WHERE folderSourceId = :folderSourceId AND localFileUri NOT IN (:presentUris)")
    suspend fun deleteMissingInFolder(folderSourceId: Long, presentUris: List<String>)

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    fun observeByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    fun observeByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT album, artist, MAX(artworkPath) as artworkPath, COUNT(*) as trackCount FROM songs GROUP BY album, artist ORDER BY album COLLATE NOCASE ASC")
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query("SELECT artist, COUNT(*) as trackCount FROM songs GROUP BY artist ORDER BY artist COLLATE NOCASE ASC")
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query("SELECT COUNT(*) FROM songs")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("UPDATE songs SET lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updatePosition(id: Long, positionMs: Long)
}
