package com.darkjade.streamlib.ui.screens.newreleases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.repository.NewReleasesRepository
import com.darkjade.streamlib.data.repository.ReleaseBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the "New" page: every themed release row (this week, Netflix, family...),
 * the full this-year-and-last-year list, and the "Available for Stream" section.
 * Shares NewReleasesRepository (and its disk cache) with Home, so opening this
 * page right after Home is instant.
 */
class NewReleasesViewModel(
    private val repository: NewReleasesRepository,
) : ViewModel() {

    val board: StateFlow<ReleaseBoard> = repository.observeBoard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReleaseBoard())

    private val _refreshing = MutableStateFlow(true)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.refresh(force)
            } finally {
                _refreshing.value = false
            }
        }
    }
}
