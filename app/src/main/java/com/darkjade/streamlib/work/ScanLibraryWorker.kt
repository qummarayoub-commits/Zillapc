package com.darkjade.streamlib.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.darkjade.streamlib.data.metadata.tmdb.TmdbMetadataProvider
import com.darkjade.streamlib.data.repository.LibraryRepository

/**
 * Runs a full folder scan + import in the background so scanning thousands
 * of files never freezes the UI (Phase 19). Triggered from Settings >
 * "Scan Library" or automatically after a new folder is added.
 */
class ScanLibraryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TREE_URI = "tree_uri"
        const val KEY_FOLDER_SOURCE_ID = "folder_source_id"
        const val UNIQUE_WORK_NAME = "scan_library_work"
    }

    override suspend fun doWork(): Result {
        val treeUriString = inputData.getString(KEY_TREE_URI) ?: return Result.failure()
        val folderSourceId = inputData.getLong(KEY_FOLDER_SOURCE_ID, -1L).takeIf { it != -1L }

        val repository = LibraryRepository(applicationContext, TmdbMetadataProvider())
        return try {
            repository.scanAndImport(Uri.parse(treeUriString), folderSourceId) { /* progress observed via DB flow */ }
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
}

fun buildScanInputData(treeUri: Uri, folderSourceId: Long?): Data = workDataOf(
    ScanLibraryWorker.KEY_TREE_URI to treeUri.toString(),
    ScanLibraryWorker.KEY_FOLDER_SOURCE_ID to (folderSourceId ?: -1L)
)
