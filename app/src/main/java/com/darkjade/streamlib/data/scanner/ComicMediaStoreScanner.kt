package com.darkjade.streamlib.data.scanner

import android.content.Context
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.darkjade.streamlib.data.parser.ComicFilenameParser
import com.darkjade.streamlib.data.parser.SupportedComicExtensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scans the device's MediaStore Files index for comic files
 * (.cbz/.cbr/.cb7/.pdf) — same approach as MediaStoreScanner uses for
 * videos, and for the same reason: it works consistently across OEMs
 * without needing a user-picked SAF folder (which was crashing/unreliable
 * on some devices). MediaStore.Files indexes all files, not just
 * video/audio/image, so this alone is enough — no folder picking needed.
 */
class ComicMediaStoreScanner(private val context: Context) {

    fun scanDevice(
        supportedExtensions: Set<String> = SupportedComicExtensions.DEFAULT,
    ): Flow<ComicScanEvent> = flow {
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Files.FileColumns.RELATIVE_PATH
            else
                MediaStore.Files.FileColumns.DATA,
        )

        var found = 0
        var processed = 0

        try {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val pathCol = cursor.getColumnIndexOrThrow(projection[3])

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in supportedExtensions) continue

                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val rawPath = cursor.getString(pathCol).orEmpty()

                    val pathSegments = rawPath
                        .trim('/')
                        .split('/')
                        .filter { it.isNotBlank() && it != name }

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val parsed = ComicFilenameParser.parse(name, pathSegments)

                    found++
                    emit(
                        ComicScanEvent.FileFound(
                            ScannedComic(
                                uri = contentUri,
                                displayName = name,
                                sizeBytes = size,
                                pathSegments = pathSegments,
                                parsed = parsed,
                            )
                        )
                    )
                    processed++
                    emit(ComicScanEvent.Progress(found, processed, name))
                }
            }
            emit(ComicScanEvent.Completed(found))
        } catch (e: SecurityException) {
            emit(ComicScanEvent.Failed("Permission denied — please allow storage access in Settings."))
        } catch (e: Throwable) {
            emit(ComicScanEvent.Failed(e.message ?: "Unknown error while scanning device for comics"))
        }
    }
}
