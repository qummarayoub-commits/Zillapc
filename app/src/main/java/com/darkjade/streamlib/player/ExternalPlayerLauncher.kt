package com.darkjade.streamlib.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap

sealed class PlaybackLaunchResult {
    object Launched : PlaybackLaunchResult()
    object NoPlayerFound : PlaybackLaunchResult()
    data class Failed(val message: String) : PlaybackLaunchResult()
}

/**
 * This app deliberately contains NO video player. Playback is always
 * delegated to whatever the user has installed via ACTION_VIEW, exactly
 * per Phase 12 of the spec. If multiple players are installed, Android's
 * own chooser is shown automatically — we don't build a custom one.
 */
object ExternalPlayerLauncher {

    fun play(context: Context, fileUri: Uri, displayName: String? = null): PlaybackLaunchResult {
        return try {
            val mimeType = guessMimeType(fileUri, displayName) ?: "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PlaybackLaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            PlaybackLaunchResult.NoPlayerFound
        } catch (e: Exception) {
            PlaybackLaunchResult.Failed(e.message ?: "Unable to open player")
        }
    }

    private fun guessMimeType(uri: Uri, displayName: String?): String? {
        val extension = (displayName ?: uri.lastPathSegment.orEmpty())
            .substringAfterLast('.', "")
            .lowercase()
        if (extension.isBlank()) return "video/*"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "video/*"
    }
}
