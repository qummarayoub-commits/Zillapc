package com.darkjade.streamlib.data.scanner

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.darkjade.streamlib.data.parser.MediaFilenameParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scans the device's MediaStore video index instead of walking a
 * user-picked SAF folder tree.
 *
 * Why this exists: SAF tree access (see LibraryScanner) depends on each
 * OEM's DocumentsProvider correctly honoring persistable URI permissions.
 * On some Android skins (this app has repeatedly hit this on ZTE/MyOS
 * devices) that grant silently fails or is unreliable, which either
 * crashes the app or makes the scan quietly find nothing. MediaStore is a
 * core system content provider — the same one Nova, VLC, and virtually
 * every Android video player use — so it works consistently across
 * OEMs and only needs the standard READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE
 * runtime permission, not a folder-specific grant.
 *
 * Trade-off: MediaStore indexes *every* video on the device — WhatsApp
 * statuses, Instagram/TikTok downloader cache clips, camera recordings,
 * screen recordings, etc. — not just movies/shows. [MediaJunkFilter] below
 * filters those out heuristically so the library stays clean.
 */
class MediaStoreScanner(private val context: Context) {

    fun scanDevice(
        supportedExtensions: Set<String> = SupportedVideoExtensions.DEFAULT,
    ): Flow<ScanEvent> = flow {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.RELATIVE_PATH
            else
                MediaStore.Video.Media.DATA,
        )

        var found = 0
        var processed = 0
        var skipped = 0

        try {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val pathCol = cursor.getColumnIndexOrThrow(projection[4])

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in supportedExtensions) continue

                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val durationMs = cursor.getLong(durationCol)
                    val rawPath = cursor.getString(pathCol).orEmpty()

                    val pathSegments = rawPath
                        .trim('/')
                        .split('/')
                        .filter { it.isNotBlank() && it != name }

                    if (MediaJunkFilter.isJunk(name, pathSegments, durationMs, size)) {
                        skipped++
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val parsed = MediaFilenameParser.parse(name, pathSegments)

                    found++
                    val scanned = ScannedFile(
                        uri = contentUri,
                        displayName = name,
                        sizeBytes = size,
                        pathSegments = pathSegments,
                        parsed = parsed,
                    )
                    emit(ScanEvent.FileFound(scanned))
                    processed++
                    emit(ScanEvent.Progress(found, processed, name))
                }
            }
            emit(ScanEvent.Completed(found))
        } catch (e: SecurityException) {
            emit(ScanEvent.Failed("Permission denied — please allow video access in Settings."))
        } catch (e: Exception) {
            emit(ScanEvent.Failed(e.message ?: "Unknown error while scanning device media"))
        }
    }
}

/**
 * Heuristics to exclude non-movie/show clutter from a device-wide
 * MediaStore scan: WhatsApp/Telegram/Instagram/TikTok media, camera
 * recordings, screen recordings, and downloader-generated hash filenames.
 * None of this is perfect — it's tuned to keep obvious junk out while
 * never being so aggressive that a real movie/episode gets excluded.
 */
object MediaJunkFilter {

    private val junkPathSegments = listOf(
        "whatsapp", "whatsapp images", "whatsapp video", "whatsapp animated gifs",
        "whatsapp voice notes", "statuses", ".statuses",
        "telegram", "telegram images", "telegram video",
        "instagram", "snapchat", "tiktok", "musically",
        "camera", "screenshots", "screen recordings", "screenrecorder",
        ".thumbnails", "thumbnails", "cache", ".cache",
        "facebook", "messenger", "share", "shareit", "xender",
    )

    // Real movies/episodes are essentially never shorter than this.
    // WhatsApp/Instagram/TikTok clips are almost always well under it.
    private const val MIN_DURATION_MS = 10 * 60 * 1000L // 10 minutes

    // Downloader apps commonly save files as bare hex hashes / UUIDs with no
    // real title in them at all (e.g. "1ec7ccee056f1cb4fd418f4dc0c51d6d 720w.mp4").
    private val hashLikeNameRegex = Regex("(?i)^[0-9a-f]{16,}([ _-][a-z0-9]+)?$")

    fun isJunk(fileName: String, pathSegments: List<String>, durationMs: Long, sizeBytes: Long): Boolean {
        val nameWithoutExt = fileName.substringBeforeLast('.', fileName)

        if (pathSegments.any { segment -> junkPathSegments.any { segment.equals(it, ignoreCase = true) } }) {
            return true
        }

        if (hashLikeNameRegex.matches(nameWithoutExt.trim())) {
            return true
        }

        // Caption-style social media filenames often start with hashtags.
        if (nameWithoutExt.trim().startsWith("#")) {
            return true
        }

        // Duration 0 usually means MediaStore hasn't indexed it properly yet
        // (still allow it through — better a false negative than hiding a
        // real file) but a known short duration is a strong junk signal.
        if (durationMs in 1 until MIN_DURATION_MS) {
            return true
        }

        return false
    }
}
