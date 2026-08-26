package com.darkjade.streamlib.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Extensions the scanner treats as (potential) music. */
object SupportedMusicExtensions {
    val DEFAULT = setOf("mp3", "flac", "m4a", "aac", "ogg", "wav", "opus")
}

data class ScannedSong(
    val uri: Uri,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val trackNumber: Int?,
    val year: Int?,
    val genre: String?,
    val durationMs: Long,
    val embeddedArtwork: ByteArray?,
)

sealed class MusicScanEvent {
    data class Progress(val found: Int, val processed: Int, val currentName: String) : MusicScanEvent()
    data class SongFound(val song: ScannedSong) : MusicScanEvent()
    /** Real audio file that scored below the music-confidence threshold —
     * never imported automatically, but available for a future manual
     * "Add to Music" flow. The file itself is never touched. */
    data class SkippedLowConfidence(val uri: Uri, val displayName: String) : MusicScanEvent()
    data class Completed(val totalFound: Int) : MusicScanEvent()
    data class Failed(val message: String) : MusicScanEvent()
}

/**
 * Walks a user-selected SAF folder tree (the same architecture already used
 * for video via LibraryScanner) looking for common local audio files,
 * extracts real embedded tag metadata via MediaMetadataRetriever, and runs
 * each candidate through [MusicClassifier] so voice notes/call recordings/
 * screen recordings/podcasts/etc. never get auto-imported as "songs" — only
 * real music, per real signals, gets into the library automatically.
 */
class MusicScanner(private val context: Context) {

    fun scanTree(
        treeUri: Uri,
        supportedExtensions: Set<String> = SupportedMusicExtensions.DEFAULT,
    ): Flow<MusicScanEvent> = flow {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.isDirectory) {
            emit(MusicScanEvent.Failed("Could not open folder: $treeUri"))
            return@flow
        }

        var found = 0
        var processed = 0

        suspend fun walk(dir: DocumentFile, pathSegments: List<String>) {
            val children = try {
                dir.listFiles()
            } catch (e: Exception) {
                emptyArray()
            }

            for (child in children) {
                if (child.isDirectory) {
                    val name = child.name.orEmpty()
                    walk(child, pathSegments + name)
                } else if (child.isFile) {
                    val name = child.name.orEmpty()
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in supportedExtensions) {
                        found++
                        val event = classifyAndExtract(child.uri, name, pathSegments)
                        emit(event)
                        processed++
                        emit(MusicScanEvent.Progress(found, processed, name))
                    }
                }
            }
        }

        walk(root, emptyList())
        emit(MusicScanEvent.Completed(found))
    }

    /**
     * Scans the WHOLE device for music — the primary, simple flow (matches
     * how "Scan Device for Videos" works for movies): no folder picker
     * needed. Uses MediaStore.Audio to find candidate files device-wide
     * (just needs READ_MEDIA_AUDIO, not All Files Access), then reads real
     * tags via MediaMetadataRetriever and runs every candidate through
     * MusicClassifier — so this still only imports real songs, not every
     * WhatsApp voice note or call recording on the device.
     */
    fun scanDevice(): Flow<MusicScanEvent> = flow {
        val collection = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
            android.provider.MediaStore.Audio.Media.RELATIVE_PATH,
        )
        var found = 0
        var processed = 0
        try {
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in SupportedMusicExtensions.DEFAULT) continue

                    val relativePath = if (pathCol >= 0) cursor.getString(pathCol) else null
                    val pathSegments = relativePath?.trim('/')?.split('/').orEmpty()
                    val uri = android.content.ContentUris.withAppendedId(collection, id)

                    found++
                    emit(classifyAndExtract(uri, name, pathSegments))
                    processed++
                    emit(MusicScanEvent.Progress(found, processed, name))
                }
            }
            emit(MusicScanEvent.Completed(found))
        } catch (e: Exception) {
            emit(MusicScanEvent.Failed(e.message ?: "Music scan failed"))
        }
    }

    /** Reads real embedded tag data, scores it with [MusicClassifier], and
     * only ever returns a [MusicScanEvent.SongFound] for candidates that
     * genuinely look like music — otherwise a [MusicScanEvent.SkippedLowConfidence],
     * never fabricating title/artist for a low-confidence file just to import it. */
    private fun classifyAndExtract(uri: Uri, displayName: String, pathSegments: List<String>): MusicScanEvent {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() && it != "<unknown>" }
            val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() }
            val trackRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val track = trackRaw?.substringBefore('/')?.trim()?.toIntOrNull()
            val yearRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            val year = yearRaw?.take(4)?.toIntOrNull()
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val artwork = retriever.embeddedPicture

            val candidate = MusicClassifier.Candidate(
                fileNameWithoutExtension = displayName.substringBeforeLast('.'),
                pathSegments = pathSegments,
                title = rawTitle,
                artist = rawArtist,
                album = rawAlbum,
                trackNumber = track,
                genre = genre,
                hasEmbeddedArtwork = artwork != null,
                durationMs = durationMs,
            )

            if (!MusicClassifier.isLikelyMusic(candidate)) {
                return MusicScanEvent.SkippedLowConfidence(uri, displayName)
            }

            MusicScanEvent.SongFound(
                ScannedSong(
                    uri = uri,
                    displayName = displayName,
                    title = rawTitle ?: displayName.substringBeforeLast('.'),
                    artist = rawArtist ?: "Unknown Artist",
                    album = rawAlbum ?: "Unknown Album",
                    albumArtist = albumArtist,
                    trackNumber = track,
                    year = year,
                    genre = genre,
                    durationMs = durationMs,
                    embeddedArtwork = artwork,
                )
            )
        } catch (e: Exception) {
            MusicScanEvent.SkippedLowConfidence(uri, displayName)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
            }
        }
    }
}
