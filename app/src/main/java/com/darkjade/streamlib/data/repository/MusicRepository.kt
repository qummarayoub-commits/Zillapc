package com.darkjade.streamlib.data.repository

import android.content.Context
import android.net.Uri
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.dao.AlbumSummary
import com.darkjade.streamlib.data.db.dao.ArtistSummary
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.scanner.MusicScanEvent
import com.darkjade.streamlib.data.scanner.MusicScanner
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest

class MusicRepository(private val context: Context) {
    private val songDao = StreamLibDatabase.getInstance(context).songDao()
    private val playlistDao = StreamLibDatabase.getInstance(context).playlistDao()
    private val scanner = MusicScanner(context)

    fun observeAllSongs(): Flow<List<SongEntity>> = songDao.observeAll()
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<SongEntity>> = songDao.observeRecentlyAdded(limit)
    fun observeAlbums(): Flow<List<AlbumSummary>> = songDao.observeAlbums()
    fun observeArtists(): Flow<List<ArtistSummary>> = songDao.observeArtists()
    fun observeSongCount(): Flow<Int> = songDao.observeCount()
    fun observeSongsByAlbum(album: String): Flow<List<SongEntity>> = songDao.observeByAlbum(album)
    fun observeSongsByArtist(artist: String): Flow<List<SongEntity>> = songDao.observeByArtist(artist)
    suspend fun getSong(id: Long): SongEntity? = songDao.getById(id)

    suspend fun saveSongPosition(songId: Long, positionMs: Long) = songDao.updatePosition(songId, positionMs)

    // --- Playlists ---

    fun observePlaylists() = playlistDao.observePlaylists()
    fun observePlaylistSongs(playlistId: Long) = playlistDao.observeSongsInPlaylist(playlistId)
    suspend fun getPlaylist(playlistId: Long) = playlistDao.getPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(com.darkjade.streamlib.data.db.entity.PlaylistEntity(name = name))

    suspend fun renamePlaylist(playlistId: Long, newName: String) = playlistDao.renamePlaylist(playlistId, newName)

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        if (playlistDao.isSongInPlaylist(playlistId, songId)) return
        val nextPosition = playlistDao.maxPosition(playlistId) + 1
        playlistDao.addSongToPlaylist(
            com.darkjade.streamlib.data.db.entity.PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition,
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = playlistDao.removeSongFromPlaylist(playlistId, songId)

    /** Reorders by rewriting positions 0..n-1 in the given final order. */
    suspend fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            playlistDao.updateSongPosition(playlistId, songId, index)
        }
    }

    /**
     * Scans a user-selected SAF folder tree for music files — same
     * architecture as the existing video LibraryScanner — and syncs the
     * songs table: inserts newly found tracks, removes ones from this
     * folder no longer present. Real embedded tag metadata + artwork only,
     * never fabricated.
     */
    suspend fun scanMusicFolder(treeUri: Uri, folderSourceId: Long): Result<Int> {
        var found = 0
        var error: String? = null
        val presentUris = mutableListOf<String>()

        try {
            scanner.scanTree(treeUri).collect { event ->
                when (event) {
                    is MusicScanEvent.SongFound -> {
                        val artworkPath = event.song.embeddedArtwork?.let { bytes -> cacheArtwork(event.song.uri.toString(), bytes) }
                        val entity = SongEntity(
                            localFileUri = event.song.uri.toString(),
                            title = event.song.title,
                            artist = event.song.artist,
                            album = event.song.album,
                            albumArtist = event.song.albumArtist,
                            trackNumber = event.song.trackNumber,
                            year = event.song.year,
                            genre = event.song.genre,
                            durationMs = event.song.durationMs,
                            artworkPath = artworkPath,
                            folderSourceId = folderSourceId,
                        )
                        songDao.insert(entity)
                        presentUris.add(event.song.uri.toString())
                        found++
                    }
                    is MusicScanEvent.Completed -> {
                        if (presentUris.isNotEmpty()) {
                            songDao.deleteMissingInFolder(folderSourceId, presentUris)
                        }
                    }
                    is MusicScanEvent.Failed -> error = event.message
                    is MusicScanEvent.Progress -> {}
                    is MusicScanEvent.SkippedLowConfidence -> {} // not music-confident — left untouched, not imported
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Music scan failed"
        }

        return if (error != null) Result.failure(Exception(error)) else Result.success(found)
    }

    /** Writes embedded album art to the app's cache dir, keyed by a hash of
     * the file's own URI so re-scanning the same song reuses the same file
     * instead of endlessly duplicating images on disk. */
    private fun cacheArtwork(key: String, bytes: ByteArray): String? {
        return try {
            val dir = File(context.cacheDir, "album_art").apply { mkdirs() }
            val hash = MessageDigest.getInstance("MD5").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
            val file = File(dir, "$hash.jpg")
            if (!file.exists()) {
                file.writeBytes(bytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
