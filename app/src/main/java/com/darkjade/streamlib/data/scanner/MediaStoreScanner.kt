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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.RELATIVE_PATH
            else
                MediaStore.Video.Media.DATA,
        )

        var found = 0
        var processed = 0

        try {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val pathCol = cursor.getColumnIndexOrThrow(projection[3])

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in supportedExtensions) continue

                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val rawPath = cursor.getString(pathCol).orEmpty()

                    // RELATIVE_PATH looks like "Movies/Anime/One Piece/Season 01/"
                    // DATA (pre-Q fallback) is a full filesystem path — normalize both
                    // into folder-name segments the same way the SAF scanner does.
                    val pathSegments = rawPath
                        .trim('/')
                        .split('/')
                        .filter { it.isNotBlank() && it != name }

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
