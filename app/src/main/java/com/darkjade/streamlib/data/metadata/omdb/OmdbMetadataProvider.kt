package com.darkjade.streamlib.data.metadata.omdb

import com.darkjade.streamlib.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class OmdbResult(
    val imdbId: String?,
    val imdbRating: Double?,       // out of 10, genuinely from IMDb via OMDb's "Internet Movie Database" rating source
    val rottenTomatoesPercent: Int?, // 0-100, from OMDb's "Rotten Tomatoes" rating source
    val metacriticScore: Int?,       // 0-100, from OMDb's own Metascore field
)

class OmdbMetadataProvider {
    private val apiKey: String = BuildConfig.OMDB_API_KEY

    private val api: OmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmdbApi::class.java)
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun fetchByImdbId(imdbId: String): OmdbResult? {
        if (!isConfigured) return null
        return runCatching { parse(api.getByImdbId(apiKey, imdbId)) }.getOrNull()
    }

    suspend fun fetchByTitle(title: String, year: Int?, isSeries: Boolean): OmdbResult? {
        if (!isConfigured) return null
        return runCatching {
            parse(api.getByTitle(apiKey, title, year, if (isSeries) "series" else "movie"))
        }.getOrNull()
    }

    private fun parse(dto: OmdbResponseDto): OmdbResult? {
        if (dto.Response != "True") return null

        val imdbRating = dto.imdbRating
            ?.takeIf { it != "N/A" }
            ?.toDoubleOrNull()
            ?: dto.Ratings.firstOrNull { it.Source == "Internet Movie Database" }
                ?.Value?.substringBefore('/')?.toDoubleOrNull()

        val rtPercent = dto.Ratings.firstOrNull { it.Source == "Rotten Tomatoes" }
            ?.Value?.removeSuffix("%")?.toIntOrNull()

        val metacritic = dto.Metascore
            ?.takeIf { it != "N/A" }
            ?.toIntOrNull()
            ?: dto.Ratings.firstOrNull { it.Source == "Metacritic" }
                ?.Value?.substringBefore('/')?.toIntOrNull()

        return OmdbResult(
            imdbId = dto.imdbID,
            imdbRating = imdbRating,
            rottenTomatoesPercent = rtPercent,
            metacriticScore = metacritic,
        )
    }
}
