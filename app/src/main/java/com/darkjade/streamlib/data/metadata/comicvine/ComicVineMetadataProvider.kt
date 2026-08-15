package com.darkjade.streamlib.data.metadata.comicvine

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Result of a comic issue lookup — provider-agnostic, mirrors MetadataResult's role for movies/shows. */
data class ComicMetadataResult(
    val title: String,
    val seriesName: String,
    val issueNumber: String?,
    val publisher: String?,
    val overview: String?,
    val coverUrl: String?,
    val releaseDate: String?,
)

object ComicVineConfig {
    const val BASE_URL = "https://comicvine.gamespot.com/api/"

    /** Set from Settings (persisted via PreferencesRepository/DataStore) — see SettingsViewModel. */
    var apiKey: String = ""
}

class ComicVineMetadataProvider {

    // ComicVine rejects requests without a real User-Agent header.
    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "DarkVault/1.0 (Android)")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ComicVineConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ComicVineApi::class.java)

    /**
     * Searches by a best-effort query built from the parsed filename (series
     * name + issue number). Never throws — returns null on any failure, so a
     * comic without a match just falls back to its local filename, exactly
     * like movies/shows do.
     */
    suspend fun searchIssue(seriesGuess: String, issueNumber: String?): ComicMetadataResult? {
        if (ComicVineConfig.apiKey.isBlank()) return null
        val query = if (issueNumber != null) "$seriesGuess $issueNumber" else seriesGuess
        return runCatching {
            val response = api.searchIssues(ComicVineConfig.apiKey, query)
            val match = response.results.firstOrNull() ?: return null
            ComicMetadataResult(
                title = match.name?.takeIf { it.isNotBlank() }
                    ?: buildString {
                        append(match.volume?.name ?: seriesGuess)
                        match.issue_number?.let { append(" #$it") }
                    },
                seriesName = match.volume?.name ?: seriesGuess,
                issueNumber = match.issue_number ?: issueNumber,
                publisher = match.volume?.publisher?.name,
                overview = match.description?.let { stripHtml(it) },
                coverUrl = match.image?.medium_url ?: match.image?.small_url,
                releaseDate = match.cover_date,
            )
        }.getOrNull()
    }

    private fun stripHtml(input: String): String =
        input.replace(Regex("<[^>]*>"), "").trim()
}
