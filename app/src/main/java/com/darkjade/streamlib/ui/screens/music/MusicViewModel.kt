package com.darkjade.streamlib.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.dao.AlbumSummary
import com.darkjade.streamlib.data.db.dao.ArtistSummary
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MusicUiState(
    val isLoading: Boolean = true,
    val recentlyAdded: List<SongEntity> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val allSongs: List<SongEntity> = emptyList(),
    val playlists: List<com.darkjade.streamlib.data.db.dao.PlaylistSummary> = emptyList(),
)

class MusicViewModel(private val musicRepository: MusicRepository) : ViewModel() {

    val uiState: StateFlow<MusicUiState> = combine(
        musicRepository.observeRecentlyAdded(),
        musicRepository.observeAlbums(),
        musicRepository.observeArtists(),
        musicRepository.observeAllSongs(),
        musicRepository.observePlaylists(),
    ) { recent, albums, artists, songs, playlists ->
        MusicUiState(
            isLoading = false,
            recentlyAdded = recent,
            albums = albums,
            artists = artists,
            allSongs = songs,
            playlists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MusicUiState())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { musicRepository.createPlaylist(name.trim()) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { musicRepository.addSongToPlaylist(playlistId, songId) }
    }
}
