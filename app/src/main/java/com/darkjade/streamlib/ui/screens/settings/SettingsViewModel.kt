package com.darkjade.streamlib.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.FolderSourceEntity
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import com.darkjade.streamlib.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val folderSources: List<FolderSourceEntity> = emptyList(),
    val scanStatus: ScanStatusEntity? = null,
)

class SettingsViewModel(
    private val appContext: Context,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                libraryRepository.observeFolderSources().collect { sources ->
                    _uiState.value = _uiState.value.copy(folderSources = sources)
                }
            }
            launch {
                libraryRepository.observeScanStatus().collect { status ->
                    _uiState.value = _uiState.value.copy(scanStatus = status)
                }
            }
        }
    }

    /**
     * Primary scan path: indexes the device's MediaStore video library.
     * No folder picking, no SAF permission grants — works consistently
     * across OEMs. Only needs the READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE
     * runtime permission, requested from SettingsScreen before this is called.
     */
    fun scanDeviceForVideos() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    libraryRepository.scanDeviceMediaStore { /* progress observed via DB flow */ }
                }
            } catch (e: Exception) {
                // Defense in depth — the repository already catches internally
                // and reports a FAILED scan status, this just guarantees the
                // app itself can never crash from a scan trigger.
            }
        }
    }

    /** Optional: SAF folder-based scan, kept for users who want to scope to a specific folder. */
    fun onFolderSelected(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            try {
                val folderSourceId = libraryRepository.addFolderSource(treeUri.toString(), displayName)
                withContext(Dispatchers.IO) {
                    libraryRepository.scanAndImport(treeUri, folderSourceId) { }
                }
            } catch (e: Exception) {
                // Never crash — the repository already reports FAILED status internally.
            }
        }
    }

    fun rescanAll() {
        viewModelScope.launch {
            _uiState.value.folderSources.forEach { source ->
                try {
                    withContext(Dispatchers.IO) {
                        libraryRepository.scanAndImport(Uri.parse(source.treeUri), source.id) { }
                    }
                } catch (e: Exception) {
                    // Never crash.
                }
            }
        }
    }
}
