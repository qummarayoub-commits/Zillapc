package com.darkjade.streamlib.data.comic

import android.content.Context
import android.net.Uri
import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

sealed class ComicExtractionResult {
    data class Success(val pages: List<File>) : ComicExtractionResult()
    data class Failed(val message: String) : ComicExtractionResult()
    /** cb7 (7-zip) and anything else we don't extract internally — caller should fall back to an external reader app. */
    object UnsupportedFormat : ComicExtractionResult()
}

/**
 * Extracts a comic archive's image pages into this app's cache directory
 * (once per comic — subsequent opens reuse the cached extraction), so the
 * internal reader can display pages without needing full random file
 * access to the original archive every time.
 */
object ComicExtractor {

    suspend fun extractPages(context: Context, comicId: Long, uri: Uri, fileExtension: String): ComicExtractionResult =
        withContext(Dispatchers.IO) {
            val ext = fileExtension.lowercase()
            val outputDir = File(context.cacheDir, "comics/$comicId")

            // Already extracted from a previous open — reuse it.
            val cachedPages = outputDir.listFiles()
                ?.filter { it.extension.lowercase() in IMAGE_EXTENSIONS }
                ?.sortedBy { it.name }
            if (!cachedPages.isNullOrEmpty()) {
                return@withContext ComicExtractionResult.Success(cachedPages)
            }

            if (ext != "cbz" && ext != "cbr") {
                return@withContext ComicExtractionResult.UnsupportedFormat
            }

            outputDir.mkdirs()

            return@withContext try {
                when (ext) {
                    "cbz" -> extractCbz(context, uri, outputDir)
                    "cbr" -> extractCbr(context, uri, outputDir)
                    else -> ComicExtractionResult.UnsupportedFormat
                }
            } catch (e: Throwable) {
                ComicExtractionResult.Failed(e.message ?: "Could not open this comic file.")
            }
        }

    private fun extractCbz(context: Context, uri: Uri, outputDir: File): ComicExtractionResult {
        val pages = mutableListOf<File>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                var index = 0
                while (entry != null) {
                    val name = entry.name
                    val entryExt = name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && entryExt in IMAGE_EXTENSIONS) {
                        index++
                        val outFile = File(outputDir, "page_%04d.%s".format(index, entryExt))
                        FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        pages.add(outFile)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return ComicExtractionResult.Failed("Could not read this file.")

        return if (pages.isEmpty()) ComicExtractionResult.Failed("No readable pages found in this comic.")
        else ComicExtractionResult.Success(pages.sortedBy { it.name })
    }

    private fun extractCbr(context: Context, uri: Uri, outputDir: File): ComicExtractionResult {
        // junrar needs real random-access File input, so copy the archive into
        // cache first, extract from there, then discard the source copy.
        val sourceCopy = File(context.cacheDir, "comics_src/${outputDir.name}.cbr")
        sourceCopy.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(sourceCopy).use { out -> input.copyTo(out) }
        } ?: return ComicExtractionResult.Failed("Could not read this file.")

        Junrar.extract(sourceCopy, outputDir)
        sourceCopy.delete()

        val pages = outputDir.listFiles()
            ?.filter { it.extension.lowercase() in IMAGE_EXTENSIONS }
            ?.sortedBy { it.name }
            .orEmpty()

        return if (pages.isEmpty()) ComicExtractionResult.Failed("No readable pages found in this comic.")
        else ComicExtractionResult.Success(pages)
    }
}
