package com.darkjade.streamlib.data.parser

import com.darkjade.streamlib.data.db.entity.MediaType

/**
 * Result of parsing a filename (and optional parent folder names) into
 * structured media info. All fields are nullable/best-effort — a bad or
 * unusual filename must never throw, only fall back to sensible defaults.
 */
data class ParsedMedia(
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val quality: String? = null,
    val type: MediaType,
    val rawFilename: String,
)

/**
 * Best-effort filename/folder parser inspired by Nova/Kodi-style naming
 * conventions. Does not depend on any single pattern matching — falls
 * back progressively until it has *something* usable.
 */
object MediaFilenameParser {

    // Real fix: release names very commonly write "S01.E01" (a dot between
    // season and episode, e.g. Scam.1992.S01.E01...) - after the dot->space
    // normalization below that becomes "S01 E01", which the old
    // S(\d{1,2})E(\d{1,4}) pattern (no separator allowed) never matched, so
    // the file silently fell through to the movie branch instead - one
    // "movie" per episode, never grouped into a series. The optional
    // [\s._-]* here tolerates S01E01, S01.E01, S01_E01, S01-E01, and S01 E01.
    private val seasonEpisodeRegexes = listOf(
        // One.Piece.S01E01.1080p.mkv / Breaking.Bad.S02E05.720p.mkv / Scam.1992.S01.E01...
        Regex("""(?i)S(\d{1,2})[\s._-]*E(\d{1,4})"""),
        // One Piece - 001.mkv  /  One Piece 001.mkv
        Regex("""(?i)[\-\s](\d{2,4})(?:\s|\.|$)"""),
        // 1x01 style
        Regex("""(?i)(\d{1,2})x(\d{1,4})"""),
    )

    private val yearRegex = Regex("""(?:19|20)\d{2}""")

    private val qualityRegex = Regex("""(?i)\b(480p|720p|1080p|2160p|4k|HDR|WEB[- ]?DL|BluRay|BRRip|HDTV)\b""")

    private val noiseTokens = listOf(
        "x264", "x265", "HEVC", "AAC", "DDP5.1", "10bit", "WEBRip", "WEB-DL",
        "BluRay", "BRRip", "HDTV", "PROPER", "REPACK", "EXTENDED"
    )

    fun parse(filenameWithExt: String, folderHints: List<String> = emptyList()): ParsedMedia {
        val filename = filenameWithExt.substringBeforeLast('.', filenameWithExt)
        val normalized = filename.replace('.', ' ').replace('_', ' ').trim()

        val quality = qualityRegex.find(normalized)?.value
        val year = yearRegex.find(normalized)?.value?.toIntOrNull()

        // Try SxxExx pattern first (series/anime)
        val seasonEpMatch = seasonEpisodeRegexes[0].find(normalized)
        if (seasonEpMatch != null) {
            val season = seasonEpMatch.groupValues[1].toIntOrNull()
            val episode = seasonEpMatch.groupValues[2].toIntOrNull()
            val title = cleanTitle(normalized.substringBefore(seasonEpMatch.value), folderHints)
            return ParsedMedia(
                title = title,
                season = season,
                episode = episode,
                year = year,
                quality = quality,
                type = MediaType.SERIES,
                rawFilename = filenameWithExt,
            )
        }

        // Try 1x01 pattern
        val xMatch = seasonEpisodeRegexes[2].find(normalized)
        if (xMatch != null) {
            val season = xMatch.groupValues[1].toIntOrNull()
            val episode = xMatch.groupValues[2].toIntOrNull()
            val title = cleanTitle(normalized.substringBefore(xMatch.value), folderHints)
            return ParsedMedia(
                title = title,
                season = season,
                episode = episode,
                year = year,
                quality = quality,
                type = MediaType.SERIES,
                rawFilename = filenameWithExt,
            )
        }

        // Anime-style: "One Piece - 001" or "One Piece 001" — no explicit season, assume 1
        val animeEpMatch = Regex("""[\-\s](\d{2,4})(?:\s|$)""").find(normalized)
        // Only fall back to "anime-style trailing number" parsing when there's an
        // explicit anime folder hint, OR when no year was detected — a detected
        // year (e.g. "Movie.Name.2024.1080p.mkv") means the trailing digits are
        // almost certainly a release year, not an episode number, so a movie
        // must never be misclassified as anime just because it lacks "SxxExx".
        if (animeEpMatch != null && folderHints.any { it.contains("anime", ignoreCase = true) } ||
            (animeEpMatch != null && year == null && looksLikeAnimeNumbering(normalized))
        ) {
            val episode = animeEpMatch.groupValues[1].toIntOrNull()
            val title = cleanTitle(normalized.substringBefore(animeEpMatch.value), folderHints)
            return ParsedMedia(
                title = title,
                season = 1,
                episode = episode,
                year = year,
                quality = quality,
                type = MediaType.ANIME,
                rawFilename = filenameWithExt,
            )
        }

        // Fall back: treat as a movie. Title = everything before the year, or full name.
        val movieTitle = if (year != null) {
            cleanTitle(normalized.substringBefore(year.toString()), folderHints)
        } else {
            cleanTitle(normalized, folderHints)
        }

        return ParsedMedia(
            title = movieTitle.ifBlank { filenameWithExt },
            season = null,
            episode = null,
            year = year,
            quality = quality,
            type = MediaType.MOVIE,
            rawFilename = filenameWithExt,
        )
    }

    /** Uses folder structure (e.g. Anime/One Piece/Season 01/) to reinforce identification. */
    fun folderTypeHint(pathSegments: List<String>): MediaType? {
        val lower = pathSegments.map { it.lowercase() }
        return when {
            lower.any { it.contains("anime") } -> MediaType.ANIME
            lower.any { it.contains("tv show") || it.contains("tv shows") || it.contains("series") } -> MediaType.SERIES
            lower.any { it.contains("movie") } -> MediaType.MOVIE
            else -> null
        }
    }

    private fun looksLikeAnimeNumbering(normalized: String): Boolean {
        // Heuristic: no "S0xE0x", has a 2-4 digit trailing number preceded by a dash or space,
        // and does not look like a resolution/year.
        return !normalized.contains(Regex("(?i)S\\d{1,2}E\\d{1,4}"))
    }

    private fun cleanTitle(raw: String, folderHints: List<String>): String {
        var title = raw
        for (token in noiseTokens) {
            title = title.replace(Regex("(?i)\\b${Regex.escape(token)}\\b"), "")
        }
        title = title.replace(Regex("""\s+"""), " ").trim(' ', '-', '.')

        if (title.isBlank() && folderHints.isNotEmpty()) {
            // Fall back to the nearest meaningful folder name (e.g. "One Piece" from
            // Anime/One Piece/Season 01/).
            title = folderHints.lastOrNull { !it.matches(Regex("(?i)season\\s*\\d+")) }.orEmpty()
        }
        return title.trim()
    }
}
