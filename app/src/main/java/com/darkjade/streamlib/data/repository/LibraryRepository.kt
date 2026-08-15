package com.darkjade.streamlib.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import com.darkjade.streamlib.data.metadata.MetadataProvider
import com.darkjade.streamlib.data.metadata.SeasonMetadata
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.data.parser.normalizeTitleForMatching
import com.darkjade.streamlib.data.scanner.LibraryScanner
import com.darkjade.streamlib.data.scanner.MediaStoreScanner
import com.darkjade.streamlib.data.scanner.ScanEvent
import kotlinx.coroutines.flow.Flow

/**
 * Central repository for the local library. Ties together the storage
 * scanner, filename parser, Room database, and metadata provider.
 *
 * Design note: metadata lookups are best-effort and never block library
 * writes — a file is stored the moment it's discovered (fallback card
 * state), then upgraded in-place if/when metadata arrives.
 */
class LibraryRepository(
    context: Context,
    private val metadataProvider: MetadataProvider,
) {
    private val db = StreamLibDatabase.getInstance(context)
    private val scanner = LibraryScanner(context)
    private val mediaStoreScanner = MediaStoreScanner(context)

    private val mediaDao = db.mediaItemDao()
    private val seasonDao = db.seasonDao()
    private val episodeDao = db.episodeDao()
    private val scanStatusDao = db.scanStatusDao()
    private val folderSourceDao = db.folderSourceDao()

    // Per-season episode metadata, fetched once per season per app run and
    // reused across every episode file discovered for that season — avoids
    // hammering TMDB with one request per episode file.
    private val seasonMetadataCache = mutableMapOf<Long, SeasonMetadata?>()

    fun observeAll(): Flow<List<MediaItemEntity>> = mediaDao.observeAll()
    fun observeRecentlyAdded(limit: Int = 20) = mediaDao.observeRecentlyAdded(limit)
    fun observeByType(type: MediaType) = mediaDao.observeByType(type)
    fun observeCount() = mediaDao.observeCount()
    fun pagingByType(type: MediaType): PagingSource<Int, MediaItemEntity> = mediaDao.pagingByType(type)
    fun pagingAll(): PagingSource<Int, MediaItemEntity> = mediaDao.pagingAll()
    fun observeScanStatus() = scanStatusDao.observe()
    fun observeFolderSources() = folderSourceDao.observeAll()

    suspend fun getMediaItem(id: Long) = mediaDao.getById(id)
    fun observeMediaItem(id: Long) = mediaDao.observeById(id)
    suspend fun getSeasonsForMedia(mediaItemId: Long) = seasonDao.getForMedia(mediaItemId)
    fun observeSeasons(mediaItemId: Long) = seasonDao.observeForMedia(mediaItemId)
    fun observeEpisodes(seasonId: Long) = episodeDao.observeForSeason(seasonId)
    suspend fun getEpisodesForMedia(mediaItemId: Long) = episodeDao.getForMedia(mediaItemId)
    suspend fun getNextUnwatchedEpisode(mediaItemId: Long) = episodeDao.getNextUnwatched(mediaItemId)
    suspend fun getEpisode(id: Long) = episodeDao.getById(id)

    suspend fun search(query: String): SearchResults {
        if (query.isBlank()) return SearchResults(emptyList(), emptyList())
        val media = mediaDao.search(query)
        val episodes = episodeDao.search(query)
        return SearchResults(media, episodes)
    }

    suspend fun setEpisodeWatched(episodeId: Long, watched: Boolean) =
        episodeDao.setWatched(episodeId, watched)

    /** "Remove from here" — removes a single episode entry from the library (not the actual file). */
    suspend fun removeEpisode(episodeId: Long) = episodeDao.deleteById(episodeId)

    /** "Remove from here" — removes an entire movie/show (and its seasons/episodes) from the library. */
    suspend fun removeMediaItem(mediaItemId: Long) = mediaDao.deleteById(mediaItemId)

    suspend fun addFolderSource(treeUri: String, displayName: String): Long {
        val existing = folderSourceDao.findByUri(treeUri)
        if (existing != null) return existing.id
        return folderSourceDao.insert(
            com.darkjade.streamlib.data.db.entity.FolderSourceEntity(
                treeUri = treeUri,
                displayName = displayName,
            )
        )
    }

    /**
     * Scans a folder tree, writing discovered media into Room incrementally
     * (never loads the full library into memory), then attempts metadata
     * enrichment per-item. Emits progress via ScanEvent for the Settings UI.
     *
     * Wrapped defensively: any exception (e.g. a SAF permission grant that
     * silently failed on a quirky OEM DocumentsProvider) is reported as a
     * FAILED scan status instead of crashing the app.
     */
    suspend fun scanAndImport(
        treeUri: android.net.Uri,
        folderSourceId: Long?,
        onEvent: suspend (ScanEvent) -> Unit,
    ) {
        try {
            runScanFlow(scanner.scanTree(treeUri), folderSourceId, onEvent)
        } catch (e: Exception) {
            reportScanFailure(e.message ?: "Unknown error while scanning folder", onEvent)
        }
    }

    /**
     * Scans the device-wide MediaStore video index instead of a SAF folder
     * tree. This is the recommended default: it sidesteps OEM-specific SAF
     * DocumentsProvider bugs entirely (see MediaStoreScanner) and only needs
     * the standard READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE permission.
     */
    suspend fun scanDeviceMediaStore(onEvent: suspend (ScanEvent) -> Unit) {
        try {
            runScanFlow(mediaStoreScanner.scanDevice(), folderSourceId = null, onEvent)
        } catch (e: Exception) {
            reportScanFailure(e.message ?: "Unknown error while scanning device media", onEvent)
        }
    }

    private suspend fun runScanFlow(
        events: kotlinx.coroutines.flow.Flow<ScanEvent>,
        folderSourceId: Long?,
        onEvent: suspend (ScanEvent) -> Unit,
    ) {
        scanStatusDao.upsert(
            com.darkjade.streamlib.data.db.entity.ScanStatusEntity(
                state = com.darkjade.streamlib.data.db.entity.ScanState.SCANNING,
                startedAt = System.currentTimeMillis(),
            )
        )

        var found = 0
        var processed = 0

        events.collect { event ->
            when (event) {
                is ScanEvent.FileFound -> {
                    found++
                    try {
                        importScannedFile(event.file, folderSourceId)
                    } catch (e: Exception) {
                        // One bad file must never abort the whole scan.
                    }
                }
                is ScanEvent.Progress -> {
                    processed = event.processed
                    scanStatusDao.upsert(
                        com.darkjade.streamlib.data.db.entity.ScanStatusEntity(
                            state = com.darkjade.streamlib.data.db.entity.ScanState.SCANNING,
                            filesFound = found,
                            filesProcessed = processed,
                        )
                    )
                }
                is ScanEvent.Completed -> {
                    scanStatusDao.upsert(
                        com.darkjade.streamlib.data.db.entity.ScanStatusEntity(
                            state = com.darkjade.streamlib.data.db.entity.ScanState.COMPLETED,
                            filesFound = event.totalFound,
                            filesProcessed = event.totalFound,
                            finishedAt = System.currentTimeMillis(),
                        )
                    )
                    if (folderSourceId != null) {
                        folderSourceDao.getAll().find { it.id == folderSourceId }?.let { source ->
                            folderSourceDao.update(
                                source.copy(itemCount = event.totalFound, lastScannedAt = System.currentTimeMillis())
                            )
                        }
                    }
                }
                is ScanEvent.Failed -> {
                    scanStatusDao.upsert(
                        com.darkjade.streamlib.data.db.entity.ScanStatusEntity(
                            state = com.darkjade.streamlib.data.db.entity.ScanState.FAILED,
                            errorMessage = event.message,
                            finishedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
            onEvent(event)
        }
    }

    private suspend fun reportScanFailure(message: String, onEvent: suspend (ScanEvent) -> Unit) {
        scanStatusDao.upsert(
            com.darkjade.streamlib.data.db.entity.ScanStatusEntity(
                state = com.darkjade.streamlib.data.db.entity.ScanState.FAILED,
                errorMessage = message,
                finishedAt = System.currentTimeMillis(),
            )
        )
        onEvent(ScanEvent.Failed(message))
    }

    private suspend fun importScannedFile(
        file: com.darkjade.streamlib.data.scanner.ScannedFile,
        folderSourceId: Long?,
    ) {
        val parsed = file.parsed
        val type = com.darkjade.streamlib.data.parser.MediaFilenameParser
            .folderTypeHint(file.pathSegments) ?: parsed.type
        val normalizedTitle = normalizeTitleForMatching(parsed.title)

        if (type == MediaType.MOVIE) {
            val existing = mediaDao.findByTitleTypeYear(normalizedTitle, MediaType.MOVIE, parsed.year)
            if (existing == null) {
                val entity = MediaItemEntity(
                    title = parsed.title,
                    sortTitle = sortableTitle(parsed.title),
                    normalizedTitle = normalizedTitle,
                    type = MediaType.MOVIE,
                    year = parsed.year,
                    localFileUri = file.uri.toString(),
                    localFilePath = file.pathSegments.joinToString("/"),
                    runtimeMinutes = file.durationMs.takeIf { it > 0 }?.let { (it / 60000).toInt() },
                    metadataFetched = false,
                    folderSourceId = folderSourceId,
                )
                val id = mediaDao.insert(entity)
                enrichMovieMetadata(id, entity)
            }
        } else {
            // Series or Anime: find-or-create the parent MediaItem, then the season, then episode.
            // Matching is done on a NORMALIZED title (lowercased, punctuation-stripped) so that
            // small filename differences ("Spider Man" vs "Spider-Man") group under one show
            // instead of each variant creating a duplicate entry.
            var mediaItem = mediaDao.findByNormalizedTitleAndType(normalizedTitle, type)
            val mediaItemId: Long
            if (mediaItem == null) {
                val entity = MediaItemEntity(
                    title = parsed.title,
                    sortTitle = sortableTitle(parsed.title),
                    normalizedTitle = normalizedTitle,
                    type = type,
                    year = parsed.year,
                    metadataFetched = false,
                    folderSourceId = folderSourceId,
                )
                mediaItemId = mediaDao.insert(entity)
                enrichSeriesMetadata(mediaItemId, entity)
                // Re-fetch: enrichment may have set tmdbId, needed below for episode metadata.
                mediaItem = mediaDao.getById(mediaItemId) ?: entity.copy(id = mediaItemId)
            } else {
                mediaItemId = mediaItem.id
            }

            val seasonNumber = parsed.season ?: 1
            val existingSeason = seasonDao.find(mediaItemId, seasonNumber)
            val seasonId = existingSeason?.id
                ?: seasonDao.insert(SeasonEntity(mediaItemId = mediaItemId, seasonNumber = seasonNumber))

            val existingEpisode = episodeDao.findByUri(file.uri.toString())
            if (existingEpisode == null) {
                val episodeMeta = fetchSeasonMetadataCached(seasonId, mediaItem, seasonNumber)
                    ?.episodes?.find { it.episodeNumber == (parsed.episode ?: -1) }

                episodeDao.insert(
                    EpisodeEntity(
                        mediaItemId = mediaItemId,
                        seasonId = seasonId,
                        episodeNumber = parsed.episode ?: 0,
                        title = episodeMeta?.title,
                        overview = episodeMeta?.overview,
                        thumbnailUrl = episodeMeta?.thumbnailUrl,
                        durationMinutes = episodeMeta?.runtimeMinutes
                            ?: file.durationMs.takeIf { it > 0 }?.let { (it / 60000).toInt() },
                        localFileUri = file.uri.toString(),
                        localFilePath = file.pathSegments.joinToString("/"),
                        fileSizeBytes = file.sizeBytes,
                        quality = parsed.quality,
                    )
                )
            }
        }
    }

    private suspend fun fetchSeasonMetadataCached(
        seasonId: Long,
        mediaItem: MediaItemEntity,
        seasonNumber: Int,
    ): SeasonMetadata? {
        if (seasonMetadataCache.containsKey(seasonId)) return seasonMetadataCache[seasonId]
        val tmdbId = mediaItem.tmdbId
        val result = if (tmdbId != null) {
            runCatching { metadataProvider.getSeasonDetails(tmdbId, seasonNumber) }.getOrNull()
        } else {
            null
        }
        seasonMetadataCache[seasonId] = result
        return result
    }

    private suspend fun enrichMovieMetadata(id: Long, entity: MediaItemEntity) {
        val result = runCatching { metadataProvider.searchMovie(entity.title, entity.year) }.getOrNull()
        if (result != null) {
            mediaDao.update(
                entity.copy(
                    id = id,
                    title = result.title,
                    originalTitle = result.originalTitle,
                    overview = result.overview,
                    posterUrl = result.posterUrl,
                    backdropUrl = result.backdropUrl,
                    rating = result.rating,
                    runtimeMinutes = result.runtimeMinutes ?: entity.runtimeMinutes,
                    genres = result.genres.joinToString(","),
                    director = result.director,
                    cast = result.cast.joinToString(","),
                    tmdbId = result.remoteId,
                    metadataFetched = true,
                    metadataMissing = false,
                )
            )
        } else {
            // Never crash — mark as fallback-card state; filename/local info is still shown.
            mediaDao.update(entity.copy(id = id, metadataFetched = true, metadataMissing = true))
        }
    }

    private suspend fun enrichSeriesMetadata(id: Long, entity: MediaItemEntity) {
        if (!entity.type.isSeriesLike()) return
        val result = runCatching { metadataProvider.searchSeries(entity.title, entity.year) }.getOrNull()
        if (result != null) {
            mediaDao.update(
                entity.copy(
                    id = id,
                    title = result.title,
                    originalTitle = result.originalTitle,
                    overview = result.overview,
                    posterUrl = result.posterUrl,
                    backdropUrl = result.backdropUrl,
                    rating = result.rating,
                    genres = result.genres.joinToString(","),
                    director = result.director,
                    cast = result.cast.joinToString(","),
                    tmdbId = result.remoteId,
                    metadataFetched = true,
                    metadataMissing = false,
                )
            )
        } else {
            mediaDao.update(entity.copy(id = id, metadataFetched = true, metadataMissing = true))
        }
    }

    private fun sortableTitle(title: String): String {
        val articles = listOf("the ", "a ", "an ")
        val lower = title.lowercase()
        for (article in articles) {
            if (lower.startsWith(article)) return title.substring(article.length)
        }
        return title
    }
}

data class SearchResults(
    val mediaItems: List<MediaItemEntity>,
    val episodes: List<EpisodeEntity>,
)
