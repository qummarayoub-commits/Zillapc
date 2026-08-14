package com.darkjade.streamlib

import android.app.Application
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
    }
}
