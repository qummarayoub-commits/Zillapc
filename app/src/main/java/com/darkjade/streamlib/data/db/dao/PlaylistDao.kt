package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkjade.streamlib.data.db.entity.PlaylistEntity
import com.darkjade.streamlib.data.db.entity.PlaylistSongEntity
import com.darkjade.streamlib.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val songCount: Int,
    val artworkPath: String?, // from the first song added, for a simple cover
)

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query(
        """
        SELECT p.id as id, p.name as name,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlistId = p.id) as songCount,
               (SELECT s.artworkPath FROM playlist_songs ps2
                    JOIN songs s ON s.id = ps2.songId
                    WHERE ps2.playlistId = p.id ORDER BY ps2.position ASC LIMIT 1) as artworkPath
        FROM playlists p
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: Long): PlaylistEntity?

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """
    )
    fun observeSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)
}
