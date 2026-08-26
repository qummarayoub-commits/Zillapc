package com.darkjade.streamlib.data.metadata.omdb

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OMDb (omdbapi.com) — the only practical source for genuine IMDb ratings
 * and Rotten Tomatoes percentages. TMDB's own `vote_average` is TMDB's
 * community score, not IMDb's, and must never be labeled as IMDb.
 */
data class OmdbResponseDto(
    val Title: String? = null,
    val imdbID: String? = null,
    val imdbRating: String? = null, // e.g. "7.6" or "N/A"
    val Metascore: String? = null,  // e.g. "67" or "N/A"
    val Ratings: List<OmdbRatingDto> = emptyList(),
    val Response: String? = null, // "True" / "False"
    val Error: String? = null,
)

data class OmdbRatingDto(
    val Source: String, // "Internet Movie Database" | "Rotten Tomatoes" | "Metacritic"
    val Value: String,  // "7.6/10" | "85%" | "67/100"
)

interface OmdbApi {
    /** Lookup by IMDb ID — preferred when we already know it (from TMDB's external_ids), far more reliable than title matching. */
    @GET("/")
    suspend fun getByImdbId(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String,
    ): OmdbResponseDto

    /** Fallback lookup by title/year/type when no IMDb ID is known yet. */
    @GET("/")
    suspend fun getByTitle(
        @Query("apikey") apiKey: String,
        @Query("t") title: String,
        @Query("y") year: Int? = null,
        @Query("type") type: String? = null, // "movie" | "series"
    ): OmdbResponseDto
}
