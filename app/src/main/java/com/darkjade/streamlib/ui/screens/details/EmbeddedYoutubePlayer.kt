package com.darkjade.streamlib.ui.screens.details

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Plays a trailer INSIDE the app via YouTube's embed player in a WebView —
 * this is the realistic in-app path for trailers (TMDB only ever gives us a
 * YouTube video key, not a raw downloadable file, and downloading/streaming
 * YouTube's actual video stream outside their own player isn't something
 * this app does). No external browser tab or YouTube app is opened.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedYoutubePlayer(youtubeKey: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                loadUrl("https://www.youtube.com/embed/$youtubeKey?autoplay=1&playsinline=1")
            }
        },
        update = { webView ->
            // Nothing to update on recomposition — the key doesn't change without recreating this composable.
        }
    )
}
