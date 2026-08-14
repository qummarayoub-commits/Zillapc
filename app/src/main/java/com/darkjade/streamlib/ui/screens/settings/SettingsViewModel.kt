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
     * Called after the SAF folder picker returns a tree URI.
     *
     * Scanning runs directly on a background coroutine dispatcher tied to this
     * ViewModel — NOT via WorkManager. Some OEM Android skins (this app has
     * previously hit this with ZTE/MyOS) aggressively defer or kill scheduled
     * background work, which silently prevented scans from ever running. A
     * direct coroutine scan while the app is in the foreground sidesteps that
     * entirely and gives immediate, visible progress.
     */
    fun onFolderSelected(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            val folderSourceId = libraryRepository.addFolderSource(treeUri.toString(), displayName)
            runScan(treeUri, folderSourceId)
        }
    }

    fun rescanAll() {
        viewModelScope.launch {
            _uiState.value.folderSources.forEach { source ->
                runScan(Uri.parse(source.treeUri), source.id)
            }
        }
    }

    private suspend fun runScan(treeUri: Uri, folderSourceId: Long?) {
        withContext(Dispatchers.IO) {
            libraryRepository.scanAndImport(treeUri, folderSourceId) { /* progress observed via DB flow */ }
        }
    }
}
