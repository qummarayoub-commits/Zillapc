package com.darkjade.streamlib.ui.screens.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
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
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
)

/**
 * Owns the Media3/ExoPlayer instance for one movie/episode. Never scans or
 * identifies media itself — it only resolves the existing URI from the
 * existing LibraryRepository by the existing movie/episode ID, exactly per
 * the "additive only" requirement.
 *
 * Controls are fully custom (see PlayerScreen) rather than relying on
 * Media3's built-in PlayerView controller, whose visibility/touch-handling
 * quirks were causing real bugs (seek bar and rewind/forward not
 * responding, controls not hiding on tap). Owning the state directly here
 * — position, duration, isPlaying — makes every control unambiguously wired.
 */
class PlayerViewModel(
    private val mediaId: Long,
    private val episodeId: Long?,
    appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(
        appContext,
        // PREFER lets ExoPlayer fall back to any available extension decoder
        // (e.g. a bundled software audio decoder) when the device's own
        // hardware codec can't handle a track — the closest this app can get
        // to "support every audio format" without shipping a custom-built
        // FFmpeg decoder module, which isn't something that can be added via
        // a simple dependency (Google doesn't publish prebuilt AC-3/DTS
        // decoders due to codec licensing — apps like VLC ship their own
        // build of FFmpeg specifically to work around this).
        DefaultRenderersFactory(appContext).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    )
        // Explicit audio attributes + automatic audio-focus handling — the
        // default builder should already do this, but being explicit rules
        // out silent-audio caused by focus/routing not being requested.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()
        .apply { volume = 1f }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var positionPollJob: Job? = null
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
                    val d = player.duration
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        durationMs = if (d != C.TIME_UNSET && d > 0) d else _uiState.value.durationMs,
                    )
                    if (!hasResumed) {
                        hasResumed = true
                        resumeSavedPosition()
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    // Playback finished — save immediately as completed (Phase: COMPLETED STATUS).
                    saveProgressNow(forceCompleted = true)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onTracksChanged(tracks: Tracks) {
                _uiState.value = _uiState.value.copy(audioTracks = buildAudioTrackOptions(tracks))
            }
        })

        startPositionPoll()

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

    /** Drives the seek bar — ExoPlayer doesn't push continuous position updates on its own. */
    private fun startPositionPoll() {
        positionPollJob?.cancel()
        positionPollJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val rawDuration = player.duration
                // Some formats report C.TIME_UNSET (a huge negative
                // number) until fully determined — never let a garbage
                // duration reach the seek bar, since dividing by it breaks
                // the slider entirely (looks like "seeking doesn't work").
                val safeDuration = if (rawDuration != C.TIME_UNSET && rawDuration > 0) rawDuration
                    else _uiState.value.durationMs
                _uiState.value = _uiState.value.copy(
                    positionMs = player.currentPosition.coerceAtLeast(0),
                    durationMs = safeDuration,
                )
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
        if (player.isPlaying) {
            player.pause()
            saveProgressNow()
        } else {
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(positionMs = target)
    }

    /** Used directly by the custom seek bar's drag gesture. */
    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(positionMs = target)
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
            if (group.type != C.TRACK_TYPE_AUDIO) continue
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
        positionPollJob?.cancel()
        saveProgressNow()
        player.release()
        super.onCleared()
    }
}
