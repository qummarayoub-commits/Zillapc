package com.darkjade.streamlib.data.scanner

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.darkjade.streamlib.data.parser.ComicFilenameParser
import com.darkjade.streamlib.data.parser.SupportedComicExtensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.ArrayDeque

/**
 * Scans the device's entire external storage for comic files
 * (.cbz/.cbr/.cb7/.pdf) using a real filesystem walk.
 *
 * Why not MediaStore: MediaStore's Files collection only reliably indexes
 * video/audio/image on Android 10+ under scoped storage — arbitrary
 * document-type files like .cbz/.cbr/.pdf are frequently NOT present in it
 * at all unless the app already has broad storage access, which was the
 * actual root cause of comic scanning silently finding nothing. This
 * scanner instead walks java.io.File directly, which works reliably once
 * the user grants "All files access" (MANAGE_EXTERNAL_STORAGE) — see
 * ComicScanPermission for the permission check/request flow.
 *
 * Uses an explicit stack instead of recursion to avoid any StackOverflow
 * risk on deeply nested folder structures.
 */
class ComicFileSystemScanner(private val context: Context) {

    fun scanDevice(
        supportedExtensions: Set<String> = SupportedComicExtensions.DEFAULT,
    ): Flow<ComicScanEvent> = flow {
        val root = Environment.getExternalStorageDirectory()
        if (root == null || !root.exists()) {
            emit(ComicScanEvent.Failed("Could not access device storage."))
            return@flow
        }

        var found = 0
        var processed = 0

        try {
            val stack = ArrayDeque<File>()
            stack.push(root)

            while (stack.isNotEmpty()) {
                val dir = stack.pop()

                // Skip Android's own sandboxed app-data directories — nothing
                // relevant to the user's own comics lives there, and some of
                // those paths throw/are inaccessible even with All Files Access.
                if (dir.name == "Android" && dir.parentFile == root) continue

                val children = try {
                    dir.listFiles()
                } catch (e: Throwable) {
                    null
                } ?: continue

                for (child in children) {
                    if (child.isDirectory) {
                        stack.push(child)
                    } else if (child.isFile) {
                        val name = child.name
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in supportedExtensions) {
                            found++
                            val relativePath = child.parentFile?.absolutePath
                                ?.removePrefix(root.absolutePath)
                                ?.trim('/')
                                ?.split('/')
                                ?.filter { it.isNotBlank() }
                                .orEmpty()

                            val parsed = ComicFilenameParser.parse(name, relativePath)
                            val uri = try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    child
                                )
                            } catch (e: Throwable) {
                                Uri.fromFile(child)
                            }

                            emit(
                                ComicScanEvent.FileFound(
                                    ScannedComic(
                                        uri = uri,
                                        displayName = name,
                                        sizeBytes = child.length(),
                                        pathSegments = relativePath,
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
            emit(ComicScanEvent.Completed(found))
        } catch (e: Throwable) {
            emit(ComicScanEvent.Failed(e.message ?: "Unknown error while scanning device for comics"))
        }
    }
}
