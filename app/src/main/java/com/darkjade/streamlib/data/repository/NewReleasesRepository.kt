package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.metadata.tmdb.NewReleaseItem
import com.darkjade.streamlib.data.metadata.tmdb.NewReleasesBundle
import com.darkjade.streamlib.data.metadata.tmdb.SeasonStamp
import com.darkjade.streamlib.data.metadata.tmdb.TmdbDiscoverProvider
import com.darkjade.streamlib.data.parser.normalizeTitleForMatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
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

/** One themed row ("New on Netflix", "New Family Fun", ...) with its availability flags resolved. */
data class ReleaseRow(
    val id: String,
    val title: String,
    val entries: List<NewReleaseEntry>,
)

/** Everything the Home preview and the New page render. */
data class ReleaseBoard(
    val isLoading: Boolean = true,
    /** Every release from this year + last year (plus anything on the device from that period). */
    val all: List<NewReleaseEntry> = emptyList(),
    val rows: List<ReleaseRow> = emptyList(),
    /** Library titles that belong to any of the above — the "Available for Stream" section. */
    val availableForStream: List<MediaItemEntity> = emptyList(),
)

/**
 * Fetches what was released this year + last year (TMDB), works out which of
 * those titles exist in the user's own library, and — importantly — also adds
 * library titles from that period that TMDB's popularity-sorted lists didn't
 * happen to include, so EVERYTHING on the device from these years gets the
 * Velora V and shows up under "Available for Stream".
 */
class NewReleasesRepository(
    context: Context,
    private val libraryRepository: LibraryRepository,
) {
    private val provider = TmdbDiscoverProvider(context)

    private val bundle = MutableStateFlow<NewReleasesBundle?>(null)
    private val seasonStamps = MutableStateFlow<Map<String, List<SeasonStamp>>>(emptyMap())
    private val refreshLock = Mutex()

    /** Loads (or re-validates the cache of) the release lists. Safe to call from several screens. */
    suspend fun refresh(forceRefresh: Boolean = false) = refreshLock.withLock {
        bundle.value = provider.fetchBundle(forceRefresh)

        val library = libraryRepository.observeAll().first()
        val ownedSeriesIds = library
            .filter { it.type != MediaType.MOVIE }
            .mapNotNull { it.tmdbId?.takeIf { id -> id.isNotBlank() } }
        seasonStamps.value = provider.seasonStampsFor(ownedSeriesIds)
    }

    fun observeBoard(): Flow<ReleaseBoard> =
        combine(bundle, seasonStamps, libraryRepository.observeAll()) { b, stamps, library ->
            if (b == null) ReleaseBoard(isLoading = true) else buildBoard(b, stamps, library)
        }

    // ---------------------------------------------------------------- board

    private suspend fun buildBoard(
        b: NewReleasesBundle,
        stamps: Map<String, List<SeasonStamp>>,
        library: List<MediaItemEntity>,
    ): ReleaseBoard {
        val index = LibraryIndex(library)
        val seasonCache = HashMap<Long, Set<Int>>()
        suspend fun localSeasons(id: Long): Set<Int> =
            seasonCache.getOrPut(id) { libraryRepository.getSeasonsForMedia(id).map { it.seasonNumber }.toSet() }

        val matched = b.all.map { match(it, index) { id -> localSeasons(id) } }
        val injected = injectLocal(matched, stamps, library) { id -> localSeasons(id) }
        val all = (matched + injected).sortedByDescending { it.release.sortKey }

        val rows = b.collections.map { c ->
            ReleaseRow(c.id, c.title, c.items.map { match(it, index) { id -> localSeasons(id) } })
        }.filter { it.entries.isNotEmpty() }

        val available = (all.asSequence() + rows.asSequence().flatMap { it.entries.asSequence() })
            .mapNotNull { it.localItem }
            .distinctBy { it.id }
            .toList()

        return ReleaseBoard(isLoading = false, all = all, rows = rows, availableForStream = available)
    }

    // ---------------------------------------------------------------- matching

    private class LibraryIndex(library: List<MediaItemEntity>) {
        val byTmdb = HashMap<String, MediaItemEntity>()
        val byTitle = HashMap<String, MutableList<MediaItemEntity>>()

        init {
            for (item in library) {
                val kind = kindOf(item.type)
                item.tmdbId?.takeIf { it.isNotBlank() }?.let { byTmdb.putIfAbsent("$kind:$it", item) }
                val titles = listOfNotNull(
                    item.normalizedTitle.ifBlank { normalizeTitleForMatching(item.title) },
                    normalizeTitleForMatching(item.title),
                    item.originalTitle?.let { normalizeTitleForMatching(it) },
                ).flatMap { listOf(it, stripYear(it)) } // "superman 2025" also indexes as "superman"
                    .filter { it.isNotBlank() }.distinct()
                for (t in titles) byTitle.getOrPut("$kind:$t") { mutableListOf() }.add(item)
            }
        }
    }

    /**
     * Matching rules:
     *  1. Same TMDB id (library items that were matched to TMDB carry it) — exact and preferred.
     *  2. Otherwise same normalized title (also ignoring a year the filename left in the title).
     *     Movies must also agree on year (±1, release years differ by region); series don't
     *     compare year because a new SEASON of an old show has a different year than the show.
     *  3. For series entries about one specific season, that season must actually exist locally —
     *     owning Season 1 doesn't mean Season 3 is playable.
     */
    private suspend fun match(
        release: NewReleaseItem,
        index: LibraryIndex,
        localSeasons: suspend (Long) -> Set<Int>,
    ): NewReleaseEntry {
        val kind = kindOf(release.type)
        var local: MediaItemEntity? = index.byTmdb["$kind:${release.tmdbId}"]

        if (local == null) {
            val normalized = normalizeTitleForMatching(release.title)
            val candidates = (index.byTitle["$kind:$normalized"].orEmpty() +
                index.byTitle["$kind:${stripYear(normalized)}"].orEmpty()).distinctBy { it.id }
            local = if (release.type == MediaType.MOVIE) {
                candidates.firstOrNull { c ->
                    val cy = c.year
                    val ry = release.year
                    cy == null || ry == null || abs(cy - ry) <= 1
                }
            } else {
                candidates.firstOrNull()
            }
        }

        val season = release.seasonNumber
        if (local != null && release.type != MediaType.MOVIE && season != null) {
            val seasons = localSeasons(local.id)
            if (seasons.isNotEmpty() && season !in seasons) local = null
        }
        return NewReleaseEntry(release, local)
    }

    /**
     * Library titles from this year / last year that TMDB's lists didn't include.
     * Movies: by their year. Series/anime: by when their seasons premiered (looked up
     * per series), falling back to the show's own first-air year.
     */
    private suspend fun injectLocal(
        matched: List<NewReleaseEntry>,
        stamps: Map<String, List<SeasonStamp>>,
        library: List<MediaItemEntity>,
        localSeasons: suspend (Long) -> Set<Int>,
    ): List<NewReleaseEntry> {
        val today = LocalDate.now()
        val firstYear = today.year - 1
        val windowStart = "$firstYear-01-01"
        val alreadyShown = matched.mapNotNull { it.localItem?.id }.toHashSet()
        val extra = ArrayList<NewReleaseEntry>()

        for (item in library) {
            if (item.id in alreadyShown) continue

            if (item.type == MediaType.MOVIE) {
                val year = item.year ?: continue
                if (year < firstYear || year > today.year) continue
                extra += NewReleaseEntry(
                    release = NewReleaseItem(
                        key = "local-movie-${item.id}",
                        tmdbId = item.tmdbId.orEmpty(),
                        type = MediaType.MOVIE,
                        title = item.title,
                        releaseDate = null,
                        year = year,
                        posterUrl = item.posterUrl,
                        overview = item.overview,
                        rating = item.rating,
                    ),
                    localItem = item,
                )
                continue
            }

            // Series / anime
            val owned = localSeasons(item.id)
            val itemStamps = item.tmdbId?.let { stamps[it] }.orEmpty()
            val recent = itemStamps
                .filter { it.airDate >= windowStart && it.airDate <= today.toString() }
                .filter { owned.isEmpty() || it.number in owned } // only seasons actually on the device
                .maxByOrNull { it.airDate }

            val release = when {
                recent != null -> NewReleaseItem(
                    key = "local-tv-${item.id}-s${recent.number}",
                    tmdbId = item.tmdbId.orEmpty(),
                    type = MediaType.SERIES,
                    title = item.title,
                    releaseDate = recent.airDate,
                    year = recent.airDate.take(4).toIntOrNull(),
                    seasonNumber = recent.number,
                    label = if (recent.number == 1) "New Series" else "Season ${recent.number}",
                    posterUrl = item.posterUrl,
                    overview = item.overview,
                    rating = item.rating,
                )
                // No season dates known (no TMDB match yet): fall back to the show's own year.
                itemStamps.isEmpty() && (item.year ?: 0) in firstYear..today.year -> NewReleaseItem(
                    key = "local-tv-${item.id}",
                    tmdbId = item.tmdbId.orEmpty(),
                    type = MediaType.SERIES,
                    title = item.title,
                    releaseDate = null,
                    year = item.year,
                    label = "New Series",
                    posterUrl = item.posterUrl,
                    overview = item.overview,
                    rating = item.rating,
                )
                else -> null
            }
            if (release != null) extra += NewReleaseEntry(release, item)
        }
        return extra
    }

    private companion object {
        fun kindOf(type: MediaType) = if (type == MediaType.MOVIE) "movie" else "tv"

        /** "superman 2025" -> "superman". Only strips standalone 19xx/20xx tokens. */
        fun stripYear(normalizedTitle: String): String =
            normalizedTitle.replace(Regex("\\b(19|20)\\d{2}\\b"), " ").trim().replace(Regex("\\s+"), " ")
    }
}
