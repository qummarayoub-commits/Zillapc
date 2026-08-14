package com.darkjade.streamlib.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small generic factory so screens can construct ViewModels with repository args without Hilt. */
class SimpleViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
