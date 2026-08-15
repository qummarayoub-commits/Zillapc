package com.darkjade.streamlib

import android.content.Context
import com.darkjade.streamlib.data.metadata.MetadataProvider
import com.darkjade.streamlib.data.metadata.comicvine.ComicVineMetadataProvider
import com.darkjade.streamlib.data.metadata.tmdb.TmdbMetadataProvider
import com.darkjade.streamlib.data.repository.ComicRepository
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.PlaybackRepository
import com.darkjade.streamlib.data.repository.PreferencesRepository
import com.darkjade.streamlib.data.repository.ProfileRepository
import com.darkjade.streamlib.data.repository.WatchRepository

/**
 * Lightweight, hand-rolled DI container. Deliberately avoids Hilt/Dagger
 * to keep the build graph simple and reduce first-build failure surface —
 * easy to swap for Hilt later if the project grows.
 */
class AppContainer(context: Context) {
    val metadataProvider: MetadataProvider = TmdbMetadataProvider()
    val libraryRepository = LibraryRepository(context, metadataProvider)
    val profileRepository = ProfileRepository(context)
    val watchRepository = WatchRepository(context)
    val preferencesRepository = PreferencesRepository(context)
    val comicVineMetadataProvider = ComicVineMetadataProvider()
    val comicRepository = ComicRepository(context, comicVineMetadataProvider)
    val playbackRepository = PlaybackRepository(context)
}
