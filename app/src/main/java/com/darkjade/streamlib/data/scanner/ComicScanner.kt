package com.darkjade.streamlib.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.darkjade.streamlib.data.parser.ComicFilenameParser
import com.darkjade.streamlib.data.parser.ParsedComic
import com.darkjade.streamlib.data.parser.SupportedComicExtensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ScannedComic(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val pathSegments: List<String>,
    val parsed: ParsedComic,
)

sealed class ComicScanEvent {
    data class Progress(val found: Int, val processed: Int, val currentName: String) : ComicScanEvent()
    data class FileFound(val file: ScannedComic) : ComicScanEvent()
    data class Completed(val totalFound: Int) : ComicScanEvent()
    data class Failed(val message: String) : ComicScanEvent()
}

/**
 * Walks a user-picked SAF folder tree looking only for comic files
 * (.cbz/.cbr/.cb7/.pdf) — deliberately separate from the video library
 * scanner (MediaStoreScanner) so comic scanning never touches videos and
 * vice versa. Comics aren't part of MediaStore's video/audio/image
 * collections, so unlike videos there's no device-wide index to query;
 * the user points this at their comics folder once.
 */
class ComicScanner(private val context: Context) {

    fun scanTree(
        treeUri: Uri,
        supportedExtensions: Set<String> = SupportedComicExtensions.DEFAULT,
    ): Flow<ComicScanEvent> = flow {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.isDirectory) {
            emit(ComicScanEvent.Failed("Could not open folder: $treeUri"))
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
                    walk(child, pathSegments + child.name.orEmpty())
                } else if (child.isFile) {
                    val name = child.name.orEmpty()
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in supportedExtensions) {
                        found++
                        val parsed = ComicFilenameParser.parse(name, pathSegments)
                        emit(
                            ComicScanEvent.FileFound(
                                ScannedComic(
                                    uri = child.uri,
                                    displayName = name,
                                    sizeBytes = child.length(),
                                    pathSegments = pathSegments,
                                    parsed = parsed,
                                )
                            )
                        )
                        processed++
                        emit(ComicScanEvent.Progress(found, processed, name))
                    }
                }
            }
        }

        try {
            walk(root, listOf(root.name.orEmpty()))
            emit(ComicScanEvent.Completed(found))
        } catch (e: Exception) {
            emit(ComicScanEvent.Failed(e.message ?: "Unknown error while scanning comics folder"))
        }
    }
}
