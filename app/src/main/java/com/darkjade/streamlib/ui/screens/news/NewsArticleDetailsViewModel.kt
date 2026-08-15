package com.darkjade.streamlib.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NewsArticleDetailsUiState(
    val isLoading: Boolean = true,
    val article: NewsArticleEntity? = null,
)

class NewsArticleDetailsViewModel(
    private val articleId: Long,
    private val newsRepository: NewsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsArticleDetailsUiState())
    val uiState: StateFlow<NewsArticleDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val article = newsRepository.getById(articleId)
            _uiState.value = NewsArticleDetailsUiState(isLoading = false, article = article)
        }
    }
}
