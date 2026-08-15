package com.darkjade.streamlib.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.db.entity.NewsCategory
import com.darkjade.streamlib.data.repository.ComicRepository
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.NewsRefreshResult
import com.darkjade.streamlib.data.repository.NewsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val DEFAULT_ARTICLE_LIMIT = 10
private const val LOAD_MORE_INCREMENT = 10

data class NewsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedCategory: NewsCategory? = null, // null = "All"
    val query: String = "",
    val searchResults: List<NewsArticleEntity>? = null, // null = not searching, show the category feed instead
    val allArticles: List<NewsArticleEntity> = emptyList(),
    val visibleCount: Int = DEFAULT_ARTICLE_LIMIT,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    // Quick-jump shortcut banners at the top of News — one sample item per category.
    val movieBanner: MediaItemEntity? = null,
    val seriesBanner: MediaItemEntity? = null,
    val comicBanner: ComicEntity? = null,
)

class NewsViewModel(
    private val newsRepository: NewsRepository,
    private val libraryRepository: LibraryRepository,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        newsRepository.observeAll()
            .onEach { articles ->
                _uiState.value = _uiState.value.copy(isLoading = false, allArticles = articles)
            }
            .launchIn(viewModelScope)

        // First-open refresh: if we already have cached articles, show them
        // immediately (offline-first) and refresh quietly in the background;
        // if the cache is empty, this is the load the user actually waits on.
        viewModelScope.launch {
            val hasCache = newsRepository.hasAnyCachedArticles()
            refresh(silentIfCached = hasCache)
        }

        // Top shortcut banners — one representative item per category, if the library has any.
        viewModelScope.launch {
            val movie = libraryRepository.observeByType(MediaType.MOVIE).first().randomOrNull()
            val series = libraryRepository.observeByType(MediaType.SERIES).first().randomOrNull()
                ?: libraryRepository.observeByType(MediaType.ANIME).first().randomOrNull()
            val comic = comicRepository.observeAll().first().randomOrNull()
            _uiState.value = _uiState.value.copy(movieBanner = movie, seriesBanner = series, comicBanner = comic)
        }
    }

    fun refresh(silentIfCached: Boolean = false) {
        viewModelScope.launch {
            if (!silentIfCached) _uiState.value = _uiState.value.copy(isRefreshing = true)
            when (val result = newsRepository.refreshAll()) {
                is NewsRefreshResult.Offline -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isOffline = _uiState.value.allArticles.isEmpty(),
                        errorMessage = if (_uiState.value.allArticles.isEmpty()) "No internet connection" else null,
                    )
                }
                is NewsRefreshResult.Success, is NewsRefreshResult.PartialFailure -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false, isOffline = false, errorMessage = null)
                }
            }
        }
    }

    fun setCategory(category: NewsCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category, visibleCount = DEFAULT_ARTICLE_LIMIT)
    }

    fun loadMore() {
        _uiState.value = _uiState.value.copy(visibleCount = _uiState.value.visibleCount + LOAD_MORE_INCREMENT)
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            val results = newsRepository.search(query)
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }
}
