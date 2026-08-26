package com.darkjade.streamlib.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.darkjade.streamlib.AppContainer
import com.darkjade.streamlib.player.ExternalPlayerLauncher
import com.darkjade.streamlib.player.PlaybackLaunchResult
import com.darkjade.streamlib.ui.components.VaultBottomBar
import com.darkjade.streamlib.ui.player.MiniPlayerBar
import com.darkjade.streamlib.ui.player.MusicPlayerScreen
import com.darkjade.streamlib.ui.player.MusicPlayerViewModel
import com.darkjade.streamlib.ui.screens.account.AccountScreen
import com.darkjade.streamlib.ui.screens.account.AccountViewModel
import com.darkjade.streamlib.ui.screens.browse.BrowseScreen
import com.darkjade.streamlib.ui.screens.browse.BrowseViewModel
import com.darkjade.streamlib.ui.screens.comics.ComicDetailsScreen
import com.darkjade.streamlib.ui.screens.comics.ComicDetailsViewModel
import com.darkjade.streamlib.ui.screens.comics.ComicReaderScreen
import com.darkjade.streamlib.ui.screens.comics.ComicReaderViewModel
import com.darkjade.streamlib.ui.screens.details.DetailsScreen
import com.darkjade.streamlib.ui.screens.details.DetailsViewModel
import com.darkjade.streamlib.ui.screens.home.HomeScreen
import com.darkjade.streamlib.ui.screens.home.HomeViewModel
import com.darkjade.streamlib.ui.screens.mylists.MyListsScreen
import com.darkjade.streamlib.ui.screens.mylists.MyListsViewModel
import com.darkjade.streamlib.ui.screens.news.NewsArticleDetailsScreen
import com.darkjade.streamlib.ui.screens.news.NewsArticleDetailsViewModel
import com.darkjade.streamlib.ui.screens.news.NewsScreen
import com.darkjade.streamlib.ui.screens.news.NewsViewModel
import com.darkjade.streamlib.ui.screens.player.PlayerScreen
import com.darkjade.streamlib.ui.screens.player.PlayerViewModel
import com.darkjade.streamlib.ui.screens.search.SearchScreen
import com.darkjade.streamlib.ui.screens.search.SearchViewModel
import com.darkjade.streamlib.ui.screens.settings.SettingsScreen
import com.darkjade.streamlib.ui.screens.settings.SettingsViewModel
import com.darkjade.streamlib.ui.util.SimpleViewModelFactory
import kotlinx.coroutines.launch

private val topLevelRoutes = setOf(Routes.HOME, Routes.MY_LISTS, Routes.BROWSE, Routes.NEWS, Routes.SEARCH, Routes.MUSIC)

@Composable
fun StreamLibNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun openComic(uriString: String) {
        val result = ExternalPlayerLauncher.openComic(context, Uri.parse(uriString))
        if (result is PlaybackLaunchResult.NoPlayerFound) {
            Toast.makeText(context, "No comic reader app found. Please install one.", Toast.LENGTH_LONG).show()
        } else if (result is PlaybackLaunchResult.Failed) {
            Toast.makeText(context, "Couldn't open file: ${result.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Created once here (Activity-scoped via LocalViewModelStoreOwner default),
    // so it survives navigation between Home/Movies/Series/Music/etc. and
    // keeps the mini-player controllable everywhere.
    val musicPlayerViewModel: MusicPlayerViewModel = viewModel(factory = SimpleViewModelFactory {
        MusicPlayerViewModel(context.applicationContext, container.musicRepository)
    })
    val musicPlayerState by musicPlayerViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                androidx.compose.foundation.layout.Column {
                    if (musicPlayerState.currentSong != null) {
                        MiniPlayerBar(
                            state = musicPlayerState,
                            onTogglePlayPause = { musicPlayerViewModel.togglePlayPause() },
                            onNext = { musicPlayerViewModel.next() },
                            onTap = { navController.navigate(Routes.MUSIC_PLAYER) },
                        )
                    }
                    VaultBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .statusBarsPadding()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = SimpleViewModelFactory {
                    HomeViewModel(
                        container.libraryRepository,
                        container.watchRepository,
                        container.profileRepository,
                        container.comicRepository,
                        container.musicRepository,
                    )
                })
                HomeScreen(
                    viewModel = vm,
                    onOpenDetails = { navController.navigate(Routes.details(it)) },
                    onOpenComicDetails = { navController.navigate(Routes.comicDetails(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenNews = { navController.navigate(Routes.NEWS) },
                    onOpenBrowse = { navController.navigate(Routes.BROWSE) },
                    onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                    onOpenMusic = { navController.navigate(Routes.MUSIC) },
                )
            }

            composable(Routes.MY_LISTS) {
                val vm: MyListsViewModel = viewModel(factory = SimpleViewModelFactory {
                    MyListsViewModel(container.libraryRepository, container.watchRepository, container.profileRepository)
                })
                MyListsScreen(viewModel = vm, onOpenDetails = { navController.navigate(Routes.details(it)) })
            }

            composable(Routes.BROWSE) {
                val vm: BrowseViewModel = viewModel(factory = SimpleViewModelFactory {
                    BrowseViewModel(container.libraryRepository, container.comicRepository, container.watchRepository, container.profileRepository)
                })
                BrowseScreen(
                    viewModel = vm,
                    onOpenDetails = { navController.navigate(Routes.details(it)) },
                    onOpenComicDetails = { navController.navigate(Routes.comicDetails(it)) },
                )
            }

            composable(Routes.MUSIC) {
                val vm: com.darkjade.streamlib.ui.screens.music.MusicViewModel = viewModel(factory = SimpleViewModelFactory {
                    com.darkjade.streamlib.ui.screens.music.MusicViewModel(container.musicRepository)
                })
                val musicState by vm.uiState.collectAsState()
                com.darkjade.streamlib.ui.screens.music.MusicScreen(
                    viewModel = vm,
                    onOpenAlbum = { _, _ -> },
                    onOpenArtist = { },
                    onOpenPlaylist = { playlistId -> navController.navigate(Routes.playlistDetail(playlistId)) },
                    onPlaySong = { song -> musicPlayerViewModel.playSong(song, musicState.allSongs) },
                )
            }

            composable(
                Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
                val vm: com.darkjade.streamlib.ui.screens.music.PlaylistDetailViewModel = viewModel(factory = SimpleViewModelFactory {
                    com.darkjade.streamlib.ui.screens.music.PlaylistDetailViewModel(playlistId, container.musicRepository)
                })
                com.darkjade.streamlib.ui.screens.music.PlaylistDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { song, queue -> musicPlayerViewModel.playSong(song, queue) },
                    onPlayShuffled = { songs ->
                        if (songs.isNotEmpty()) {
                            musicPlayerViewModel.playSong(songs.random(), songs)
                        }
                    },
                )
            }

            composable(Routes.MUSIC_PLAYER) {
                MusicPlayerScreen(viewModel = musicPlayerViewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = SimpleViewModelFactory {
                    SearchViewModel(container.libraryRepository, container.watchRepository, container.profileRepository)
                })
                SearchScreen(viewModel = vm, onOpenDetails = { navController.navigate(Routes.details(it)) })
            }

            composable(Routes.ACCOUNT) {
                val vm: AccountViewModel = viewModel(factory = SimpleViewModelFactory {
                    AccountViewModel(container.profileRepository)
                })
                AccountScreen(
                    viewModel = vm,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = SimpleViewModelFactory {
                    SettingsViewModel(
                        context.applicationContext,
                        container.libraryRepository,
                        container.comicRepository,
                        container.preferencesRepository,
                    )
                })
                SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.NEWS) {
                val vm: NewsViewModel = viewModel(factory = SimpleViewModelFactory {
                    NewsViewModel(container.newsRepository, container.libraryRepository, container.comicRepository)
                })
                NewsScreen(viewModel = vm, onOpenArticle = { navController.navigate(Routes.newsArticle(it)) })
            }

            composable(
                route = Routes.NEWS_ARTICLE,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) { entry: NavBackStackEntry ->
                val articleId = entry.arguments?.getLong("articleId") ?: -1L
                val vm: NewsArticleDetailsViewModel = viewModel(
                    key = "news_article_$articleId",
                    factory = SimpleViewModelFactory {
                        NewsArticleDetailsViewModel(articleId, container.newsRepository)
                    }
                )
                NewsArticleDetailsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
            ) { entry: NavBackStackEntry ->
                val mediaId = entry.arguments?.getLong("mediaId") ?: -1L
                val vm: DetailsViewModel = viewModel(
                    key = "details_$mediaId",
                    factory = SimpleViewModelFactory {
                        DetailsViewModel(mediaId, container.libraryRepository, container.watchRepository, container.profileRepository, container.playbackRepository)
                    }
                )
                DetailsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { _, episodeId -> navController.navigate(Routes.player(mediaId, episodeId)) }
                )
            }

            composable(
                route = Routes.PLAYER,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.LongType },
                    navArgument("episodeId") { type = NavType.LongType },
                )
            ) { entry: NavBackStackEntry ->
                val playerMediaId = entry.arguments?.getLong("mediaId") ?: -1L
                val rawEpisodeId = entry.arguments?.getLong("episodeId") ?: -1L
                val playerEpisodeId = rawEpisodeId.takeIf { it != -1L }
                val vm: PlayerViewModel = viewModel(
                    key = "player_${playerMediaId}_${playerEpisodeId}",
                    factory = SimpleViewModelFactory {
                        PlayerViewModel(
                            playerMediaId,
                            playerEpisodeId,
                            context.applicationContext,
                            container.libraryRepository,
                            container.playbackRepository,
                        )
                    }
                )
                PlayerScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.COMIC_DETAILS,
                arguments = listOf(navArgument("comicId") { type = NavType.LongType })
            ) { entry: NavBackStackEntry ->
                val comicId = entry.arguments?.getLong("comicId") ?: -1L
                val vm: ComicDetailsViewModel = viewModel(
                    key = "comic_details_$comicId",
                    factory = SimpleViewModelFactory {
                        ComicDetailsViewModel(comicId, container.comicRepository)
                    }
                )
                ComicDetailsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpen = { _ -> navController.navigate(Routes.comicReader(comicId)) }
                )
            }

            composable(
                route = Routes.COMIC_READER,
                arguments = listOf(navArgument("comicId") { type = NavType.LongType })
            ) { entry: NavBackStackEntry ->
                val readerComicId = entry.arguments?.getLong("comicId") ?: -1L
                val vm: ComicReaderViewModel = viewModel(
                    key = "comic_reader_$readerComicId",
                    factory = SimpleViewModelFactory {
                        ComicReaderViewModel(readerComicId, context.applicationContext, container.comicRepository)
                    }
                )
                ComicReaderScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenExternally = {
                        coroutineScope.launch {
                            val comic = container.comicRepository.getById(readerComicId)
                            comic?.let { openComic(it.localFileUri) }
                        }
                    }
                )
            }
        }
    }
}
