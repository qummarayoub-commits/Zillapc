package com.darkjade.streamlib.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
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
    val hero: MediaItemEntity? = null,
    val continueWatching: List<MediaItemEntity> = emptyList(),
    val recentlyAdded: List<MediaItemEntity> = emptyList(),
    val movies: List<MediaItemEntity> = emptyList(),
    val series: List<MediaItemEntity> = emptyList(),
    val anime: List<MediaItemEntity> = emptyList(),
    val libraryEmpty: Boolean = false,
)

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeProfileId: Long? = null

    init {
        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()
            activeProfileId = profile.id

            combine(
                libraryRepository.observeRecentlyAdded(20),
                libraryRepository.observeByType(MediaType.MOVIE),
                libraryRepository.observeByType(MediaType.SERIES),
                libraryRepository.observeByType(MediaType.ANIME),
                watchRepository.observeContinueWatching(profile.id),
            ) { recentlyAdded, movies, series, anime, continueWatching ->
                HomeUiState(
                    isLoading = false,
                    hero = recentlyAdded.firstOrNull() ?: movies.firstOrNull() ?: series.firstOrNull(),
                    continueWatching = continueWatching,
                    recentlyAdded = recentlyAdded,
                    movies = movies,
                    series = series,
                    anime = anime,
                    libraryEmpty = recentlyAdded.isEmpty() && movies.isEmpty() && series.isEmpty() && anime.isEmpty(),
                )
            }.onEach { _uiState.value = it }.launchIn(viewModelScope)
        }
    }
}
