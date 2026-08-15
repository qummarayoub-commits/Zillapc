package com.darkjade.streamlib.data.metadata.comicvine

import retrofit2.http.GET
import retrofit2.http.Query

// Minimal DTOs — ComicVine's API returns a lot more than we need.

data class ComicVineSearchResponse(
    val results: List<ComicVineIssueDto> = emptyList(),
)

data class ComicVineIssueDto(
    val id: Int,
    val name: String? = null, // issue title, often null
    val issue_number: String? = null,
    val description: String? = null,
    val cover_date: String? = null,
    val image: ComicVineImageDto? = null,
    val volume: ComicVineVolumeRefDto? = null,
)

data class ComicVineImageDto(
    val super_url: String? = null,
    val original_url: String? = null,
    val medium_url: String? = null,
    val small_url: String? = null,
)

data class ComicVineVolumeRefDto(
    val name: String? = null,
    val publisher: ComicVinePublisherDto? = null,
)

data class ComicVinePublisherDto(val name: String? = null)

interface ComicVineApi {
    @GET("search/")
    suspend fun searchIssues(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("resources") resources: String = "issue",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
    ): ComicVineSearchResponse
}
