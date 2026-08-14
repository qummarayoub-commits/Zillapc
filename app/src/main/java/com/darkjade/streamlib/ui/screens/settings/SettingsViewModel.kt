package com.darkjade.streamlib.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.darkjade.streamlib.data.db.entity.FolderSourceEntity
import com.darkjade.streamlib.data.db.entity.ScanState
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.work.ScanLibraryWorker
import com.darkjade.streamlib.work.buildScanInputData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    /** Called after the SAF folder picker returns a tree URI. */
    fun onFolderSelected(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            libraryRepository.addFolderSource(treeUri.toString(), displayName)
            triggerScan(treeUri, folderSourceId = null)
        }
    }

    fun rescanAll() {
        viewModelScope.launch {
            _uiState.value.folderSources.forEach { source ->
                triggerScan(Uri.parse(source.treeUri), source.id)
            }
        }
    }

    private fun triggerScan(treeUri: Uri, folderSourceId: Long?) {
        val request = OneTimeWorkRequestBuilder<ScanLibraryWorker>()
            .setInputData(buildScanInputData(treeUri, folderSourceId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ScanLibraryWorker.UNIQUE_WORK_NAME + (folderSourceId ?: treeUri.toString()),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
