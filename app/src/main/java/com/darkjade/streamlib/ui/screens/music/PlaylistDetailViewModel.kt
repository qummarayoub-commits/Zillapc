package com.darkjade.streamlib.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.PlaylistEntity
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val isLoading: Boolean = true,
    val playlist: PlaylistEntity? = null,
    val songs: List<SongEntity> = emptyList(),
)

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _playlist = MutableStateFlow<PlaylistEntity?>(null)

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        _playlist,
        musicRepository.observePlaylistSongs(playlistId),
    ) { playlist, songs ->
        PlaylistDetailUiState(isLoading = false, playlist = playlist, songs = songs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistDetailUiState())

    init {
        viewModelScope.launch {
            _playlist.value = musicRepository.getPlaylist(playlistId)
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            musicRepository.renamePlaylist(playlistId, newName)
            _playlist.value = musicRepository.getPlaylist(playlistId)
        }
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch { musicRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    fun reorder(orderedSongIds: List<Long>) {
        viewModelScope.launch { musicRepository.reorderPlaylist(playlistId, orderedSongIds) }
    }
}
