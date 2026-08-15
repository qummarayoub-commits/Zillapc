package com.darkjade.streamlib.data.parser

data class ParsedComic(
    val seriesGuess: String,
    val issueNumber: String?,
    val rawFilename: String,
)

/** Extensions the comic scanner treats as readable comic files. */
object SupportedComicExtensions {
    val DEFAULT = setOf("cbz", "cbr", "cb7", "pdf")
}

/**
 * Best-effort parser for comic filenames like:
 *   "Amazing Spider-Man 045 (2020).cbz"
 *   "Batman #045.cbr"
 *   "Spider-Man_45.cbz"
 * Never throws — an unparseable name just falls back to the filename as the
 * series guess with no issue number, same "never crash" philosophy as the
 * movie/show parser.
 */
object ComicFilenameParser {

    private val issueNumberRegex = Regex("""(?:#|\bissue\s*)?(\d{1,4})(?:\s|\.|$|\))""", RegexOption.IGNORE_CASE)
    private val yearRegex = Regex("""\((?:19|20)\d{2}\)""")

    fun parse(filenameWithExt: String, folderHints: List<String> = emptyList()): ParsedComic {
        val filename = filenameWithExt.substringBeforeLast('.', filenameWithExt)
        var normalized = filename.replace('.', ' ').replace('_', ' ').trim()
        normalized = normalized.replace(yearRegex, "").trim()

        val issueMatch = issueNumberRegex.find(normalized)
        val issueNumber = issueMatch?.groupValues?.get(1)

        val seriesGuess = if (issueMatch != null) {
            normalized.substring(0, issueMatch.range.first).trim(' ', '-', '#')
        } else {
            normalized.trim()
        }.ifBlank {
            folderHints.lastOrNull()?.trim() ?: filenameWithExt
        }

        return ParsedComic(
            seriesGuess = seriesGuess,
            issueNumber = issueNumber,
            rawFilename = filenameWithExt,
        )
    }
}
