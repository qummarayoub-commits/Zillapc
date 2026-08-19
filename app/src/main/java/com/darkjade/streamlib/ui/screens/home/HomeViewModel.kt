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
    val heroItems: List<HeroCandidate> = emptyList(),
    val movieBanners: List<MediaItemEntity> = emptyList(),
    val seriesBanners: List<MediaItemEntity> = emptyList(),
    val animeBanners: List<MediaItemEntity> = emptyList(),
    val comicsBanners: List<ComicEntity> = emptyList(),
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

private const val HERO_CAROUSEL_SIZE = 6
private const val SECONDARY_BANNER_SIZE = 3

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Each carousel remembers its picks for the lifetime of this screen/app
    // session so items don't shuffle around on every recomposition — but a
    // fresh app open always re-rolls, so it's never the same lineup forever.
    private val pinnedHeroKeys = mutableListOf<Pair<Long, Boolean>>() // id to isComic
    private val pinnedMovieBannerIds = mutableListOf<Long>()
    private val pinnedSeriesBannerIds = mutableListOf<Long>()
    private val pinnedAnimeBannerIds = mutableListOf<Long>()
    private val pinnedComicsBannerIds = mutableListOf<Long>()

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
                val heroPool = buildList {
                    addAll(flows.recentlyAdded.map { HeroCandidate.Media(it) })
                    addAll(flows.movies.map { HeroCandidate.Media(it) })
                    addAll(flows.series.map { HeroCandidate.Media(it) })
                    addAll(comics.map { HeroCandidate.Comic(it) })
                }.distinctBy { it.id to (it is HeroCandidate.Comic) }

                val heroItems = pickPinnedHero(heroPool)
                val movieBanners = pickPinnedHomogeneous(flows.movies, pinnedMovieBannerIds) { it.id }
                val seriesBanners = pickPinnedHomogeneous(flows.series, pinnedSeriesBannerIds) { it.id }
                val animeBanners = pickPinnedHomogeneous(flows.anime, pinnedAnimeBannerIds) { it.id }
                val comicsBanners = pickPinnedHomogeneous(comics, pinnedComicsBannerIds) { it.id }

                HomeUiState(
                    isLoading = false,
                    heroItems = heroItems,
                    movieBanners = movieBanners,
                    seriesBanners = seriesBanners,
                    animeBanners = animeBanners,
                    comicsBanners = comicsBanners,
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

    private fun pickPinnedHero(pool: List<HeroCandidate>): List<HeroCandidate> {
        if (pool.isEmpty()) return emptyList()
        val target = minOf(HERO_CAROUSEL_SIZE, pool.size)
        val stillValid = pinnedHeroKeys.mapNotNull { (id, isComic) ->
            pool.firstOrNull { it.id == id && (it is HeroCandidate.Comic) == isComic }
        }
        if (stillValid.size >= target) return stillValid.take(target)

        val picked = pool.shuffled().take(target)
        pinnedHeroKeys.clear()
        pinnedHeroKeys.addAll(picked.map { it.id to (it is HeroCandidate.Comic) })
        return picked
    }

    private fun <T> pickPinnedHomogeneous(pool: List<T>, pinnedIds: MutableList<Long>, idOf: (T) -> Long): List<T> {
        if (pool.isEmpty()) return emptyList()
        val target = minOf(SECONDARY_BANNER_SIZE, pool.size)
        val stillValid = pinnedIds.mapNotNull { pid -> pool.firstOrNull { idOf(it) == pid } }
        if (stillValid.size >= target) return stillValid.take(target)

        val picked = pool.shuffled().take(target)
        pinnedIds.clear()
        pinnedIds.addAll(picked.map(idOf))
        return picked
    }

    fun addToWatchlist(item: MediaItemEntity) {
        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()
            watchRepository.addToWatchlist(profile.id, item.id)
        }
    }

    fun removeFromLibrary(item: MediaItemEntity) {
        viewModelScope.launch { libraryRepository.removeMediaItem(item.id) }
    }
}
