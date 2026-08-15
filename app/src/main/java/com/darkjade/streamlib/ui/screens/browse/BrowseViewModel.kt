package com.darkjade.streamlib.ui.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.repository.ComicRepository
import com.darkjade.streamlib.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class BrowseCategory { ALL, MOVIES, SERIES, ANIME, COMICS }
enum class SortOrder { RECENTLY_ADDED, A_Z, YEAR }

data class BrowseUiState(
    val isLoading: Boolean = true,
    val category: BrowseCategory = BrowseCategory.ALL,
    val sortOrder: SortOrder = SortOrder.RECENTLY_ADDED,
    val allItems: List<MediaItemEntity> = emptyList(),
    val displayedItems: List<MediaItemEntity> = emptyList(),
    val comics: List<ComicEntity> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
)

class BrowseViewModel(
    private val libraryRepository: LibraryRepository,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        libraryRepository.observeAll()
            .onEach { items ->
                val genres = items.flatMap { it.genres.split(",") }
                    .map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allItems = items,
                    genres = genres,
                )
                applyFilters()
            }
            .launchIn(viewModelScope)

        comicRepository.observeAll()
            .onEach { comics ->
                _uiState.value = _uiState.value.copy(comics = comics)
            }
            .launchIn(viewModelScope)
    }

    fun setCategory(category: BrowseCategory) {
        _uiState.value = _uiState.value.copy(category = category)
        applyFilters()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
        applyFilters()
    }

    fun setGenre(genre: String?) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var items = state.allItems

        items = when (state.category) {
            BrowseCategory.ALL -> items
            BrowseCategory.MOVIES -> items.filter { it.type == MediaType.MOVIE }
            BrowseCategory.SERIES -> items.filter { it.type == MediaType.SERIES }
            BrowseCategory.ANIME -> items.filter { it.type == MediaType.ANIME }
            BrowseCategory.COMICS -> emptyList() // comics render from state.comics separately
        }

        state.selectedGenre?.let { genre ->
            items = items.filter { it.genres.contains(genre) }
        }

        items = when (state.sortOrder) {
            SortOrder.RECENTLY_ADDED -> items.sortedByDescending { it.dateAdded }
            SortOrder.A_Z -> items.sortedBy { it.sortTitle }
            SortOrder.YEAR -> items.sortedByDescending { it.year ?: 0 }
        }

        _uiState.value = state.copy(displayedItems = items)
    }
}
