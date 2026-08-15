package com.darkjade.streamlib.ui.screens.comics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComicDetailsUiState(
    val isLoading: Boolean = true,
    val comic: ComicEntity? = null,
)

class ComicDetailsViewModel(
    private val comicId: Long,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicDetailsUiState())
    val uiState: StateFlow<ComicDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            comicRepository.observeById(comicId).collect { comic ->
                _uiState.value = ComicDetailsUiState(isLoading = false, comic = comic)
            }
        }
    }

    fun remove(onRemoved: () -> Unit) {
        viewModelScope.launch {
            comicRepository.removeComic(comicId)
            onRemoved()
        }
    }
}
