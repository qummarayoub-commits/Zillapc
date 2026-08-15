package com.darkjade.streamlib.ui.screens.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.PlaybackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "DarkVaultPlayer"

/**
 * Human-readable codec name from a Format's MIME type — used for both audio
 * and text (subtitle) tracks so the UI never shows a raw MIME string or,
 * worse, confuses a language code for a codec name.
 */
private fun codecLabelFor(mimeType: String?): String = when (mimeType) {
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_MPEG_L1, MimeTypes.AUDIO_MPEG_L2 -> "MP3"
    MimeTypes.AUDIO_AC3 -> "AC3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    MimeTypes.AUDIO_RAW -> "PCM"
    MimeTypes.AUDIO_ALAC -> "ALAC"
    MimeTypes.AUDIO_AMR_NB, MimeTypes.AUDIO_AMR_WB -> "AMR"
    MimeTypes.APPLICATION_SUBRIP -> "SRT"
    MimeTypes.TEXT_VTT -> "WebVTT"
    MimeTypes.APPLICATION_TTML -> "TTML"
    MimeTypes.TEXT_SSA -> "ASS/SSA"
    MimeTypes.APPLICATION_PGS -> "PGS (image)"
    MimeTypes.APPLICATION_DVBSUBS -> "DVB"
    null -> "Unknown"
    else -> mimeType.substringAfterLast('/').uppercase()
}

/**
 * Human-readable language name from an ISO language code using the
 * platform's own Locale data — deliberately not a hardcoded language list,
 * so any language present in the file's actual metadata (Hindi, Tamil,
 * Telugu, Malayalam, Urdu, English, or anything else) resolves correctly.
 */
private fun languageLabelFor(languageCode: String?): String? {
    if (languageCode.isNullOrBlank() || languageCode.equals("und", ignoreCase = true)) return null
    return try {
        val displayName = Locale(languageCode).displayName
        // If Locale couldn't resolve it, displayName just echoes the code back — not useful.
        if (displayName.equals(languageCode, ignoreCase = true)) null else displayName
    } catch (e: Exception) {
        null
    }
}

data class AudioTrackOption(
    val group: Tracks.Group,
    val trackIndexInGroup: Int,
    val label: String,
    val isSelected: Boolean,
    val isSupportedByDevice: Boolean,
)

data class TextTrackOption(
    val group: Tracks.Group,
    val trackIndexInGroup: Int,
    val label: String,
    val isSelected: Boolean,
    val isSupportedByDevice: Boolean,
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val errorMessage: String? = null,
    val audioTracks: List<AudioTrackOption> = emptyList(),
    val textTracks: List<TextTrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val positionMs: Long = 0,

    /** Shown as a brief banner when the audio track couldn't be decoded but video kept playing. */
    val audioUnavailableNotice: String? = null,
)

/**
 * Owns the Media3/ExoPlayer instance for one movie/episode.
 *
 * Never scans or identifies media itself — it only resolves the existing URI
 * from the existing LibraryRepository by the existing movie/episode ID.
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

        // Prefer extension decoders when available.
        DefaultRenderersFactory(appContext)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            .setEnableDecoderFallback(true)
    )
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
        .apply {
            volume = 1f
        }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var positionPollJob: Job? = null
    private var hasResumed = false
    private var alreadyRetriedWithoutAudio = false
    private var currentMediaUriString: String? = null
    private val externalSubtitles = mutableListOf<MediaItem.SubtitleConfiguration>()

    init {
        player.addListener(
            object : Player.Listener {

                override fun onPlayerError(error: PlaybackException) {
                    handlePlaybackError(error)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val d = player.duration

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            durationMs =
                                if (d != C.TIME_UNSET && d > 0) {
                                    d
                                } else {
                                    _uiState.value.durationMs
                                }
                        )

                        if (!hasResumed) {
                            hasResumed = true
                            resumeSavedPosition()
                        }

                    } else if (playbackState == Player.STATE_ENDED) {
                        // Playback finished — save immediately as completed.
                        saveProgressNow(forceCompleted = true)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = isPlaying
                    )
                }

                override fun onTracksChanged(tracks: Tracks) {
                    logAudioTrackDiagnostics(tracks)

                    _uiState.value = _uiState.value.copy(
                        audioTracks = buildAudioTrackOptions(tracks),
                        textTracks = buildTextTrackOptions(tracks),
                    )
                }
            }
        )

        startPositionPoll()

        viewModelScope.launch {
            try {
                val (uriString, title) = resolveMedia()

                if (uriString.isNullOrBlank()) {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        errorMessage = "Could not find this file."
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    title = title
                )

                currentMediaUriString = uriString

                player.setMediaItem(
                    MediaItem.fromUri(
                        Uri.parse(uriString)
                    )
                )

                player.prepare()
                player.playWhenReady = true

                startAutoSave()

            } catch (e: Exception) {
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Playback error"
                )
            }
        }
    }

    /**
     * Investigates the real cause of a playback error.
     *
     * If the failure is specifically the audio decoder for this track,
     * disables that audio track and retries so video keeps playing.
     */
    private fun handlePlaybackError(
        error: PlaybackException
    ) {
        val exoError = error as? ExoPlaybackException
        val failedFormat = exoError?.rendererFormat

        val isAudioRendererFailure =
            exoError?.type == ExoPlaybackException.TYPE_RENDERER &&
                failedFormat?.sampleMimeType?.startsWith("audio/") == true

        Log.e(
            TAG,
            buildString {
                append(
                    "Playback error: " +
                        "errorCode=${error.errorCode} " +
                        "(${error.errorCodeName})"
                )

                append(" rendererType=${exoError?.type}")

                if (failedFormat != null) {
                    append(
                        " | failedTrack mime=${failedFormat.sampleMimeType}"
                    )

                    append(
                        " channels=${failedFormat.channelCount}"
                    )

                    // Media3 Format uses `sampleRate`, not `sampleRateHz`.
                    append(
                        " sampleRate=${failedFormat.sampleRate}"
                    )

                    append(
                        " bitrate=${failedFormat.bitrate}"
                    )

                    append(
                        " language=${failedFormat.language}"
                    )

                    append(
                        " codecs=${failedFormat.codecs}"
                    )
                }
            },
            error
        )

        if (
            isAudioRendererFailure &&
            !alreadyRetriedWithoutAudio
        ) {
            alreadyRetriedWithoutAudio = true

            val mimeLabel =
                failedFormat
                    ?.sampleMimeType
                    ?.removePrefix("audio/")
                    ?.uppercase()
                    ?: "THIS"

            Log.w(
                TAG,
                "Disabling unsupported audio track " +
                    "($mimeLabel) and retrying video-only playback."
            )

            val resumePosition = player.currentPosition

            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(
                        C.TRACK_TYPE_AUDIO,
                        true
                    )
                    .build()

            _uiState.value = _uiState.value.copy(
                errorMessage = null,
                audioUnavailableNotice =
                    "Audio format ($mimeLabel) isn't supported " +
                        "on this device — playing video only."
            )

            player.prepare()
            player.seekTo(resumePosition)
            player.playWhenReady = true

        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = buildUserFacingErrorMessage(
                    error,
                    exoError,
                    failedFormat
                )
            )
        }
    }

    private fun buildUserFacingErrorMessage(
        error: PlaybackException,
        exoError: ExoPlaybackException?,
        failedFormat: Format?,
    ): String {

        return when {
            failedFormat != null -> {
                val codec =
                    failedFormat
                        .sampleMimeType
                        ?.substringAfter('/')
                        ?.uppercase()
                        ?: "unknown"

                "This file's ${
                    if (
                        exoError?.type ==
                        ExoPlaybackException.TYPE_RENDERER
                    ) {
                        "media"
                    } else {
                        ""
                    }
                } format ($codec) isn't supported on this device."
            }

            error.errorCode ==
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                "This file could no longer be found."
            }

            error.errorCode ==
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> {
                "Permission to access this file was lost."
            }

            else -> {
                error.message
                    ?: "This video could not be played."
            }
        }
    }

    /**
     * Logs full diagnostic information for every detected audio track.
     *
     * This is useful for identifying which codecs/formats are failing.
     */
    private fun logAudioTrackDiagnostics(
        tracks: Tracks
    ) {
        val audioGroups =
            tracks.groups.filter {
                it.type == C.TRACK_TYPE_AUDIO
            }

        Log.d(
            TAG,
            "Audio tracks detected: " +
                audioGroups.sumOf { it.length }
        )

        audioGroups.forEach { group ->

            for (i in 0 until group.length) {

                val format =
                    group.getTrackFormat(i)

                Log.d(
                    TAG,
                    buildString {

                        append(
                            "Audio track: " +
                                "mime=${format.sampleMimeType}"
                        )

                        append(
                            " codecs=${format.codecs}"
                        )

                        append(
                            " channels=${format.channelCount}"
                        )

                        // Media3 Format uses `sampleRate`.
                        append(
                            " sampleRate=${format.sampleRate}"
                        )

                        append(
                            " bitrate=${format.bitrate}"
                        )

                        append(
                            " language=" +
                                "${format.language ?: "unknown"}"
                        )

                        append(
                            " selected=" +
                                "${group.isTrackSelected(i)}"
                        )

                        append(
                            " supportedByDevice=" +
                                "${group.isTrackSupported(i)}"
                        )
                    }
                )
            }
        }
    }

    /**
     * Drives the seek bar.
     *
     * ExoPlayer doesn't push continuous position updates on its own.
     */
    private fun startPositionPoll() {
        positionPollJob?.cancel()

        positionPollJob = viewModelScope.launch {

            while (true) {

                delay(500)

                val rawDuration =
                    player.duration

                // Some formats report C.TIME_UNSET until fully determined.
                val safeDuration =
                    if (
                        rawDuration != C.TIME_UNSET &&
                        rawDuration > 0
                    ) {
                        rawDuration
                    } else {
                        _uiState.value.durationMs
                    }

                _uiState.value = _uiState.value.copy(
                    positionMs =
                        player.currentPosition.coerceAtLeast(0),
                    durationMs = safeDuration
                )
            }
        }
    }

    private suspend fun resolveMedia(): Pair<String?, String> {

        return if (episodeId != null) {

            val episode =
                libraryRepository.getEpisode(episodeId)

            val media =
                libraryRepository.getMediaItem(mediaId)

            val title = buildString {

                append(
                    media?.title ?: "Episode"
                )

                episode?.let {
                    append(
                        " · E${it.episodeNumber}"
                    )
                }

                episode?.title?.let {

                    if (it.isNotBlank()) {
                        append(
                            " - $it"
                        )
                    }
                }
            }

            (episode?.localFileUri) to title

        } else {

            val media =
                libraryRepository.getMediaItem(mediaId)

            (media?.localFileUri) to
                (media?.title.orEmpty())
        }
    }

    private fun resumeSavedPosition() {

        viewModelScope.launch {

            val saved =
                playbackRepository.getProgress(
                    mediaId,
                    episodeId
                )

            if (
                saved != null &&
                saved.positionMs > 0 &&
                !saved.completed
            ) {
                player.seekTo(
                    saved.positionMs
                )
            }
        }
    }

    private fun startAutoSave() {

        autoSaveJob?.cancel()

        autoSaveJob = viewModelScope.launch {

            while (true) {

                delay(10_000)

                saveProgressNow()
            }
        }
    }

    /**
     * Called on pause, Back/exit, backgrounding,
     * finish, and periodically.
     */
    fun saveProgressNow(
        forceCompleted: Boolean = false
    ) {

        val duration =
            player.duration

        if (duration <= 0) {
            return
        }

        val position =
            if (forceCompleted) {
                duration
            } else {
                player.currentPosition
                    .coerceAtLeast(0)
            }

        viewModelScope.launch {

            playbackRepository.saveProgress(
                mediaId,
                episodeId,
                position,
                duration
            )
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

        val target =
            (
                player.currentPosition +
                    deltaMs
            ).coerceIn(
                0,
                player.duration.coerceAtLeast(0)
            )

        player.seekTo(target)

        _uiState.value =
            _uiState.value.copy(
                positionMs = target
            )
    }

    /** Used directly by the custom seek bar's drag gesture. */
    fun seekTo(positionMs: Long) {

        val target =
            positionMs.coerceIn(
                0,
                player.duration.coerceAtLeast(0)
            )

        player.seekTo(target)

        _uiState.value =
            _uiState.value.copy(
                positionMs = target
            )
    }

    fun setPlaybackSpeed(
        speed: Float
    ) {
        player.setPlaybackSpeed(speed)
    }

    fun selectAudioTrack(
        option: AudioTrackOption
    ) {

        val override =
            TrackSelectionOverride(
                option.group.mediaTrackGroup,
                option.trackIndexInGroup
            )

        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(override)
                .build()
    }

    fun dismissAudioNotice() {

        _uiState.value =
            _uiState.value.copy(
                audioUnavailableNotice = null
            )
    }

    fun selectTextTrack(option: TextTrackOption) {
        val override = TrackSelectionOverride(option.group.mediaTrackGroup, option.trackIndexInGroup)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(override)
            .build()
        _uiState.value = _uiState.value.copy(subtitlesEnabled = true)
    }

    /** Turns subtitles off entirely without forgetting which track was selected. */
    fun disableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        _uiState.value = _uiState.value.copy(subtitlesEnabled = false)
    }

    fun enableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        _uiState.value = _uiState.value.copy(subtitlesEnabled = true)
    }

    /**
     * Attaches a user-picked external subtitle file (.srt/.vtt/.ass/.ssa) to
     * the currently playing media and reloads it, resuming from the same
     * position. The file's own extension determines its MIME type — no
     * language is assumed or hardcoded; the track's displayed language
     * comes from whatever the user names it or leaves as "Track N" until
     * they pick a track (external files rarely carry language metadata
     * themselves, unlike embedded MKV/MP4 tracks).
     */
    fun addExternalSubtitle(uri: Uri, displayName: String) {
        val uriString = currentMediaUriString ?: return
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val mimeType = when (ext) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "ttml", "xml" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP // reasonable default for an unrecognized plain-text subtitle file
        }

        val label = displayName.substringBeforeLast('.', displayName)
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        externalSubtitles.add(subtitleConfig)

        val resumePosition = player.currentPosition
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(uriString))
            .setSubtitleConfigurations(externalSubtitles.toList())
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(resumePosition)
        player.playWhenReady = true
    }

    private fun buildAudioTrackOptions(
        tracks: Tracks
    ): List<AudioTrackOption> {

        val options =
            mutableListOf<AudioTrackOption>()

        for (group in tracks.groups) {

            if (group.type != C.TRACK_TYPE_AUDIO) {
                continue
            }

            for (i in 0 until group.length) {

                val format =
                    group.getTrackFormat(i)

                // "English — AAC", "Hindi — AC3" — language is metadata
                // (resolved via Locale, never hardcoded), codec comes from
                // the actual sampleMimeType, never confused with each other.
                val languageLabel = languageLabelFor(format.language)
                val codecLabel = codecLabelFor(format.sampleMimeType)
                val label = if (languageLabel != null) "$languageLabel \u2014 $codecLabel"
                    else format.label ?: "Track ${i + 1} \u2014 $codecLabel"

                options.add(
                    AudioTrackOption(
                        group = group,
                        trackIndexInGroup = i,
                        label = label,
                        isSelected =
                            group.isTrackSelected(i),
                        isSupportedByDevice =
                            group.isTrackSupported(i)
                    )
                )
            }
        }

        return options
    }

    private fun buildTextTrackOptions(
        tracks: Tracks
    ): List<TextTrackOption> {

        val options = mutableListOf<TextTrackOption>()

        for (group in tracks.groups) {

            if (group.type != C.TRACK_TYPE_TEXT) {
                continue
            }

            for (i in 0 until group.length) {

                val format = group.getTrackFormat(i)

                val languageLabel = languageLabelFor(format.language)
                val codecLabel = codecLabelFor(format.sampleMimeType)
                val label = if (languageLabel != null) "$languageLabel \u2014 $codecLabel"
                    else format.label ?: "Track ${i + 1} \u2014 $codecLabel"

                options.add(
                    TextTrackOption(
                        group = group,
                        trackIndexInGroup = i,
                        label = label,
                        isSelected = group.isTrackSelected(i),
                        isSupportedByDevice = group.isTrackSupported(i),
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
