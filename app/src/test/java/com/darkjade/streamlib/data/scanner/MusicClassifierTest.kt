package com.darkjade.streamlib.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicClassifierTest {

    private fun candidate(
        fileName: String,
        pathSegments: List<String> = emptyList(),
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        trackNumber: Int? = null,
        genre: String? = null,
        hasArtwork: Boolean = false,
        durationMs: Long = 180_000,
    ) = MusicClassifier.Candidate(
        fileNameWithoutExtension = fileName,
        pathSegments = pathSegments,
        title = title,
        artist = artist,
        album = album,
        trackNumber = trackNumber,
        genre = genre,
        hasEmbeddedArtwork = hasArtwork,
        durationMs = durationMs,
    )

    @Test
    fun `real song with full metadata is accepted as HIGH`() {
        val c = candidate(
            fileName = "Shape of You",
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            trackNumber = 4,
            genre = "Pop",
            hasArtwork = true,
        )
        assertEquals(MusicClassifier.Confidence.HIGH, MusicClassifier.classify(c))
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `song with only a title tag is still accepted`() {
        val c = candidate(fileName = "01", title = "Believer")
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `generic filename with zero metadata is still accepted (MEDIUM, not rejected)`() {
        val c = candidate(fileName = "01")
        assertTrue(MusicClassifier.isLikelyMusic(c))
        assertEquals(MusicClassifier.Confidence.MEDIUM, MusicClassifier.classify(c))
    }

    @Test
    fun `a whole folder of untagged mp3s is never reduced to one song`() {
        val songs = listOf("01", "02", "03", "04", "05").map { candidate(fileName = it) }
        songs.forEach { assertTrue(MusicClassifier.isLikelyMusic(it)) }
    }

    @Test
    fun `whatsapp audio is rejected`() {
        val c = candidate(fileName = "WhatsApp Audio 2026-08-26 at 10.15.00")
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `AUD- prefixed voice recording is rejected`() {
        val c = candidate(fileName = "AUD-1234")
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `generic Recording filename is rejected`() {
        val c = candidate(fileName = "Recording_001")
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `voice note filename is rejected`() {
        val c = candidate(fileName = "Voice Note")
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `call recording is rejected`() {
        val c = candidate(fileName = "call_recording")
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `file inside a WhatsApp folder is rejected regardless of filename`() {
        val c = candidate(
            fileName = "PTT-20260826-WA0001",
            pathSegments = listOf("WhatsApp", "Media", "WhatsApp Audio"),
        )
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `a song legitimately titled Voices is NOT rejected`() {
        val c = candidate(fileName = "Voices", title = "Voices", artist = "Some Band")
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `very short clip is rejected even with a title`() {
        val c = candidate(fileName = "clip", title = "Something", durationMs = 3000)
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }
}
