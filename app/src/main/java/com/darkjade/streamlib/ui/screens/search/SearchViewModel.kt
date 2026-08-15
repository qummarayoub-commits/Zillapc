package com.darkjade.streamlib.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<MediaItemEntity> = emptyList(),
    val hasSearched: Boolean = false,
    // Shown by default before the user types anything, so the screen is
    // never just an empty search box.
    val todaysTopPicks: List<MediaItemEntity> = emptyList(),
    val weeklyPicks: List<MediaItemEntity> = emptyList(),
    val recentlyAdded: List<MediaItemEntity> = emptyList(),
)

class SearchViewModel(private val libraryRepository: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        libraryRepository.observeAll()
            .onEach { all ->
                if (all.isEmpty()) {
                    _uiState.value = _uiState.value.copy(todaysTopPicks = emptyList(), weeklyPicks = emptyList(), recentlyAdded = emptyList())
                    return@onEach
                }
                // "Today's Top Picks" / "Weekly Picks" are lightweight, deterministic-per-session
                // random samples of the library — there's no real popularity signal in a purely
                // local library, so this just keeps Search feeling alive instead of blank.
                val shuffled = all.shuffled()
                _uiState.value = _uiState.value.copy(
                    todaysTopPicks = shuffled.take(10),
                    weeklyPicks = shuffled.drop(10).take(10).ifEmpty { shuffled.take(10) },
                    recentlyAdded = all.sortedByDescending { it.dateAdded }.take(10),
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false, isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // debounce — search is fast/case-insensitive per spec, no need to hammer DB every keystroke
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = libraryRepository.search(query)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                hasSearched = true,
                results = result.mediaItems,
            )
        }
    }
}
