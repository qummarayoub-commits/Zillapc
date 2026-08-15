package com.darkjade.streamlib.data.repository

import android.content.Context
import android.net.Uri
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.ScanState
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import com.darkjade.streamlib.data.metadata.comicvine.ComicVineMetadataProvider
import com.darkjade.streamlib.data.scanner.ComicFileSystemScanner
import com.darkjade.streamlib.data.scanner.ComicScanEvent
import com.darkjade.streamlib.data.scanner.ComicScanner
import com.darkjade.streamlib.data.scanner.ScannedComic
import kotlinx.coroutines.flow.Flow

/** Comic scan status uses row id = 1 in the shared scan_status table — see ScanStatusDao. */
private const val COMIC_SCAN_STATUS_ID = 1

class ComicRepository(
    context: Context,
    private val metadataProvider: ComicVineMetadataProvider,
) {
    private val db = StreamLibDatabase.getInstance(context)
    private val folderScanner = ComicScanner(context)
    private val deviceScanner = ComicFileSystemScanner(context)
    private val comicDao = db.comicDao()
    private val scanStatusDao = db.scanStatusDao()

    fun observeAll() = comicDao.observeAll()
    fun observeRecentlyAdded(limit: Int = 20) = comicDao.observeRecentlyAdded(limit)
    fun observeCount() = comicDao.observeCount()
    fun observeComicScanStatus() = scanStatusDao.observeComicScan()
    suspend fun getById(id: Long) = comicDao.getById(id)
    fun observeById(id: Long) = comicDao.observeById(id)
    suspend fun search(query: String) = if (query.isBlank()) emptyList() else comicDao.search(query)
    suspend fun removeComic(id: Long) = comicDao.deleteById(id)

    /**
     * Primary scan path: indexes the device's MediaStore Files for comic
     * files — same reliable, permission-simple approach as the video
     * scanner. No folder picking, no SAF crashes.
     */
    suspend fun scanDevice(onEvent: suspend (ComicScanEvent) -> Unit) {
        runScan(deviceScanner.scanDevice(), folderSourceId = null, onEvent)
    }

    /** Optional: SAF folder-based scan for users who want to scope to one specific folder. */
    suspend fun scanFolder(treeUri: Uri, folderSourceId: Long?, onEvent: suspend (ComicScanEvent) -> Unit) {
        runScan(folderScanner.scanTree(treeUri), folderSourceId, onEvent)
    }

    private suspend fun runScan(
        events: Flow<ComicScanEvent>,
        folderSourceId: Long?,
        onEvent: suspend (ComicScanEvent) -> Unit,
    ) {
        try {
            scanStatusDao.upsert(
                ScanStatusEntity(id = COMIC_SCAN_STATUS_ID, state = ScanState.SCANNING, startedAt = System.currentTimeMillis())
            )

            var found = 0
            var processed = 0

            events.collect { event ->
                when (event) {
                    is ComicScanEvent.FileFound -> {
                        found++
                        try {
                            importScannedComic(event.file, folderSourceId)
                        } catch (e: Throwable) {
                            // One bad comic file must never abort the whole scan.
                        }
                    }
                    is ComicScanEvent.Progress -> {
                        processed = event.processed
                        scanStatusDao.upsert(
                            ScanStatusEntity(
                                id = COMIC_SCAN_STATUS_ID,
                                state = ScanState.SCANNING,
                                filesFound = found,
                                filesProcessed = processed,
                            )
                        )
                    }
                    is ComicScanEvent.Completed -> {
                        scanStatusDao.upsert(
                            ScanStatusEntity(
                                id = COMIC_SCAN_STATUS_ID,
                                state = ScanState.COMPLETED,
                                filesFound = event.totalFound,
                                filesProcessed = event.totalFound,
                                finishedAt = System.currentTimeMillis(),
                            )
                        )
                    }
                    is ComicScanEvent.Failed -> {
                        scanStatusDao.upsert(
                            ScanStatusEntity(
                                id = COMIC_SCAN_STATUS_ID,
                                state = ScanState.FAILED,
                                errorMessage = event.message,
                                finishedAt = System.currentTimeMillis(),
                            )
                        )
                    }
                }
                onEvent(event)
            }
        } catch (e: Throwable) {
            scanStatusDao.upsert(
                ScanStatusEntity(
                    id = COMIC_SCAN_STATUS_ID,
                    state = ScanState.FAILED,
                    errorMessage = e.message ?: "Unknown error while scanning comics",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            onEvent(ComicScanEvent.Failed(e.message ?: "Unknown error"))
        }
    }

    private suspend fun importScannedComic(file: ScannedComic, folderSourceId: Long?) {
        val existing = comicDao.findByUri(file.uri.toString())
        if (existing != null) return

        val parsed = file.parsed
        val entity = ComicEntity(
            title = parsed.issueNumber?.let { "${parsed.seriesGuess} #$it" } ?: parsed.seriesGuess,
            seriesName = parsed.seriesGuess,
            issueNumber = parsed.issueNumber,
            sortTitle = parsed.seriesGuess,
            localFileUri = file.uri.toString(),
            localFilePath = file.pathSegments.joinToString("/"),
            fileSizeBytes = file.sizeBytes,
            folderSourceId = folderSourceId,
        )
        val id = comicDao.insert(entity)
        enrichComicMetadata(id, entity)
    }

    private suspend fun enrichComicMetadata(id: Long, entity: ComicEntity) {
        val result = runCatching {
            metadataProvider.searchIssue(entity.seriesName, entity.issueNumber)
        }.getOrNull()

        if (result != null) {
            comicDao.update(
                entity.copy(
                    id = id,
                    title = result.title,
                    seriesName = result.seriesName,
                    issueNumber = result.issueNumber ?: entity.issueNumber,
                    publisher = result.publisher,
                    overview = result.overview,
                    coverUrl = result.coverUrl,
                    releaseDate = result.releaseDate,
                    metadataFetched = true,
                    metadataMissing = false,
                )
            )
        } else {
            comicDao.update(entity.copy(id = id, metadataFetched = true, metadataMissing = true))
        }
    }
}
