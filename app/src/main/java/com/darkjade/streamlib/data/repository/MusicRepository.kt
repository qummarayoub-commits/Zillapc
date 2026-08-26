package com.darkjade.streamlib.data.repository

import android.content.Context
import android.net.Uri
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.dao.AlbumSummary
import com.darkjade.streamlib.data.db.dao.ArtistSummary
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.scanner.MusicScanEvent
import com.darkjade.streamlib.data.scanner.MusicScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.security.MessageDigest

class MusicRepository(private val context: Context) {
    private val songDao = StreamLibDatabase.getInstance(context).songDao()
    private val playlistDao = StreamLibDatabase.getInstance(context).playlistDao()
    private val scanner = MusicScanner(context)
    private val artworkProvider = com.darkjade.streamlib.data.metadata.musicbrainz.MusicArtworkProvider()

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

    /** Priority 3 — for songs still missing artwork after embedded/folder
     * lookup, tries MusicBrainz + Cover Art Archive by Artist+Album (never
     * by song title alone, and never invents artwork — silently leaves
     * artworkPath null on any miss). One lookup per distinct album, with a
     * small delay between requests per MusicBrainz's rate-limit etiquette.
     * Run as a separate step after a scan, so a slow network doesn't stall
     * the local scan itself. */
    suspend fun fetchMissingArtworkOnline(): Int {
        var updated = 0
        val allSongs = songDao.observeAll().first()
        val missingAlbums = allSongs
            .filter { it.artworkPath == null }
            .groupBy { it.artist to it.album }

        for ((key, songsInAlbum) in missingAlbums) {
            val (artist, album) = key
            val bytes = artworkProvider.fetchAlbumArt(artist, album) ?: continue
            val path = cacheArtwork("$artist|$album", bytes) ?: continue
            songsInAlbum.forEach { song ->
                songDao.update(song.copy(artworkPath = path))
                updated++
            }
            kotlinx.coroutines.delay(1100) // be polite to MusicBrainz's free service
        }
        return updated
    }

    /**
     * Primary flow — scans the whole device (no folder picker needed),
     * same idea as movies' "Scan Device for Videos". MusicClassifier still
     * filters out voice notes/call recordings/etc.; only real songs get
     * inserted, existing entries with no matching device file get removed.
     */
    suspend fun scanDeviceForMusic(): Result<Int> {
        var found = 0
        var error: String? = null
        val presentUris = mutableListOf<String>()

        try {
            scanner.scanDevice().collect { event ->
                when (event) {
                    is MusicScanEvent.SongFound -> {
                        val artworkPath = event.song.embeddedArtwork?.let { bytes -> cacheArtwork("${event.song.artist}|${event.song.album}", bytes) }
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
                            folderSourceId = null,
                        )
                        songDao.upsert(entity)
                        presentUris.add(event.song.uri.toString())
                        found++
                    }
                    is MusicScanEvent.Completed -> {}
                    is MusicScanEvent.Failed -> error = event.message
                    is MusicScanEvent.Progress -> {}
                    is MusicScanEvent.SkippedLowConfidence -> {}
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Music scan failed"
        }

        return if (error != null) Result.failure(Exception(error)) else Result.success(found)
    }

    /**
     * Scans a user-selected SAF folder tree for music files — same
     * architecture as the existing video LibraryScanner — and syncs the
     * songs table: inserts newly found tracks, removes ones from this
     * folder no longer present. Real embedded tag metadata + artwork only,
     * never fabricated. Kept as a secondary/optional path, same as the
     * "Add Specific Folder" option on Movies/Comics.
     */
    suspend fun scanMusicFolder(treeUri: Uri, folderSourceId: Long): Result<Int> {
        var found = 0
        var error: String? = null
        val presentUris = mutableListOf<String>()

        try {
            scanner.scanTree(treeUri).collect { event ->
                when (event) {
                    is MusicScanEvent.SongFound -> {
                        val artworkPath = event.song.embeddedArtwork?.let { bytes -> cacheArtwork("${event.song.artist}|${event.song.album}", bytes) }
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
                        songDao.upsert(entity)
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

    /** Writes embedded album art to the app's cache dir, keyed by ALBUM (not
     * per-song URI) so every song from the same album shares one cached
     * file instead of duplicating identical bytes per-track. Returns a
     * proper "file://" URI string — Coil's default String→data resolution
     * parses via Uri.parse(), and a schemeless raw path silently fails to
     * load (this was the actual root cause of "no artwork showing"). */
    private fun cacheArtwork(albumKey: String, bytes: ByteArray): String? {
        return try {
            val dir = File(context.cacheDir, "album_art").apply { mkdirs() }
            val hash = MessageDigest.getInstance("MD5").digest(albumKey.toByteArray()).joinToString("") { "%02x".format(it) }
            val file = File(dir, "$hash.jpg")
            if (!file.exists() || file.length() == 0L) {
                file.writeBytes(bytes)
            }
            android.net.Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }
}
