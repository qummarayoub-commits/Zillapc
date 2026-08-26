package com.darkjade.streamlib.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.darkjade.streamlib.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scans the device's MediaStore audio index — mirrors [MediaStoreScanner]'s
 * approach for video, for the same reason: MediaStore is the one content
 * provider every OEM implements consistently, and it already surfaces
 * title/artist/album/duration/track/year — real embedded tag metadata —
 * without us needing to open and parse each file ourselves.
 */
class MusicScanner(private val context: Context) {

    fun scanDevice(): Flow<MusicScanEvent> = flow {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
        )

        val songs = mutableListOf<SongEntity>()
        var found = 0
        var skipped = 0

        try {
            // IS_MUSIC=1 filters out notification tones, alarms, call recordings, etc.
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumArtistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val durationMs = cursor.getLong(durationCol)
                    // Skip extremely short clips (voice memos, notification-style audio
                    // that slipped through IS_MUSIC) — real songs are essentially always
                    // longer than 20s.
                    if (durationMs < 20_000) {
                        skipped++
                        continue
                    }

                    val title = cursor.getString(titleCol) ?: continue
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    // MediaStore.Audio.Media.TRACK often encodes as (disc*1000 + track);
                    // keep just the track portion when that pattern is present.
                    val rawTrack = if (trackCol >= 0 && !cursor.isNull(trackCol)) cursor.getInt(trackCol) else null
                    val track = rawTrack?.let { if (it > 1000) it % 1000 else it }

                    songs.add(
                        SongEntity(
                            mediaStoreId = id,
                            title = title,
                            artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist",
                            album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album",
                            albumArtist = if (albumArtistCol >= 0) cursor.getString(albumArtistCol) else null,
                            trackNumber = track,
                            year = if (yearCol >= 0 && !cursor.isNull(yearCol)) cursor.getInt(yearCol).takeIf { it > 0 } else null,
                            genre = if (genreCol >= 0) cursor.getString(genreCol) else null,
                            durationMs = durationMs,
                            localFileUri = contentUri.toString(),
                            albumId = cursor.getLong(albumIdCol),
                        )
                    )
                    found++
                    if (found % 50 == 0) emit(MusicScanEvent.Progress(found, skipped))
                }
            }
            emit(MusicScanEvent.Complete(songs))
        } catch (e: Exception) {
            emit(MusicScanEvent.Error(e.message ?: "Music scan failed"))
        }
    }

    /** Album art URI for a given album — MediaStore's standard artwork content path. */
    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

sealed class MusicScanEvent {
    data class Progress(val found: Int, val skipped: Int) : MusicScanEvent()
    data class Complete(val songs: List<SongEntity>) : MusicScanEvent()
    data class Error(val message: String) : MusicScanEvent()
}
