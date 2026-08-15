package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NewsCategory { MOVIES, SERIES, ANIME, ANIMATION, COMICS }

/**
 * A single news article fetched from an RSS source. Kept as its own table,
 * fully separate from the movie/series/comic library — this feature is
 * purely additive and never touches existing tables.
 */
@Entity(
    tableName = "news_articles",
    indices = [Index(value = ["articleUrl"], unique = true)]
)
data class NewsArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleUrl: String, // used as the de-dup key (guid/link)
    val headline: String,
    val excerpt: String?,
    val imageUrl: String?,
    val category: NewsCategory,
    val sourceName: String,
    val publishedAt: Long, // epoch millis, for sorting
    val fetchedAt: Long = System.currentTimeMillis(),
)
