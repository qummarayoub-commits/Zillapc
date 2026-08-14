package com.darkjade.streamlib.ui.screens.mylists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.ProfileRepository
import com.darkjade.streamlib.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MyListsTab { WATCHLIST, HISTORY, CONTINUE_WATCHING }

data class MyListsUiState(
    val isLoading: Boolean = true,
    val tab: MyListsTab = MyListsTab.WATCHLIST,
    val watchlist: List<MediaItemEntity> = emptyList(),
    val continueWatching: List<MediaItemEntity> = emptyList(),
    val historyMediaIds: List<Long> = emptyList(),
)

class MyListsViewModel(
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListsUiState())
    val uiState: StateFlow<MyListsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()

            launch {
                watchRepository.observeWatchlist(profile.id).collect { list ->
                    _uiState.value = _uiState.value.copy(isLoading = false, watchlist = list)
                }
            }
            launch {
                watchRepository.observeContinueWatching(profile.id).collect { list ->
                    _uiState.value = _uiState.value.copy(continueWatching = list)
                }
            }
        }
    }

    fun setTab(tab: MyListsTab) {
        _uiState.value = _uiState.value.copy(tab = tab)
    }
}
