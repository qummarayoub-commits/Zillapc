package com.darkjade.streamlib.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.data.repository.MusicRepository
import com.google.common.util.concurrent.MoreExecutors
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
    val repeatMode: Int = Player.REPEAT_MODE_OFF, // OFF / ONE / ALL
)

/**
 * Talks to MusicPlaybackService (a real foreground MediaSessionService) via
 * a MediaController instead of owning an ExoPlayer directly — this is what
 * lets playback keep going in the background/screen-off, and lets this
 * ViewModel be created fresh per navigation without interrupting playback,
 * since the actual player lives in the service, not here.
 */
class MusicPlayerViewModel(
    appContext: Context,
    private val musicRepository: MusicRepository? = null,
) : ViewModel() {

    private var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
    ).buildAsync()

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState

    private var queue: List<SongEntity> = emptyList()
    private var currentIndex: Int = -1
    private var lastSaveTick = 0L

    init {
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
                }
                override fun onRepeatModeChanged(repeatMode: Int) {
                    _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
                }
            })
        }, MoreExecutors.directExecutor())

        // Position ticker — also periodically persists playback position so
        // resuming a song later can continue from where the user left off.
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val c = controller ?: continue
                if (c.isPlaying || c.playbackState == Player.STATE_READY) {
                    val pos = c.currentPosition.coerceAtLeast(0)
                    _uiState.value = _uiState.value.copy(
                        positionMs = pos,
                        durationMs = c.duration.coerceAtLeast(0),
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
        val c = controller ?: return
        c.setMediaItem(MediaItem.fromUri(song.localFileUri))
        c.prepare()
        // Continue from previous position where it makes sense (not right
        // at the very end — that's effectively "finished").
        if (song.lastPositionMs > 0 && song.durationMs > 0 && song.lastPositionMs < song.durationMs - 5000) {
            c.seekTo(song.lastPositionMs)
        }
        c.playWhenReady = true
        _uiState.value = _uiState.value.copy(currentSong = song, positionMs = song.lastPositionMs, durationMs = 0)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
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
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    /** Cycles OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun playCurrentIndex() {
        val song = queue.getOrNull(currentIndex) ?: return
        val c = controller ?: return
        c.setMediaItem(MediaItem.fromUri(song.localFileUri))
        c.prepare()
        c.playWhenReady = true
        _uiState.value = _uiState.value.copy(currentSong = song, positionMs = 0, durationMs = 0)
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
