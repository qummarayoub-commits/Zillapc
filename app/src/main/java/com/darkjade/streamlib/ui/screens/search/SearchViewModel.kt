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
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<MediaItemEntity> = emptyList(),
    val hasSearched: Boolean = false,
)

class SearchViewModel(private val libraryRepository: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

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
