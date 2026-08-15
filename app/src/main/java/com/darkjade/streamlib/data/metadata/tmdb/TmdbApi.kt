package com.darkjade.streamlib.data.metadata.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Minimal DTOs — only fields we actually use.

data class TmdbSearchResponse<T>(val results: List<T> = emptyList())

data class TmdbMovieDto(
    val id: Int,
    val title: String?,
    val original_title: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double?,
    val runtime: Int?,
    val release_date: String?,
    val genres: List<TmdbGenreDto>? = null,
    val credits: TmdbCreditsDto? = null,
    val images: TmdbImagesDto? = null,
)

data class TmdbSeriesDto(
    val id: Int,
    val name: String?,
    val original_name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double?,
    val first_air_date: String?,
    val genres: List<TmdbGenreDto>? = null,
    val number_of_seasons: Int? = null,
    val credits: TmdbCreditsDto? = null,
    val images: TmdbImagesDto? = null,
)

data class TmdbImagesDto(val posters: List<TmdbPosterDto> = emptyList())
data class TmdbPosterDto(val file_path: String)

data class TmdbGenreDto(val id: Int, val name: String)

data class TmdbCreditsDto(
    val cast: List<TmdbCastDto> = emptyList(),
    val crew: List<TmdbCrewDto> = emptyList(),
)

data class TmdbCastDto(val name: String, val order: Int = Int.MAX_VALUE)
data class TmdbCrewDto(val name: String, val job: String)

data class TmdbSeasonDto(
    val season_number: Int,
    val name: String?,
    val poster_path: String?,
    val episodes: List<TmdbEpisodeDto> = emptyList(),
)

data class TmdbEpisodeDto(
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val still_path: String?,
    val runtime: Int?,
)

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: Int? = null,
    ): TmdbSearchResponse<TmdbMovieDto>

    @GET("movie/{id}")
    suspend fun getMovie(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,images",
    ): TmdbMovieDto

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: Int? = null,
    ): TmdbSearchResponse<TmdbSeriesDto>

    @GET("tv/{id}")
    suspend fun getSeries(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,images",
    ): TmdbSeriesDto

    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun getSeason(
        @Path("id") id: Int,
        @Path("seasonNumber") seasonNumber: Int,
        @Query("api_key") apiKey: String,
    ): TmdbSeasonDto
}
