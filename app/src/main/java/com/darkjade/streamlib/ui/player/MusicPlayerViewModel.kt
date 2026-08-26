package com.darkjade.streamlib.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.repository.MusicRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MusicPlayerUiState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val errorMessage: String? = null,
)

/**
 * Owns a direct ExoPlayer instance — same proven, reliable pattern as the
 * video PlayerViewModel (which has worked correctly this whole project),
 * instead of the previous MediaController/MediaSessionService indirection.
 * That async service-binding split was the most likely source of the
 * "shows Playing but no audio" bug (a race between the controller resolving
 * and the UI issuing play commands). Trading background-playback
 * continuity for reliability, per explicit priority: get real audio output
 * working correctly first.
 */
class MusicPlayerViewModel(
    appContext: Context,
    private val musicRepository: MusicRepository? = null,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .build()
        .apply { volume = 1f }

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState

    private var queue: List<SongEntity> = emptyList()
    private var currentIndex: Int = -1
    private var lastSaveTick = 0L

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
            }
            // The UI must reflect what ExoPlayer actually did — never show
            // "Playing" if playback genuinely failed.
            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MusicPlayer", "Playback error for ${_uiState.value.currentSong?.title}: ${error.errorCodeName} - ${error.message}", error)
                _uiState.value = _uiState.value.copy(
                    isPlaying = false,
                    errorMessage = "Couldn't play this file (${error.errorCodeName}). It may use an unsupported format.",
                )
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("MusicPlayer", "playbackState=$playbackState for ${_uiState.value.currentSong?.title}")
            }
        })

        viewModelScope.launch {
            while (true) {
                delay(500)
                if (player.isPlaying || player.playbackState == Player.STATE_READY) {
                    val pos = player.currentPosition.coerceAtLeast(0)
                    _uiState.value = _uiState.value.copy(
                        positionMs = pos,
                        durationMs = player.duration.coerceAtLeast(0),
                    )
                    val song = _uiState.value.currentSong
                    if (song != null && musicRepository != null && System.currentTimeMillis() - lastSaveTick > 4000) {
                        lastSaveTick = System.currentTimeMillis()
                        musicRepository.saveSongPosition(song.id, pos)
                    }
                }
            }
        }
    }

    fun playSong(song: SongEntity, playQueue: List<SongEntity> = listOf(song)) {
        queue = playQueue
        currentIndex = playQueue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        _uiState.value = _uiState.value.copy(currentSong = song, positionMs = song.lastPositionMs, durationMs = 0, errorMessage = null)
        player.setMediaItem(MediaItem.fromUri(song.localFileUri))
        player.prepare()
        if (song.lastPositionMs > 0 && song.durationMs > 0 && song.lastPositionMs < song.durationMs - 5000) {
            player.seekTo(song.lastPositionMs)
        }
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (queue.isEmpty()) return
        val nextIndex = (currentIndex + 1).coerceAtMost(queue.size - 1)
        if (nextIndex != currentIndex) {
            currentIndex = nextIndex
            playCurrentIndex()
        }
    }

    fun previous() {
        if (queue.isEmpty()) return
        val prevIndex = (currentIndex - 1).coerceAtLeast(0)
        if (prevIndex != currentIndex) {
            currentIndex = prevIndex
            playCurrentIndex()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun playCurrentIndex() {
        val song = queue.getOrNull(currentIndex) ?: return
        _uiState.value = _uiState.value.copy(currentSong = song, positionMs = 0, durationMs = 0, errorMessage = null)
        player.setMediaItem(MediaItem.fromUri(song.localFileUri))
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
