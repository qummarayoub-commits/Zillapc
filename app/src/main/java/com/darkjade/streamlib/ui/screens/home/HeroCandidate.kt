package com.darkjade.streamlib.ui.screens.home

import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity

/** Anything the Home hero banner can showcase — a movie/show/anime or a comic issue. */
sealed class HeroCandidate {
    abstract val id: Long
    abstract val title: String
    abstract val backdropUrl: String?
    abstract val overview: String?

    data class Media(val item: MediaItemEntity) : HeroCandidate() {
        override val id: Long get() = item.id
        override val title: String get() = item.title
        override val backdropUrl: String? get() = item.backdropUrl
        override val overview: String? get() = item.overview
    }

    data class Comic(val item: ComicEntity) : HeroCandidate() {
        override val id: Long get() = item.id
        override val title: String get() = item.title
        override val backdropUrl: String? get() = item.coverUrl
        override val overview: String? get() = item.overview
    }
}
