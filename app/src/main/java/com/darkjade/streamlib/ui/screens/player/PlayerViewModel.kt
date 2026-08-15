package com.darkjade.streamlib.ui.screens.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.PlaybackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioTrackOption(
    val group: Tracks.Group,
    val trackIndexInGroup: Int,
    val label: String,
    val isSelected: Boolean,
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val errorMessage: String? = null,
    val audioTracks: List<AudioTrackOption> = emptyList(),
)

/**
 * Owns the Media3/ExoPlayer instance for one movie/episode. Never scans or
 * identifies media itself — it only resolves the existing URI from the
 * existing LibraryRepository by the existing movie/episode ID, exactly per
 * the "additive only" requirement.
 */
class PlayerViewModel(
    private val mediaId: Long,
    private val episodeId: Long?,
    appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var hasResumed = false

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "This video could not be played."
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    if (!hasResumed) {
                        hasResumed = true
                        resumeSavedPosition()
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    // Playback finished — save immediately as completed (Phase: COMPLETED STATUS).
                    saveProgressNow(forceCompleted = true)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                _uiState.value = _uiState.value.copy(audioTracks = buildAudioTrackOptions(tracks))
            }
        })

        viewModelScope.launch {
            try {
                val (uriString, title) = resolveMedia()
                if (uriString.isNullOrBlank()) {
                    _uiState.value = PlayerUiState(isLoading = false, errorMessage = "Could not find this file.")
                    return@launch
                }
                _uiState.value = _uiState.value.copy(title = title)
                player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
                player.prepare()
                player.playWhenReady = true
                startAutoSave()
            } catch (e: Exception) {
                _uiState.value = PlayerUiState(isLoading = false, errorMessage = e.message ?: "Playback error")
            }
        }
    }

    private suspend fun resolveMedia(): Pair<String?, String> {
        return if (episodeId != null) {
            val episode = libraryRepository.getEpisode(episodeId)
            val media = libraryRepository.getMediaItem(mediaId)
            val title = buildString {
                append(media?.title ?: "Episode")
                episode?.let { append(" · E${it.episodeNumber}") }
                episode?.title?.let { if (it.isNotBlank()) append(" - $it") }
            }
            (episode?.localFileUri) to title
        } else {
            val media = libraryRepository.getMediaItem(mediaId)
            (media?.localFileUri) to (media?.title.orEmpty())
        }
    }

    private fun resumeSavedPosition() {
        viewModelScope.launch {
            val saved = playbackRepository.getProgress(mediaId, episodeId)
            if (saved != null && saved.positionMs > 0 && !saved.completed) {
                player.seekTo(saved.positionMs)
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(10_000) // periodic save without hammering the DB
                saveProgressNow()
            }
        }
    }

    /** Called on pause, on Back/exit, on backgrounding, on finish, and periodically. */
    fun saveProgressNow(forceCompleted: Boolean = false) {
        val duration = player.duration
        if (duration <= 0) return
        val position = if (forceCompleted) duration else player.currentPosition.coerceAtLeast(0)
        viewModelScope.launch {
            playbackRepository.saveProgress(mediaId, episodeId, position, duration)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        if (!player.isPlaying) saveProgressNow()
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration.coerceAtLeast(0)))
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        val override = TrackSelectionOverride(option.group.mediaTrackGroup, option.trackIndexInGroup)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(override)
            .build()
    }

    private fun buildAudioTrackOptions(tracks: Tracks): List<AudioTrackOption> {
        val options = mutableListOf<AudioTrackOption>()
        for (group in tracks.groups) {
            if (group.type != androidx.media3.common.C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.language?.uppercase() ?: format.label ?: "Track ${i + 1}"
                options.add(
                    AudioTrackOption(
                        group = group,
                        trackIndexInGroup = i,
                        label = label,
                        isSelected = group.isTrackSelected(i),
                    )
                )
            }
        }
        return options
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        saveProgressNow()
        player.release()
        super.onCleared()
    }
}
