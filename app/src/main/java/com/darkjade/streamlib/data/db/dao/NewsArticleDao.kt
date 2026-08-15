package com.darkjade.streamlib.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.db.entity.NewsCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<NewsArticleEntity>): List<Long>

    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishedAt DESC")
    fun observeByCategory(category: NewsCategory): Flow<List<NewsArticleEntity>>

    @Query("""
        SELECT * FROM news_articles 
        WHERE headline LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%'
        ORDER BY publishedAt DESC
    """)
    suspend fun search(query: String): List<NewsArticleEntity>

    @Query("SELECT articleUrl FROM news_articles")
    suspend fun getAllUrls(): List<String>

    /** Keeps the local cache from growing forever — only the most recent N articles are kept. */
    @Query("""
        DELETE FROM news_articles WHERE id NOT IN (
            SELECT id FROM news_articles ORDER BY publishedAt DESC LIMIT :keepCount
        )
    """)
    suspend fun trimOldArticles(keepCount: Int = 300)

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun count(): Int
}
