package com.darkjade.streamlib

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.darkjade.streamlib.data.metadata.tmdb.TmdbConfig

class StreamLibApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Loaded from local.properties (TMDB_API_KEY) at build time via BuildConfig —
        // see app/build.gradle.kts. Never hardcoded in source, never committed to git.
        TmdbConfig.apiKey = BuildConfig.TMDB_API_KEY
        container = AppContainer(this)

        // Registers a video-frame decoder so that when a movie/episode has no
        // TMDB poster/thumbnail, Coil can decode a frame directly from the
        // local video file itself as a fallback thumbnail — the same idea
        // Nova/most local players use for unmatched files.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
        )
    }
}
