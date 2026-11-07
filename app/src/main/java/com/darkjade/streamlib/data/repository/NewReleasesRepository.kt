package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.metadata.tmdb.NewReleaseItem
import com.darkjade.streamlib.data.metadata.tmdb.TmdbDiscoverProvider
import com.darkjade.streamlib.data.parser.normalizeTitleForMatching
import kotlin.math.abs

/**
 * A TMDB release paired with the matching title on THIS device, if any.
 * `localItem != null` means it's available to stream from the library.
 */
data class NewReleaseEntry(
    val release: NewReleaseItem,
    val localItem: MediaItemEntity?,
) {
    val isAvailable: Boolean get() = localItem != null
}

/**
 * Fetches what was released this year + last year (TMDB) and works out which
 * of those titles already exist in the user's own library.
 */
class NewReleasesRepository(
    context: Context,
    private val libraryRepository: LibraryRepository,
) {
    private val provider = TmdbDiscoverProvider(context)

    suspend fun fetch(forceRefresh: Boolean = false): List<NewReleaseItem> =
        provider.fetchNewReleases(forceRefresh)

    /**
     * Matching rules:
     *  1. Same TMDB id (library items that were matched to TMDB carry it) — exact and preferred.
     *  2. Otherwise same normalized title. Movies must also agree on year (±1, since
     *     release years vary by region); series don't compare year because a new
     *     SEASON of an old show has a different year than the show's first-air year.
     *  3. For series entries that are about one specific season, that season must
     *     actually exist locally — owning Season 1 doesn't mean Season 3 is playable.
     */
    suspend fun match(releases: List<NewReleaseItem>, library: List<MediaItemEntity>): List<NewReleaseEntry> {
        if (releases.isEmpty()) return emptyList()

        fun isMovie(t: MediaType) = t == MediaType.MOVIE

        val byTmdb = HashMap<String, MediaItemEntity>()
        val byTitle = HashMap<String, MutableList<MediaItemEntity>>()
        for (item in library) {
            val movie = isMovie(item.type)
            item.tmdbId?.takeIf { it.isNotBlank() }?.let { byTmdb.putIfAbsent("$movie:$it", item) }
            val titles = listOfNotNull(
                item.normalizedTitle.ifBlank { normalizeTitleForMatching(item.title) },
                item.originalTitle?.let { normalizeTitleForMatching(it) },
            ).filter { it.isNotBlank() }.distinct()
            for (t in titles) byTitle.getOrPut("$movie:$t") { mutableListOf() }.add(item)
        }

        val seasonCache = HashMap<Long, Set<Int>>()
        suspend fun localSeasons(id: Long): Set<Int> =
            seasonCache.getOrPut(id) { libraryRepository.getSeasonsForMedia(id).map { it.seasonNumber }.toSet() }

        return releases.map { release ->
            val movie = isMovie(release.type)
            var local: MediaItemEntity? = byTmdb["$movie:${release.tmdbId}"]
            if (local == null) {
                val candidates = byTitle["$movie:${normalizeTitleForMatching(release.title)}"].orEmpty()
                local = if (movie) {
                    candidates.firstOrNull { c ->
                        c.year == null || release.year == null || abs(c.year - release.year) <= 1
                    }
                } else {
                    candidates.firstOrNull()
                }
            }

            // Series: the specific season has to be on the device too.
            if (local != null && !movie && release.seasonNumber != null) {
                val seasons = localSeasons(local.id)
                if (seasons.isNotEmpty() && release.seasonNumber !in seasons) local = null
            }

            NewReleaseEntry(release, local)
        }
    }
}
