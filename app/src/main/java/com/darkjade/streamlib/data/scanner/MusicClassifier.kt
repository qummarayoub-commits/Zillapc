package com.darkjade.streamlib.data.scanner

/**
 * Decides whether a scanned audio file is a real song vs. a voice note,
 * call recording, screen recording, podcast, or other non-music audio —
 * using multiple signals together (filename patterns, folder path, and
 * embedded metadata quality), not filename matching alone.
 *
 * Kept as its own small, pure, easily-testable component per the
 * architecture requirement — no Android framework dependencies, just plain
 * data in, a decision out.
 */
object MusicClassifier {

    /** Filename patterns strongly associated with non-music audio. Checked
     * as "starts with" against the filename (without extension), case-insensitive. */
    private val REJECT_FILENAME_PREFIXES = listOf(
        "whatsapp audio", "whatsapp voice", "ptt-", "aud-", "voice", "recording",
        "record-", "rec-", "audio recording", "call_recording", "call recording",
        "call-", "callrecord", "voice note", "voicenote", "voice_note", "vm-",
        "screen recording", "screenrecording", "notification", "ringtone", "alarm",
    )

    /** Folder-name signals — a path segment matching one of these strongly
     * suggests the file isn't music, regardless of its own filename. */
    private val REJECT_FOLDER_SIGNALS = listOf(
        "whatsapp", "telegram", "messenger", "call recordings", "callrecordings",
        "recordings", "voice notes", "voicenotes", "screen recordings", "screenrecordings",
        "notifications", "ringtones", "alarms", "podcasts", "audiobooks",
    )

    /** Folder-name signals suggesting genuine music organization — a boost, not a requirement. */
    private val MUSIC_FOLDER_SIGNALS = listOf("music", "songs", "albums", "artists")

    data class Candidate(
        val fileNameWithoutExtension: String,
        val pathSegments: List<String>, // parent folder names
        val title: String?,
        val artist: String?,
        val album: String?,
        val trackNumber: Int?,
        val genre: String?,
        val hasEmbeddedArtwork: Boolean,
        val durationMs: Long,
    )

    /** Confidence threshold — candidates scoring at or above this are
     * auto-imported; below it, they're skipped (but never deleted/modified —
     * the user can still add them manually via "Add to Music"). */
    const val AUTO_IMPORT_THRESHOLD = 3

    fun isLikelyMusic(candidate: Candidate): Boolean = score(candidate) >= AUTO_IMPORT_THRESHOLD

    fun score(candidate: Candidate): Int {
        val lowerName = candidate.fileNameWithoutExtension.lowercase().trim()
        val lowerPath = candidate.pathSegments.joinToString("/").lowercase()

        // Hard rejects — filename or folder unmistakably signals non-music,
        // regardless of any metadata present.
        if (REJECT_FILENAME_PREFIXES.any { lowerName.startsWith(it) }) return -10
        if (REJECT_FOLDER_SIGNALS.any { signal -> candidate.pathSegments.any { it.lowercase() == signal } }) return -10

        var score = 0

        val hasRealArtist = !candidate.artist.isNullOrBlank() && candidate.artist != "<unknown>"
        val hasRealAlbum = !candidate.album.isNullOrBlank()
        val hasMeaningfulTitle = !candidate.title.isNullOrBlank() && candidate.title.length > 2 &&
            candidate.title.lowercase() != lowerName // a real tag, not just the filename echoed back

        if (hasRealArtist) score += 2
        if (hasRealAlbum) score += 1
        if (hasMeaningfulTitle) score += 2
        if (candidate.trackNumber != null) score += 1
        if (candidate.hasEmbeddedArtwork) score += 1
        if (!candidate.genre.isNullOrBlank()) score += 1
        if (MUSIC_FOLDER_SIGNALS.any { signal -> candidate.pathSegments.any { it.lowercase().contains(signal) } }) score += 1

        // Generic/absent metadata drags confidence down even without a hard reject.
        if (!hasRealArtist && !hasMeaningfulTitle) score -= 2
        if (candidate.durationMs in 1..15_000) score -= 2 // very short clips read as voice notes/system sounds

        return score
    }
}
