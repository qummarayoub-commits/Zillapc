package com.darkjade.streamlib.data.scanner

/**
 * Decides whether a scanned audio file is a real song vs. an obvious voice
 * note, call recording, or screen recording — REJECT-FIRST design: a file
 * is only excluded when it clearly matches a non-music pattern (filename
 * or folder). Everything else is treated as music by default, even with
 * no metadata at all, since plenty of legitimately-ripped/downloaded songs
 * have no ID3 tags whatsoever. We must never turn a folder full of real
 * songs into "one song" by being too strict.
 */
object MusicClassifier {

    private val REJECT_FILENAME_PREFIXES = listOf(
        "whatsapp audio", "whatsapp voice", "ptt-", "aud-", "voice note", "voicenote",
        "voice_note", "recording", "record-", "rec-", "audio recording",
        "call_recording", "call recording", "call-", "callrecord", "vm-",
        "screen recording", "screenrecording", "notification", "ringtone", "alarm",
    )

    /** Bare "voice" alone is common as a false-positive prefix for real song
     * titles (e.g. "Voices" by a band), so it's only rejected when followed
     * by a clear non-music word, not as a blanket prefix. */
    private val REJECT_FILENAME_CONTAINS = listOf("voice message", "voice memo")

    private val REJECT_FOLDER_SIGNALS = listOf(
        "whatsapp", "telegram", "messenger", "call recordings", "callrecordings",
        "recordings", "voice notes", "voicenotes", "screen recordings", "screenrecordings",
        "notifications", "ringtones", "alarms",
    )

    data class Candidate(
        val fileNameWithoutExtension: String,
        val pathSegments: List<String>,
        val title: String?,
        val artist: String?,
        val album: String?,
        val trackNumber: Int?,
        val genre: String?,
        val hasEmbeddedArtwork: Boolean,
        val durationMs: Long,
    )

    enum class Confidence { HIGH, MEDIUM, LOW }

    /** HIGH and MEDIUM are auto-imported; only LOW is skipped (and even
     * then, only via the manual "Add to Music" path, never deleted). */
    fun isLikelyMusic(candidate: Candidate): Boolean = classify(candidate) != Confidence.LOW

    fun classify(candidate: Candidate): Confidence {
        val lowerName = candidate.fileNameWithoutExtension.lowercase().trim()

        // Hard rejects — unmistakable non-music signal. This is the ONLY
        // path to LOW; missing metadata alone never results in LOW.
        if (REJECT_FILENAME_PREFIXES.any { lowerName.startsWith(it) }) return Confidence.LOW
        if (REJECT_FILENAME_CONTAINS.any { lowerName.contains(it) }) return Confidence.LOW
        if (REJECT_FOLDER_SIGNALS.any { signal -> candidate.pathSegments.any { it.lowercase() == signal } }) return Confidence.LOW
        // Genuinely tiny clips (a couple of seconds) read as system sounds/notifications.
        if (candidate.durationMs in 1..8_000) return Confidence.LOW

        val hasRealArtist = !candidate.artist.isNullOrBlank() && candidate.artist != "<unknown>"
        val hasRealAlbum = !candidate.album.isNullOrBlank()
        val hasRealTitle = !candidate.title.isNullOrBlank()

        val signalCount = listOf(
            hasRealArtist,
            hasRealAlbum,
            hasRealTitle,
            candidate.trackNumber != null,
            candidate.hasEmbeddedArtwork,
            !candidate.genre.isNullOrBlank(),
        ).count { it }

        // Any real tag data at all -> HIGH. No metadata whatsoever is still
        // MEDIUM (not LOW) as long as it didn't hit a hard reject above —
        // per the explicit rule: a file with no tags can still be a real song.
        return if (signalCount >= 1) Confidence.HIGH else Confidence.MEDIUM
    }
}
