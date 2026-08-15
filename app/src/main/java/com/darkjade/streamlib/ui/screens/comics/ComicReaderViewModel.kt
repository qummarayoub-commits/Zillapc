package com.darkjade.streamlib.ui.screens.comics

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.comic.ComicExtractionResult
import com.darkjade.streamlib.data.comic.ComicExtractor
import com.darkjade.streamlib.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ComicReaderUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val pages: List<File> = emptyList(),
    val errorMessage: String? = null,
    /** Set when the format (e.g. cb7) isn't supported by the internal reader — caller falls back to an external app. */
    val unsupportedFormat: Boolean = false,
)

class ComicReaderViewModel(
    private val comicId: Long,
    private val appContext: Context,
    private val comicRepository: ComicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicReaderUiState())
    val uiState: StateFlow<ComicReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val comic = comicRepository.getById(comicId)
            if (comic == null) {
                _uiState.value = ComicReaderUiState(isLoading = false, errorMessage = "This comic is no longer in your library.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(title = comic.title)

            val comicUri = Uri.parse(comic.localFileUri)
            when (val result = ComicExtractor.extractPages(appContext, comicId, comicUri, comic.fileExtension)) {
                is ComicExtractionResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, pages = result.pages)
                }
                is ComicExtractionResult.Failed -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                ComicExtractionResult.UnsupportedFormat -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, unsupportedFormat = true)
                }
            }
        }
    }
}
