package com.darkjade.streamlib.ui.util

import com.darkjade.streamlib.data.db.entity.MediaItemEntity

/**
 * TMDB has multiple poster images per title. Rather than picking one
 * forever at scan time, we keep the full pool (MediaItemEntity.
 * alternatePosterUrls) and pick a random one from it ONCE per app process
 * — cached here so it stays stable while the app is open, but naturally
 * varies again next time the app is launched.
 */
object PosterRotationCache {
    private val cache = mutableMapOf<Long, String?>()

    fun posterFor(item: MediaItemEntity): String? {
        return cache.getOrPut(item.id) {
            val pool = item.alternatePosterUrls.split(",").filter { it.isNotBlank() }
            if (pool.isNotEmpty()) pool.random() else item.posterUrl
        }
    }
}
