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
 * This app deliberately contains NO video player (or comic reader).
 * Playback/reading is always delegated to whatever the user has installed
 * via ACTION_VIEW, exactly per Phase 12 of the spec. If multiple apps are
 * installed and no default player preference is set, Android's own
 * chooser is shown automatically — we don't build a custom one.
 */
object ExternalPlayerLauncher {

    /**
     * @param preferredPackage if set, launches that package directly
     *   (skipping Android's chooser) — see Settings > Default Player. If
     *   that package can no longer handle the file (e.g. uninstalled),
     *   transparently falls back to the normal chooser instead of failing.
     */
    fun play(context: Context, fileUri: Uri, displayName: String? = null, preferredPackage: String? = null): PlaybackLaunchResult {
        val mimeType = guessMimeType(fileUri, displayName, fallback = "video/*")
        return launch(context, fileUri, mimeType, preferredPackage)
    }

    /** Opens a comic file (.cbz/.cbr/.cb7/.pdf) with whatever reader app the user has installed. */
    fun openComic(context: Context, fileUri: Uri, displayName: String? = null): PlaybackLaunchResult {
        val mimeType = guessMimeType(fileUri, displayName, fallback = "*/*")
        return launch(context, fileUri, mimeType, preferredPackage = null)
    }

    private fun launch(context: Context, fileUri: Uri, mimeType: String, preferredPackage: String?): PlaybackLaunchResult {
        fun buildIntent(withPackage: String?) = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            withPackage?.let { setPackage(it) }
        }

        return try {
            if (!preferredPackage.isNullOrBlank()) {
                try {
                    context.startActivity(buildIntent(preferredPackage))
                    return PlaybackLaunchResult.Launched
                } catch (e: ActivityNotFoundException) {
                    // Preferred app no longer available for this file — fall through to the chooser.
                }
            }
            context.startActivity(buildIntent(null))
            PlaybackLaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            PlaybackLaunchResult.NoPlayerFound
        } catch (e: Exception) {
            PlaybackLaunchResult.Failed(e.message ?: "Unable to open file")
        }
    }

    private fun guessMimeType(uri: Uri, displayName: String?, fallback: String): String {
        val extension = (displayName ?: uri.lastPathSegment.orEmpty())
            .substringAfterLast('.', "")
            .lowercase()
        if (extension.isBlank()) return fallback
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: fallback
    }
}
