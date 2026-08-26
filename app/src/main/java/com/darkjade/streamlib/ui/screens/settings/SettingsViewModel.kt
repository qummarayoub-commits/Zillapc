package com.darkjade.streamlib.ui.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.FolderSourceEntity
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import com.darkjade.streamlib.data.metadata.comicvine.ComicVineConfig
import com.darkjade.streamlib.data.repository.ComicRepository
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledPlayerApp(val packageName: String, val label: String)

data class SettingsUiState(
    val folderSources: List<FolderSourceEntity> = emptyList(),
    val scanStatus: ScanStatusEntity? = null,
    val comicFolderSources: List<FolderSourceEntity> = emptyList(),
    val comicScanStatus: ScanStatusEntity? = null,
    val comicVineApiKey: String = "",
    val preferredPlayerPackage: String? = null,
    val installedPlayers: List<InstalledPlayerApp> = emptyList(),
    val musicScanResult: String? = null,
)

class SettingsViewModel(
    private val appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val comicRepository: ComicRepository,
    private val preferencesRepository: PreferencesRepository,
    private val musicRepository: com.darkjade.streamlib.data.repository.MusicRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                libraryRepository.observeFolderSources().collect { sources ->
                    _uiState.value = _uiState.value.copy(
                        folderSources = sources.filterNot { it.isComicSource },
                        comicFolderSources = sources.filter { it.isComicSource },
                    )
                }
            }
            launch {
                libraryRepository.observeScanStatus().collect { status ->
                    _uiState.value = _uiState.value.copy(scanStatus = status)
                }
            }
            launch {
                comicRepository.observeComicScanStatus().collect { status ->
                    _uiState.value = _uiState.value.copy(comicScanStatus = status)
                }
            }
            launch {
                preferencesRepository.observeComicVineApiKey().collect { key ->
                    _uiState.value = _uiState.value.copy(comicVineApiKey = key.orEmpty())
                    ComicVineConfig.apiKey = key.orEmpty()
                }
            }
            launch {
                preferencesRepository.observePreferredPlayerPackage().collect { pkg ->
                    _uiState.value = _uiState.value.copy(preferredPlayerPackage = pkg)
                }
            }
            _uiState.value = _uiState.value.copy(installedPlayers = queryInstalledPlayers())
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
            } catch (e: Throwable) {
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
            } catch (e: Throwable) {
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
                } catch (e: Throwable) {
                    // Never crash.
                }
            }
        }
    }

    /** Primary comics scan: device-wide via MediaStore, same reliable pattern as videos. */
    fun scanDeviceForComics() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    comicRepository.scanDevice { }
                }
            } catch (e: Throwable) {
                // Never crash — repository already reports FAILED status internally.
            }
        }
    }

    /** Comics folder — scoped SAF pick, scans only comic files (.cbz/.cbr/.cb7/.pdf). */
    fun onComicFolderSelected(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            try {
                val folderSourceId = libraryRepository.addFolderSource(treeUri.toString(), displayName, isComicSource = true)
                withContext(Dispatchers.IO) {
                    comicRepository.scanFolder(treeUri, folderSourceId) { }
                }
            } catch (e: Throwable) {
                // Never crash.
            }
        }
    }

    /** Music gets its own folder picker, same isolation as comics — never
     * touches video/comic scanning, and vice versa. */
    fun onMusicFolderSelected(treeUri: Uri, displayName: String) {
        val repo = musicRepository ?: return
        viewModelScope.launch {
            try {
                val folderSourceId = libraryRepository.addFolderSource(treeUri.toString(), displayName, isMusicSource = true)
                withContext(Dispatchers.IO) {
                    repo.scanMusicFolder(treeUri, folderSourceId)
                }
            } catch (e: Throwable) {
                // Never crash.
            }
        }
    }

    /** Primary music scan — whole device, no folder picker needed, same
     * pattern as scanDeviceForVideos(). MusicClassifier keeps it clean. */
    fun scanDeviceForMusic() {
        val repo = musicRepository ?: return
        _uiState.value = _uiState.value.copy(musicScanResult = "Scanning\u2026")
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    repo.scanDeviceForMusic()
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
            _uiState.value = _uiState.value.copy(
                musicScanResult = result.fold(
                    onSuccess = { count -> if (count > 0) "Found $count song${if (count == 1) "" else "s"}." else "No music found on this device." },
                    onFailure = { e -> "Scan failed: ${e.message ?: "unknown error"}" },
                )
            )
        }
    }

    /** Priority-3 online artwork lookup (MusicBrainz + Cover Art Archive)
     * for whatever still has no embedded/folder art after scanning. */
    fun fetchMissingArtworkOnline() {
        val repo = musicRepository ?: return
        _uiState.value = _uiState.value.copy(musicScanResult = "Fetching artwork\u2026")
        viewModelScope.launch {
            val count = try {
                withContext(Dispatchers.IO) {
                    repo.fetchMissingArtworkOnline()
                }
            } catch (e: Throwable) {
                -1
            }
            _uiState.value = _uiState.value.copy(
                musicScanResult = if (count >= 0) "Updated artwork for $count song${if (count == 1) "" else "s"}." else "Artwork fetch failed."
            )
        }
    }

    fun rescanComics() {
        viewModelScope.launch {
            _uiState.value.comicFolderSources.forEach { source ->
                try {
                    withContext(Dispatchers.IO) {
                        comicRepository.scanFolder(Uri.parse(source.treeUri), source.id) { }
                    }
                } catch (e: Throwable) {
                    // Never crash.
                }
            }
        }
    }

    fun setComicVineApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.setComicVineApiKey(key)
            ComicVineConfig.apiKey = key
        }
    }

    fun setPreferredPlayer(packageName: String?) {
        viewModelScope.launch { preferencesRepository.setPreferredPlayerPackage(packageName) }
    }

    private fun queryInstalledPlayers(): List<InstalledPlayerApp> {
        return try {
            val pm = appContext.packageManager
            // Using just ACTION_VIEW + type="video/*" (no data URI) is the
            // standard way to enumerate video-capable apps — a query with a
            // synthetic content:// URI can silently exclude real players
            // whose intent-filters don't happen to match that fake scheme.
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                type = "video/*"
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfos
                .map { InstalledPlayerApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .distinctBy { it.packageName }
                .sortedBy { it.label }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}
