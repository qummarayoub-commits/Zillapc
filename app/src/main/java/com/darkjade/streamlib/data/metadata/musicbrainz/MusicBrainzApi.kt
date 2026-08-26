package com.darkjade.streamlib.data.metadata.musicbrainz

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class MusicBrainzSearchDto(val releases: List<MusicBrainzReleaseDto> = emptyList())
data class MusicBrainzReleaseDto(val id: String, val title: String, val score: Int? = null)

interface MusicBrainzApi {
    @GET("release/")
    suspend fun searchRelease(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 1,
        // MusicBrainz's usage policy requires a descriptive User-Agent
        // identifying the application — not an API key, this service is free.
        @Header("User-Agent") userAgent: String = "Velora/1.0 (local media player)",
    ): MusicBrainzSearchDto
}
