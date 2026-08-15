package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.db.entity.NewsCategory
import com.darkjade.streamlib.data.news.NewsSource
import com.darkjade.streamlib.data.news.NewsSources
import com.darkjade.streamlib.data.news.RssItem
import com.darkjade.streamlib.data.news.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class NewsRefreshResult {
    data class Success(val newArticleCount: Int) : NewsRefreshResult()
    object Offline : NewsRefreshResult()
    data class PartialFailure(val failedSources: List<String>, val newArticleCount: Int) : NewsRefreshResult()
}

/**
 * Fetches from every configured RSS source, parses, categorizes, dedupes
 * (by article URL), sorts by publish date, and caches locally. If one
 * source fails (bad feed, timeout, 404), the others still succeed — this
 * never lets a single broken source take down the whole News feed.
 */
class NewsRepository(context: Context) {
    private val db = StreamLibDatabase.getInstance(context)
    private val dao = db.newsArticleDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun observeAll() = dao.observeAll()
    fun observeByCategory(category: NewsCategory) = dao.observeByCategory(category)
    suspend fun search(query: String) = if (query.isBlank()) emptyList() else dao.search(query)
    suspend fun hasAnyCachedArticles() = dao.count() > 0

    /** Fetches every source concurrently; a failed individual source is skipped, not fatal. */
    suspend fun refreshAll(): NewsRefreshResult = withContext(Dispatchers.IO) {
        val existingUrls = try {
            dao.getAllUrls().toHashSet()
        } catch (e: Exception) {
            hashSetOf()
        }

        val failedSources = mutableListOf<String>()
        val allNewArticles = mutableListOf<NewsArticleEntity>()

        val results = coroutineScope {
            NewsSources.ALL.map { source ->
                async { fetchSource(source) }
            }.awaitAll()
        }

        results.forEachIndexed { index, items ->
            val source = NewsSources.ALL[index]
            if (items == null) {
                failedSources.add(source.name)
                return@forEachIndexed
            }
            items.forEach { item ->
                if (item.link !in existingUrls) {
                    allNewArticles.add(
                        NewsArticleEntity(
                            articleUrl = item.link,
                            headline = item.title,
                            excerpt = item.description?.take(280),
                            imageUrl = item.imageUrl,
                            category = source.category,
                            sourceName = source.name,
                            publishedAt = item.pubDateMs,
                        )
                    )
                    existingUrls.add(item.link)
                }
            }
        }

        if (allNewArticles.isEmpty() && failedSources.size == NewsSources.ALL.size) {
            // Every single source failed — most likely no network at all.
            return@withContext NewsRefreshResult.Offline
        }

        try {
            dao.insertAll(allNewArticles)
            dao.trimOldArticles()
        } catch (e: Exception) {
            // Never crash the app over a cache-write failure.
        }

        if (failedSources.isEmpty()) NewsRefreshResult.Success(allNewArticles.size)
        else NewsRefreshResult.PartialFailure(failedSources, allNewArticles.size)
    }

    /** Returns null on failure (network error, bad feed, etc.) rather than throwing. */
    private fun fetchSource(source: NewsSource): List<RssItem>? {
        return try {
            val request = Request.Builder().url(source.feedUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.byteStream()?.use { stream -> RssParser.parse(stream) }
            }
        } catch (e: Throwable) {
            null
        }
    }
}
