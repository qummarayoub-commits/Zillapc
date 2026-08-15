package com.darkjade.streamlib

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.darkjade.streamlib.data.metadata.comicvine.ComicVineConfig
import com.darkjade.streamlib.data.metadata.tmdb.TmdbConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreamLibApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Loaded from local.properties (TMDB_API_KEY) at build time via BuildConfig —
        // see app/build.gradle.kts. Never hardcoded in source, never committed to git.
        TmdbConfig.apiKey = BuildConfig.TMDB_API_KEY
        container = AppContainer(this)

        // Comic Vine key: prefer whatever the user has explicitly saved in
        // Settings; if nothing saved yet, fall back to the build-time default
        // (from local.properties/COMICVINE_API_KEY) and persist it once so
        // future edits in Settings always take precedence from then on.
        CoroutineScope(Dispatchers.IO).launch {
            val saved = container.preferencesRepository.getComicVineApiKey()
            val effective = if (saved.isNullOrBlank()) BuildConfig.COMICVINE_API_KEY else saved
            ComicVineConfig.apiKey = effective
            if (saved.isNullOrBlank() && effective.isNotBlank()) {
                container.preferencesRepository.setComicVineApiKey(effective)
            }
        }

        // Registers a video-frame decoder so that when a movie/episode has no
        // TMDB poster/thumbnail, Coil can decode a frame directly from the
        // local video file itself as a fallback thumbnail — the same idea
        // Nova/most local players use for unmatched files.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
        )

        // News: periodic background refresh so the feed is already current
        // when the user opens it, without hammering the network — WorkManager
        // handles battery/connectivity constraints and won't run more often
        // than this interval.
        val newsWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.darkjade.streamlib.work.NewsFetchWorker>(
            6, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.darkjade.streamlib.work.NewsFetchWorker.UNIQUE_WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            newsWorkRequest,
        )
    }
}
