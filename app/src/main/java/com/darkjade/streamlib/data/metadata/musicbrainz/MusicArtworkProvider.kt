package com.darkjade.streamlib.data.metadata.musicbrainz

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Priority 3 artwork source (only used when a song has no embedded picture
 * and no local folder cover) — searches MusicBrainz by Artist + Album to
 * find the release, then fetches the actual cover image from the Cover Art
 * Archive. Both are free, open services; no API key required, only a
 * descriptive User-Agent per MusicBrainz's usage policy. Never invents
 * artwork — returns null (and callers fall back to the placeholder) if no
 * match is found.
 */
class MusicArtworkProvider {
    private val api: MusicBrainzApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
    }

    private val httpClient = OkHttpClient()

    suspend fun fetchAlbumArt(artist: String, album: String): ByteArray? {
        if (artist.isBlank() || album.isBlank() || artist == "Unknown Artist" || album == "Unknown Album") return null
        return runCatching {
            val query = "artist:\"${artist.replace("\"", "")}\" AND release:\"${album.replace("\"", "")}\""
            val result = api.searchRelease(query)
            val mbid = result.releases.firstOrNull()?.id ?: return null
            fetchCoverArt(mbid)
        }.getOrNull()
    }

    private fun fetchCoverArt(releaseMbid: String): ByteArray? {
        return runCatching {
            val request = Request.Builder()
                .url("https://coverartarchive.org/release/$releaseMbid/front-500")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        }.getOrNull()
    }
}
