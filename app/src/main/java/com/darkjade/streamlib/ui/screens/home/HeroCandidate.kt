package com.darkjade.streamlib.ui.screens.home

import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity

/** Anything the Home hero banner can showcase — a movie/show/anime or a comic issue. */
sealed class HeroCandidate {
    abstract val id: Long
    abstract val title: String
    abstract val backdropUrl: String?
    abstract val overview: String?
    abstract val rating: Double?
    abstract val metaLine: String? // e.g. "2h 14m • Action • 2019"

    data class Media(val item: MediaItemEntity) : HeroCandidate() {
        override val id: Long get() = item.id
        override val title: String get() = item.title
        override val backdropUrl: String? get() = item.backdropUrl
        override val overview: String? get() = item.overview
        override val rating: Double? get() = item.imdbRating ?: item.rating
        override val metaLine: String?
            get() {
                val bits = buildList {
                    item.runtimeMinutes?.takeIf { it > 0 }?.let {
                        add(if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m")
                    }
                    item.genres.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                    item.year?.let { add(it.toString()) }
                }
                return bits.takeIf { it.isNotEmpty() }?.joinToString("  \u2022  ")
            }
    }

    data class Comic(val item: ComicEntity) : HeroCandidate() {
        override val id: Long get() = item.id
        override val title: String get() = item.title
        override val backdropUrl: String? get() = item.coverUrl
        override val overview: String? get() = item.overview
        override val rating: Double? get() = null
        override val metaLine: String? get() = item.seriesName?.takeIf { it.isNotBlank() }
    }
}
