package com.darkjade.streamlib.data.news

import com.darkjade.streamlib.data.db.entity.NewsCategory

data class NewsSource(
    val name: String,
    val feedUrl: String,
    val category: NewsCategory,
)

/**
 * The list of RSS sources the News feed pulls from. Each source maps to
 * exactly one category. Adding a new source later is just adding a line
 * here — NewsRepository/NewsViewModel/NewsScreen don't need to change.
 */
object NewsSources {
    val ALL = listOf(
        NewsSource("IGN", "https://feeds.ign.com/ign/movies-articles", NewsCategory.MOVIES),
        NewsSource("IGN", "https://feeds.ign.com/ign/tv-articles", NewsCategory.SERIES),
        NewsSource("Anime News Network", "https://www.animenewsnetwork.com/all/rss.xml", NewsCategory.ANIME),
        NewsSource("Animation World Network", "https://www.awn.com/rss.xml", NewsCategory.ANIMATION),
        NewsSource("CBR", "https://www.cbr.com/feed/category/comics/", NewsCategory.COMICS),
    )
}
