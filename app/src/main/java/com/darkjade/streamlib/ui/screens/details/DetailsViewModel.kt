package com.darkjade.streamlib.ui.screens.details

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.data.repository.LibraryRepository
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
)

class DetailsViewModel(
    private val mediaId: Long,
    private val libraryRepository: LibraryRepository,
    private val watchRepository: WatchRepository,
    private val profileRepository: ProfileRepository,
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

            if (media.type.isSeriesLike()) {
                val seasons = libraryRepository.getSeasonsForMedia(mediaId)
                val firstSeason = seasons.firstOrNull()

                val nextEpisode = libraryRepository.getNextUnwatchedEpisode(mediaId)
                val allEpisodesForMedia = libraryRepository.getEpisodesForMedia(mediaId)
                val hasWatchedAny = allEpisodesForMedia.any { it.watched }

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
                )

                firstSeason?.let { season ->
                    viewModelScope.launch {
                        libraryRepository.observeEpisodes(season.id).collect { eps ->
                            _uiState.value = _uiState.value.copy(episodes = eps)
                        }
                    }
                }
            } else {
                _uiState.value = DetailsUiState(
                    isLoading = false,
                    media = media,
                    nextUpLabel = "Play",
                    nextUpUri = media.localFileUri?.let { Uri.parse(it) },
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
    fun removeMediaItem(onRemoved: () -> Unit) {
        viewModelScope.launch {
            libraryRepository.removeMediaItem(mediaId)
            onRemoved()
        }
    }
}
