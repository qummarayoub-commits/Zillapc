package com.darkjade.streamlib.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.darkjade.streamlib.data.parser.MediaFilenameParser
import com.darkjade.streamlib.data.parser.ParsedMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Extensions the scanner treats as playable video. Easy to extend later. */
object SupportedVideoExtensions {
    val DEFAULT = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts")
}

data class ScannedFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val pathSegments: List<String>, // parent folder names, root -> immediate parent
    val parsed: ParsedMedia,
    val durationMs: Long = 0, // 0 = unknown (SAF scanner doesn't probe this; MediaStore does)
)

sealed class ScanEvent {
    data class Progress(val found: Int, val processed: Int, val currentName: String) : ScanEvent()
    data class FileFound(val file: ScannedFile) : ScanEvent()
    data class Completed(val totalFound: Int) : ScanEvent()
    data class Failed(val message: String) : ScanEvent()
}

/**
 * Recursively walks a SAF tree URI, finding supported video files.
 * Runs entirely off the main thread (caller collects from a background
 * dispatcher — see ScanLibraryWorker).
 */
class LibraryScanner(private val context: Context) {

    fun scanTree(
        treeUri: Uri,
        supportedExtensions: Set<String> = SupportedVideoExtensions.DEFAULT,
    ): Flow<ScanEvent> = flow {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.isDirectory) {
            emit(ScanEvent.Failed("Could not open folder: $treeUri"))
            return@flow
        }

        var found = 0
        var processed = 0

        suspend fun walk(dir: DocumentFile, pathSegments: List<String>) {
            val children = try {
                dir.listFiles()
            } catch (e: Exception) {
                // Permission revoked or folder deleted mid-scan — skip gracefully.
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
                        val parsed = MediaFilenameParser.parse(name, pathSegments)
                        val scanned = ScannedFile(
                            uri = child.uri,
                            displayName = name,
                            sizeBytes = child.length(),
                            pathSegments = pathSegments,
                            parsed = parsed,
                        )
                        emit(ScanEvent.FileFound(scanned))
                        processed++
                        emit(ScanEvent.Progress(found, processed, name))
                    }
                }
            }
        }

        try {
            walk(root, listOf(root.name.orEmpty()))
            emit(ScanEvent.Completed(found))
        } catch (e: Exception) {
            emit(ScanEvent.Failed(e.message ?: "Unknown scan error"))
        }
    }
}
