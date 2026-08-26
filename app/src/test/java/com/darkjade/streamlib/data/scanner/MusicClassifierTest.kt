package com.darkjade.streamlib.data.scanner

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
    fun `real song with full metadata is accepted`() {
        val c = candidate(
            fileName = "Shape of You",
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            trackNumber = 4,
            genre = "Pop",
            hasArtwork = true,
        )
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `track-numbered song with artist and album is accepted`() {
        val c = candidate(
            fileName = "01 - Believer",
            title = "Believer",
            artist = "Imagine Dragons",
            album = "Evolve",
            trackNumber = 1,
        )
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `generic filename with complete music metadata is still accepted`() {
        val c = candidate(
            fileName = "song",
            title = "Real Song Title",
            artist = "Real Artist",
            album = "Real Album",
            genre = "Rock",
            hasArtwork = true,
        )
        assertTrue(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `whatsapp audio is rejected even with no other signal`() {
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
    fun `completely generic filename with no metadata at all is rejected`() {
        val c = candidate(fileName = "audio", artist = null, title = null, album = null)
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `very short clip is rejected even with a title`() {
        val c = candidate(fileName = "clip", title = "Something", durationMs = 3000)
        assertFalse(MusicClassifier.isLikelyMusic(c))
    }

    @Test
    fun `file in a Music folder gets a confidence boost`() {
        val withFolder = candidate(
            fileName = "track1",
            title = "Track One",
            artist = "Some Artist",
            pathSegments = listOf("Music", "MyAlbum"),
        )
        val withoutFolder = candidate(
            fileName = "track1",
            title = "Track One",
            artist = "Some Artist",
        )
        assertTrue(MusicClassifier.score(withFolder) > MusicClassifier.score(withoutFolder))
    }
}
