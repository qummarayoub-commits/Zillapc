package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.dao.AlbumSummary
import com.darkjade.streamlib.data.db.dao.ArtistSummary
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.scanner.MusicScanEvent
import com.darkjade.streamlib.data.scanner.MusicScanner
import kotlinx.coroutines.flow.Flow

class MusicRepository(context: Context) {
    private val songDao = StreamLibDatabase.getInstance(context).songDao()
    private val scanner = MusicScanner(context)

    fun observeAllSongs(): Flow<List<SongEntity>> = songDao.observeAll()
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<SongEntity>> = songDao.observeRecentlyAdded(limit)
    fun observeAlbums(): Flow<List<AlbumSummary>> = songDao.observeAlbums()
    fun observeArtists(): Flow<List<ArtistSummary>> = songDao.observeArtists()
    fun observeSongCount(): Flow<Int> = songDao.observeCount()
    fun observeSongsByAlbum(album: String): Flow<List<SongEntity>> = songDao.observeByAlbum(album)
    fun observeSongsByArtist(artist: String): Flow<List<SongEntity>> = songDao.observeByArtist(artist)
    suspend fun getSong(id: Long): SongEntity? = songDao.getById(id)
    fun albumArtUri(albumId: Long) = scanner.albumArtUri(albumId)

    /** Scans MediaStore's audio index and syncs the songs table — inserts new
     * tracks, removes ones no longer present on device, per real MediaStore data. */
    suspend fun scanMusicLibrary(): Result<Int> {
        var finalCount = 0
        var error: String? = null
        scanner.scanDevice().collect { event ->
            when (event) {
                is MusicScanEvent.Complete -> {
                    songDao.insertAll(event.songs)
                    val presentIds = event.songs.map { it.mediaStoreId }
                    if (presentIds.isNotEmpty()) {
                        songDao.deleteMissing(presentIds)
                    }
                    finalCount = event.songs.size
                }
                is MusicScanEvent.Error -> error = event.message
                is MusicScanEvent.Progress -> {}
            }
        }
        return if (error != null) Result.failure(Exception(error)) else Result.success(finalCount)
    }
}
