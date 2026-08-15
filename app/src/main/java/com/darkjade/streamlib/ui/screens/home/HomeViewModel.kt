package com.darkjade.streamlib.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.repository.ComicRepository
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.ProfileRepository
import com.darkjade.streamlib.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val hero: HeroCandidate? = null,
    val movieBanner: MediaItemEntity? = null,
    val seriesBanner: MediaItemEntity? = null,
    val comicsBanner: ComicEntity? = null,
    val continueWatching: List<MediaItemEntity> = emptyList(),
    val recentlyAdded: List<MediaItemEntity> = emptyList(),
    val movies: List<MediaItemEntity> = emptyList(),
    val series: List<MediaItemEntity> = emptyList(),
    val anime: List<MediaItemEntity> = emptyList(),
    val comics: List<ComicEntity> = emptyList(),
    val libraryEmpty: Boolean = false,
)

/** Groups the five MediaItem-based flows so they can be combined with the comics flow separately —
 * kotlinx.coroutines' typed `combine` only supports up to 5 flows directly. */
private data class MediaFlows(
    val recentlyAdded: List<MediaItemEntity>,
    val movies: List<MediaItemEntity>,
    val series: List<MediaItemEntity>,
    val anime: List<MediaItemEntity>,
    val continueWatching: List<MediaItemEntity>,
)

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Each banner slot (main hero + the three category banners) remembers its
    // own pick for the lifetime of this screen/app session, so it doesn't jump
    // around on every recomposition — but a fresh app open always re-rolls,
    // so the banner is never stuck showing the same poster every single time.
    private var pinnedHeroId: Long? = null
    private var pinnedHeroIsComic: Boolean = false
    private var pinnedMovieBannerId: Long? = null
    private var pinnedSeriesBannerId: Long? = null
    private var pinnedComicsBannerId: Long? = null

    init {
        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()

            val mediaFlows = combine(
                libraryRepository.observeRecentlyAdded(20),
                libraryRepository.observeByType(MediaType.MOVIE),
                libraryRepository.observeByType(MediaType.SERIES),
                libraryRepository.observeByType(MediaType.ANIME),
                watchRepository.observeContinueWatching(profile.id),
            ) { recentlyAdded, movies, series, anime, continueWatching ->
                MediaFlows(recentlyAdded, movies, series, anime, continueWatching)
            }

            mediaFlows.combine(comicRepository.observeRecentlyAdded(20)) { flows, comics ->
                val pool = buildList {
                    addAll(flows.recentlyAdded.map { HeroCandidate.Media(it) })
                    addAll(flows.movies.map { HeroCandidate.Media(it) })
                    addAll(flows.series.map { HeroCandidate.Media(it) })
                    addAll(comics.map { HeroCandidate.Comic(it) })
                }.distinctBy { it.id to (it is HeroCandidate.Comic) }

                val hero = if (pool.isEmpty()) {
                    null
                } else {
                    val stillValid = pool.firstOrNull {
                        it.id == pinnedHeroId && (it is HeroCandidate.Comic) == pinnedHeroIsComic
                    }
                    stillValid ?: pool.random().also {
                        pinnedHeroId = it.id
                        pinnedHeroIsComic = it is HeroCandidate.Comic
                    }
                }

                val movieBanner = pickPinned(flows.movies, pinnedMovieBannerId) { pinnedMovieBannerId = it }
                val seriesBanner = pickPinned(flows.series, pinnedSeriesBannerId) { pinnedSeriesBannerId = it }
                val comicsBanner = pickPinned(comics, pinnedComicsBannerId) { pinnedComicsBannerId = it }

                HomeUiState(
                    isLoading = false,
                    hero = hero,
                    movieBanner = movieBanner,
                    seriesBanner = seriesBanner,
                    comicsBanner = comicsBanner,
                    continueWatching = flows.continueWatching,
                    recentlyAdded = flows.recentlyAdded,
                    movies = flows.movies,
                    series = flows.series,
                    anime = flows.anime,
                    comics = comics,
                    libraryEmpty = flows.recentlyAdded.isEmpty() && flows.movies.isEmpty() &&
                        flows.series.isEmpty() && flows.anime.isEmpty() && comics.isEmpty(),
                )
            }.onEach { _uiState.value = it }.launchIn(viewModelScope)
        }
    }

    private fun <T> pickPinned(pool: List<T>, pinnedId: Long?, setPinned: (Long) -> Unit): T? where T : Any {
        if (pool.isEmpty()) return null
        val idOf: (T) -> Long = { item ->
            when (item) {
                is MediaItemEntity -> item.id
                is ComicEntity -> item.id
                else -> 0L
            }
        }
        val stillValid = pool.firstOrNull { idOf(it) == pinnedId }
        return stillValid ?: pool.random().also { setPinned(idOf(it)) }
    }
}
