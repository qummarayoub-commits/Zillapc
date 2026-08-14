package com.darkjade.streamlib.data.parser

import com.darkjade.streamlib.data.db.entity.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFilenameParserTest {

    @Test
    fun `parses standard SxxExx series filename`() {
        val result = MediaFilenameParser.parse("Breaking.Bad.S02E05.720p.mkv")
        assertEquals(MediaType.SERIES, result.type)
        assertEquals(2, result.season)
        assertEquals(5, result.episode)
        assertEquals("720p", result.quality)
    }

    @Test
    fun `parses movie filename with year`() {
        val result = MediaFilenameParser.parse("Movie.Name.2024.1080p.mkv")
        assertEquals(MediaType.MOVIE, result.type)
        assertEquals(2024, result.year)
        assertEquals("Movie Name", result.title)
    }

    @Test
    fun `parses anime-style numbered filename with folder hint`() {
        val result = MediaFilenameParser.parse("One Piece - 899.mkv", listOf("Anime", "One Piece", "Season 01"))
        assertEquals(1, result.season)
        assertEquals(899, result.episode)
    }

    @Test
    fun `never throws on unusual filenames`() {
        val weird = listOf("", "....mkv", "###weird###.mp4", "no_extension", "  spaced  .mkv")
        weird.forEach { name ->
            // Should not throw regardless of how malformed the filename is.
            MediaFilenameParser.parse(name)
        }
    }

    @Test
    fun `folder type hint overrides ambiguous guesses`() {
        assertEquals(MediaType.ANIME, MediaFilenameParser.folderTypeHint(listOf("Storage", "Anime", "One Piece")))
        assertEquals(MediaType.MOVIE, MediaFilenameParser.folderTypeHint(listOf("Storage", "Movies")))
        assertEquals(null, MediaFilenameParser.folderTypeHint(listOf("Storage", "Random")))
    }
}
