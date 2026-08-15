package com.darkjade.streamlib.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.darkjade.streamlib.data.db.dao.ComicDao
import com.darkjade.streamlib.data.db.dao.EpisodeDao
import com.darkjade.streamlib.data.db.dao.FolderSourceDao
import com.darkjade.streamlib.data.db.dao.MediaItemDao
import com.darkjade.streamlib.data.db.dao.NewsArticleDao
import com.darkjade.streamlib.data.db.dao.PlaybackProgressDao
import com.darkjade.streamlib.data.db.dao.ProfileDao
import com.darkjade.streamlib.data.db.dao.ScanStatusDao
import com.darkjade.streamlib.data.db.dao.SeasonDao
import com.darkjade.streamlib.data.db.dao.WatchHistoryDao
import com.darkjade.streamlib.data.db.dao.WatchlistDao
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.FolderSourceEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.db.entity.PlaybackProgressEntity
import com.darkjade.streamlib.data.db.entity.ProfileEntity
import com.darkjade.streamlib.data.db.entity.ScanStatusEntity
import com.darkjade.streamlib.data.db.entity.SeasonEntity
import com.darkjade.streamlib.data.db.entity.WatchHistoryEntity
import com.darkjade.streamlib.data.db.entity.WatchlistEntity

@Database(
    entities = [
        MediaItemEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        ProfileEntity::class,
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        FolderSourceEntity::class,
        ScanStatusEntity::class,
        ComicEntity::class,
        PlaybackProgressEntity::class,
        NewsArticleEntity::class,
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class StreamLibDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun profileDao(): ProfileDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun folderSourceDao(): FolderSourceDao
    abstract fun scanStatusDao(): ScanStatusDao
    abstract fun comicDao(): ComicDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun newsArticleDao(): NewsArticleDao

    companion object {
        @Volatile private var INSTANCE: StreamLibDatabase? = null

        fun getInstance(context: Context): StreamLibDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreamLibDatabase::class.java,
                    "streamlib.db"
                )
                    // Safe default while schema evolves during active development.
                    // TODO: replace with real Migration objects before shipping v2 schema changes.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
