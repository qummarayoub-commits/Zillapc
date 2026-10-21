package com.darkjade.streamlib.ui.screens.player

import android.content.Context
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

/**
 * Exposes one user-selected local media URI over the current LAN so a Cast
 * receiver can read it. The random path token prevents accidental discovery
 * by other devices on the same network. Requests support byte ranges so
 * Chromecast seeking works.
 */
class LocalMediaHttpServer(private val appContext: Context) : NanoHTTPD(0) {
    @Volatile private var mediaUri: Uri? = null
    @Volatile private var mediaName: String = "video"
    @Volatile private var accessToken: String = UUID.randomUUID().toString().replace("-", "")

    @Synchronized
    fun urlFor(uri: Uri, title: String): String {
        mediaUri = uri
        mediaName = title
        accessToken = UUID.randomUUID().toString().replace("-", "")
        if (!isAlive) {
            start(SOCKET_READ_TIMEOUT, false)
        }
        val address = localIpv4Address()
            ?: throw IllegalStateException("Connect your phone to Wi-Fi before casting a local video.")
        return "http://$address:$listeningPort/$accessToken"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = mediaUri
        if (uri == null || session.uri != "/$accessToken") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        return try {
            val source = openSource(uri)
            val totalLength = source.second
            if (totalLength < 0L) {
                source.first.close()
                return newFixedLengthResponse(
                    Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE,
                    MIME_PLAINTEXT,
                    "Media length unavailable"
                )
            }

            val rangeHeader = session.headers["range"]
            val range = parseRange(rangeHeader, totalLength)
            if (rangeHeader != null && range == null) {
                source.first.close()
                val response = newFixedLengthResponse(
                    Response.Status.RANGE_NOT_SATISFIABLE,
                    MIME_PLAINTEXT,
                    "Invalid range"
                )
                response.addHeader("Content-Range", "bytes */$totalLength")
                return response
            }

            val start = range?.first ?: 0L
            val end = range?.last ?: (totalLength - 1L)
            val length = (end - start + 1L).coerceAtLeast(0L)
            skipFully(source.first, start)

            val mime = mimeTypeFor(mediaName)
            val status = if (rangeHeader != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
            val response = newFixedLengthResponse(status, mime, source.first, length)
            response.addHeader("Accept-Ranges", "bytes")
            response.addHeader("Content-Length", length.toString())
            response.addHeader("Cache-Control", "no-store")
            if (rangeHeader != null) response.addHeader("Content-Range", "bytes $start-$end/$totalLength")
            response
        } catch (error: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, error.message ?: "Unable to read media")
        }
    }

    fun stopServer() {
        try {
            if (isAlive) stop()
        } catch (_: Exception) {
            // Safe during ViewModel cleanup.
        }
    }

    private fun openSource(uri: Uri): Pair<InputStream, Long> {
        return when (uri.scheme) {
            "file" -> {
                val file = File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
                FileInputStream(file) to file.length()
            }
            "content" -> {
                val descriptor = appContext.contentResolver.openAssetFileDescriptor(uri, "r")
                    ?: throw IllegalArgumentException("Cannot open selected video")
                val stream = descriptor.createInputStream()
                // The asset descriptor owns the underlying file descriptor. Closing the stream
                // closes it too, so no separate descriptor close is needed.
                stream to descriptor.length
            }
            else -> throw IllegalArgumentException("Only local files can be served by this server")
        }
    }

    private fun parseRange(header: String?, total: Long): LongRange? {
        if (header.isNullOrBlank()) return null
        val match = Regex("^bytes=(\\d*)-(\\d*)$").matchEntire(header.trim()) ?: return null
        val first = match.groupValues[1]
        val last = match.groupValues[2]
        return try {
            if (first.isEmpty() && last.isEmpty()) return null
            if (first.isEmpty()) {
                val suffix = last.toLong()
                if (suffix <= 0) return null
                (total - suffix.coerceAtMost(total)) until total
            } else {
                val start = first.toLong()
                val end = if (last.isEmpty()) total - 1 else last.toLong().coerceAtMost(total - 1)
                if (start < 0 || start >= total || end < start) null else start..end
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                if (stream.read() == -1) throw IllegalStateException("Unexpected end of media")
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun mimeTypeFor(title: String): String = when (title.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
    }

    private fun localIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
