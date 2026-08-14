package com.darkjade.streamlib.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import com.darkjade.streamlib.data.metadata.MetadataProvider
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.data.scanner.LibraryScanner
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

    private val mediaDao = db.mediaItemDao()
    private val seasonDao = db.seasonDao()
    private val episodeDao = db.episodeDao()
    private val scanStatusDao = db.scanStatusDao()
    private val folderSourceDao = db.folderSourceDao()

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

    suspend fun addFolderSource(treeUri: String, displayName: String) {
        val existing = folderSourceDao.findByUri(treeUri)
        if (existing == null) {
            folderSourceDao.insert(
                com.darkjade.streamlib.data.db.entity.FolderSourceEntity(
                    treeUri = treeUri,
                    displayName = displayName,
                )
            )
        }
    }

    /**
     * Scans a folder tree, writing discovered media into Room incrementally
     * (never loads the full library into memory), then attempts metadata
     * enrichment per-item. Emits progress via ScanEvent for the Settings UI.
     */
    suspend fun scanAndImport(
        treeUri: android.net.Uri,
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

        scanner.scanTree(treeUri).collect { event ->
            when (event) {
                is ScanEvent.FileFound -> {
                    found++
                    importScannedFile(event.file, folderSourceId)
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

    private suspend fun importScannedFile(
        file: com.darkjade.streamlib.data.scanner.ScannedFile,
        folderSourceId: Long?,
    ) {
        val parsed = file.parsed
        val type = com.darkjade.streamlib.data.parser.MediaFilenameParser
            .folderTypeHint(file.pathSegments) ?: parsed.type

        if (type == MediaType.MOVIE) {
            val existing = mediaDao.findByTitleTypeYear(parsed.title, MediaType.MOVIE, parsed.year)
            if (existing == null) {
                val entity = MediaItemEntity(
                    title = parsed.title,
                    sortTitle = sortableTitle(parsed.title),
                    type = MediaType.MOVIE,
                    year = parsed.year,
                    localFileUri = file.uri.toString(),
                    localFilePath = file.pathSegments.joinToString("/"),
                    metadataFetched = false,
                    folderSourceId = folderSourceId,
                )
                val id = mediaDao.insert(entity)
                enrichMovieMetadata(id, entity)
            }
        } else {
            // Series or Anime: find-or-create the parent MediaItem, then the season, then episode.
            var mediaItem = mediaDao.findByTitleTypeYear(parsed.title, type, null)
            val mediaItemId: Long
            if (mediaItem == null) {
                val entity = MediaItemEntity(
                    title = parsed.title,
                    sortTitle = sortableTitle(parsed.title),
                    type = type,
                    year = parsed.year,
                    metadataFetched = false,
                    folderSourceId = folderSourceId,
                )
                mediaItemId = mediaDao.insert(entity)
                mediaItem = entity.copy(id = mediaItemId)
                enrichSeriesMetadata(mediaItemId, mediaItem)
            } else {
                mediaItemId = mediaItem.id
            }

            val seasonNumber = parsed.season ?: 1
            var season = seasonDao.find(mediaItemId, seasonNumber)
            val seasonId = if (season == null) {
                val newSeasonId = seasonDao.insert(SeasonEntity(mediaItemId = mediaItemId, seasonNumber = seasonNumber))
                newSeasonId
            } else {
                season.id
            }

            val existingEpisode = episodeDao.findByUri(file.uri.toString())
            if (existingEpisode == null) {
                episodeDao.insert(
                    EpisodeEntity(
                        mediaItemId = mediaItemId,
                        seasonId = seasonId,
                        episodeNumber = parsed.episode ?: 0,
                        localFileUri = file.uri.toString(),
                        localFilePath = file.pathSegments.joinToString("/"),
                        fileSizeBytes = file.sizeBytes,
                        quality = parsed.quality,
                    )
                )
            }
        }
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
                    runtimeMinutes = result.runtimeMinutes,
                    genres = result.genres.joinToString(","),
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
