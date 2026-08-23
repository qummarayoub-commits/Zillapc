package com.darkjade.streamlib.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val MY_LISTS = "my_lists"
    const val BROWSE = "browse"
    const val SEARCH = "search"
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val DETAILS = "details/{mediaId}"
    const val COMIC_DETAILS = "comic_details/{comicId}"
    const val COMIC_READER = "comic_reader/{comicId}"
    const val PLAYER = "player/{mediaId}/{episodeId}"
    const val NEWS = "news"
    const val NEWS_ARTICLE = "news_article/{articleId}"
    const val SWITCH_PROFILE = "switch_profile"

    fun details(mediaId: Long) = "details/$mediaId"
    fun comicDetails(comicId: Long) = "comic_details/$comicId"
    fun comicReader(comicId: Long) = "comic_reader/$comicId"
    fun player(mediaId: Long, episodeId: Long?) = "player/$mediaId/${episodeId ?: -1L}"
    fun newsArticle(articleId: Long) = "news_article/$articleId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlineIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.MY_LISTS, "My Lists", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem(Routes.BROWSE, "Browse", Icons.Filled.GridView, Icons.Outlined.GridView),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
)
