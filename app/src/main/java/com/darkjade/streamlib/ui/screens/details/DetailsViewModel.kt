package com.darkjade.streamlib.ui.screens.details

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.PlaybackProgressEntity
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.data.repository.LibraryRepository
import com.darkjade.streamlib.data.repository.PlaybackRepository
import com.darkjade.streamlib.data.repository.ProfileRepository
import com.darkjade.streamlib.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val media: MediaItemEntity? = null,
    val seasons: List<SeasonEntity> = emptyList(),
    val selectedSeasonId: Long? = null,
    val episodes: List<EpisodeEntity> = emptyList(),
    val isInWatchlist: Boolean = false,
    val nextUpLabel: String? = null, // "Continue E899" / "Start Watching E1"
    val nextUpUri: Uri? = null,
    val nextUpEpisodeId: Long? = null,
    // Real playback progress (from the internal player) — separate from the
    // "watched" checkmark, this is what actually powers Play from
    // Beginning / Resume and the per-episode watched-time display.
    val hasResumeProgress: Boolean = false,
    val resumePositionMs: Long = 0,
    val episodeProgress: Map<Long, PlaybackProgressEntity> = emptyMap(),
)

class DetailsViewModel(
    private val mediaId: Long,
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var profileId: Long = -1

    init {
        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()
            profileId = profile.id

            val media = libraryRepository.getMediaItem(mediaId)
            if (media == null) {
                _uiState.value = DetailsUiState(isLoading = false)
                return@launch
            }
            _searchIsSeries.value = media.type.isSeriesLike()

            // Fetch genuine IMDb/Rotten Tomatoes ratings in the background —
            // cached after the first successful/attempted fetch (omdbFetched),
            // so reopening this screen never re-hits the OMDb API.
            viewModelScope.launch {
                libraryRepository.fetchOmdbRatingsIfNeeded(mediaId)
                val refreshed = libraryRepository.getMediaItem(mediaId)
                if (refreshed != null) {
                    _uiState.value = _uiState.value.copy(media = refreshed)
                }
            }

            // Real per-episode progress for the whole series, used for the
            // "Xm watched" display under each episode.
            val allProgress = playbackRepository.getAllForMedia(mediaId)
                .filter { it.episodeId != null }
                .associateBy { it.episodeId!! }

            if (media.type.isSeriesLike()) {
                val seasons = libraryRepository.getSeasonsForMedia(mediaId)
                val firstSeason = seasons.firstOrNull()

                val nextEpisode = libraryRepository.getNextUnwatchedEpisode(mediaId)
                val allEpisodesForMedia = libraryRepository.getEpisodesForMedia(mediaId)
                val hasWatchedAny = allEpisodesForMedia.any { it.watched }

                val resumeProgress = nextEpisode?.let { playbackRepository.getProgress(mediaId, it.id) }
                val hasResume = resumeProgress != null && resumeProgress.positionMs > 0 && !resumeProgress.completed

                _uiState.value = DetailsUiState(
                    isLoading = false,
                    media = media,
                    seasons = seasons,
                    selectedSeasonId = firstSeason?.id,
                    nextUpLabel = nextEpisode?.let {
                        if (hasWatchedAny) "Continue E${it.episodeNumber}" else "Start Watching E${it.episodeNumber}"
                    },
                    nextUpUri = nextEpisode?.localFileUri?.let { Uri.parse(it) },
                    nextUpEpisodeId = nextEpisode?.id,
                    hasResumeProgress = hasResume,
                    resumePositionMs = resumeProgress?.positionMs ?: 0,
                    episodeProgress = allProgress,
                )

                firstSeason?.let { season ->
                    viewModelScope.launch {
                        libraryRepository.observeEpisodes(season.id).collect { eps ->
                            _uiState.value = _uiState.value.copy(episodes = eps)
                        }
                    }
                }
            } else {
                val resumeProgress = playbackRepository.getProgress(mediaId, null)
                val hasResume = resumeProgress != null && resumeProgress.positionMs > 0 && !resumeProgress.completed

                _uiState.value = DetailsUiState(
                    isLoading = false,
                    media = media,
                    nextUpLabel = "Play",
                    nextUpUri = media.localFileUri?.let { Uri.parse(it) },
                    hasResumeProgress = hasResume,
                    resumePositionMs = resumeProgress?.positionMs ?: 0,
                )
            }
        }

        viewModelScope.launch {
            val profile = profileRepository.ensureDefaultProfile()
            watchRepository.observeIsInWatchlist(profile.id, mediaId).collect { inList ->
                _uiState.value = _uiState.value.copy(isInWatchlist = inList)
            }
        }
    }

    fun selectSeason(seasonId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSeasonId = seasonId)
            libraryRepository.observeEpisodes(seasonId).collect { eps ->
                _uiState.value = _uiState.value.copy(episodes = eps)
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_uiState.value.isInWatchlist) {
                watchRepository.removeFromWatchlist(profileId, mediaId)
            } else {
                watchRepository.addToWatchlist(profileId, mediaId)
            }
        }
    }

    fun recordOpened(episodeId: Long?) {
        viewModelScope.launch {
            watchRepository.recordOpened(profileId, mediaId, episodeId)
        }
    }

    /** "Play from Beginning" — discards any saved position first so the player starts at 0. */
    fun playFromBeginning(episodeId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            playbackRepository.clearProgress(mediaId, episodeId)
            onReady()
        }
    }

    fun toggleEpisodeWatched(episode: EpisodeEntity) {
        viewModelScope.launch {
            libraryRepository.setEpisodeWatched(episode.id, !episode.watched)
            // Patch the currently displayed list in place for instant UI feedback.
            _uiState.value = _uiState.value.copy(
                episodes = _uiState.value.episodes.map {
                    if (it.id == episode.id) it.copy(watched = !episode.watched) else it
                }
            )
        }
    }

    fun removeEpisode(episodeId: Long) {
        viewModelScope.launch {
            libraryRepository.removeEpisode(episodeId)
            _uiState.value = _uiState.value.copy(
                episodes = _uiState.value.episodes.filterNot { it.id == episodeId }
            )
        }
    }

    /** Removes the whole movie/show from the library. Caller should navigate back after this. */
    fun rateMedia(stars: Int) {
        viewModelScope.launch {
            libraryRepository.setUserRating(mediaId, stars)
            val refreshed = libraryRepository.getMediaItem(mediaId)
            if (refreshed != null) {
                _uiState.value = _uiState.value.copy(media = refreshed)
            }
        }
    }

    fun markAsWatched() {
        viewModelScope.launch {
            val media = _uiState.value.media ?: return@launch
            val durationMsFallback = (media.runtimeMinutes ?: 0).toLong() * 60000L
            playbackRepository.markAsWatched(mediaId, null, durationMsFallback)
            _uiState.value = _uiState.value.copy(hasResumeProgress = true, resumePositionMs = durationMsFallback)
        }
    }

    fun removeMediaItem(onRemoved: () -> Unit) {
        viewModelScope.launch {
            libraryRepository.removeMediaItem(mediaId)
            onRemoved()
        }
    }

    /** "Change Poster" / "Change Backdrop" — pick a specific image from the
     * TMDB gallery already fetched with this title (alternatePosterUrls /
     * alternateBackdropUrls) instead of the one TMDB auto-assigned. */
    fun setPoster(url: String) {
        viewModelScope.launch {
            libraryRepository.setPosterUrl(mediaId, url)
            val refreshed = libraryRepository.getMediaItem(mediaId)
            if (refreshed != null) _uiState.value = _uiState.value.copy(media = refreshed)
        }
    }

    fun setBackdrop(url: String) {
        viewModelScope.launch {
            libraryRepository.setBackdropUrl(mediaId, url)
            val refreshed = libraryRepository.getMediaItem(mediaId)
            if (refreshed != null) _uiState.value = _uiState.value.copy(media = refreshed)
        }
    }

    private val _searchResults = kotlinx.coroutines.flow.MutableStateFlow<List<com.darkjade.streamlib.data.metadata.SearchCandidate>>(emptyList())
    val searchResults: kotlinx.coroutines.flow.StateFlow<List<com.darkjade.streamlib.data.metadata.SearchCandidate>> = _searchResults
    private val _searchInProgress = kotlinx.coroutines.flow.MutableStateFlow(false)
    val searchInProgress: kotlinx.coroutines.flow.StateFlow<Boolean> = _searchInProgress

    /** Which TMDB catalog "Add Info" searches - Movie or Series. Defaults to
     * whatever this library item's own type already is, but the user can
     * flip it explicitly in the dialog. This matters because a title can be
     * mis-typed in the library (a series scanned/added as a movie or vice
     * versa) - without an override, search was permanently stuck searching
     * the wrong TMDB catalog with no way to ever find the right match. */
    private val _searchIsSeries = kotlinx.coroutines.flow.MutableStateFlow(false)
    val searchIsSeries: kotlinx.coroutines.flow.StateFlow<Boolean> = _searchIsSeries

    fun setSearchIsSeries(isSeries: Boolean) {
        if (_searchIsSeries.value == isSeries) return
        _searchIsSeries.value = isSeries
        // Clear stale results from the other catalog so they're never shown
        // mislabeled as if they came from the newly-selected type.
        _searchResults.value = emptyList()
    }

    /** "Add Info" — user searches TMDB manually to fix a wrong/missing match. */
    fun searchTmdb(query: String) {
        viewModelScope.launch {
            _searchInProgress.value = true
            _searchResults.value = libraryRepository.searchMetadataCandidates(query, _searchIsSeries.value)
            _searchInProgress.value = false
        }
    }

    fun applyManualMatch(remoteId: String, onApplied: () -> Unit) {
        viewModelScope.launch {
            val isSeries = _searchIsSeries.value
            libraryRepository.applyManualMatch(mediaId, remoteId, isSeries)
            val refreshed = libraryRepository.getMediaItem(mediaId)
            if (refreshed != null) {
                _uiState.value = _uiState.value.copy(media = refreshed)
            }
            // Re-check OMDb for the newly-corrected title.
            libraryRepository.fetchOmdbRatingsIfNeeded(mediaId)
            val withRatings = libraryRepository.getMediaItem(mediaId)
            if (withRatings != null) {
                _uiState.value = _uiState.value.copy(media = withRatings)
            }
            _searchResults.value = emptyList()
            onApplied()
        }
    }
}
