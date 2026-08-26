package com.darkjade.streamlib.ui.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the actual audio ExoPlayer + MediaSession. Running as a foreground
 * service is what lets music keep playing when the screen turns off or the
 * user switches to another app — a bare ExoPlayer instance living only in a
 * ViewModel gets torn down once the process loses foreground priority.
 *
 * The video player (PlayerViewModel) is untouched — it creates and owns its
 * own separate ExoPlayer instance scoped to the Player screen, exactly as
 * before. This service only ever plays audio.
 */
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
