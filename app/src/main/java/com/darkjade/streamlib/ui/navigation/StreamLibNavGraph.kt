package com.darkjade.streamlib.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
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
import com.darkjade.streamlib.ui.screens.account.AccountScreen
import com.darkjade.streamlib.ui.screens.account.AccountViewModel
import com.darkjade.streamlib.ui.screens.browse.BrowseScreen
import com.darkjade.streamlib.ui.screens.browse.BrowseViewModel
import com.darkjade.streamlib.ui.screens.details.DetailsScreen
import com.darkjade.streamlib.ui.screens.details.DetailsViewModel
import com.darkjade.streamlib.ui.screens.home.HomeScreen
import com.darkjade.streamlib.ui.screens.home.HomeViewModel
import com.darkjade.streamlib.ui.screens.mylists.MyListsScreen
import com.darkjade.streamlib.ui.screens.mylists.MyListsViewModel
import com.darkjade.streamlib.ui.screens.search.SearchScreen
import com.darkjade.streamlib.ui.screens.search.SearchViewModel
import com.darkjade.streamlib.ui.screens.settings.SettingsScreen
import com.darkjade.streamlib.ui.screens.settings.SettingsViewModel
import com.darkjade.streamlib.ui.util.SimpleViewModelFactory

private val topLevelRoutes = setOf(Routes.HOME, Routes.MY_LISTS, Routes.BROWSE, Routes.SEARCH, Routes.ACCOUNT)

@Composable
fun StreamLibNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
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
                    HomeViewModel(container.libraryRepository, container.watchRepository, container.profileRepository)
                })
                HomeScreen(
                    viewModel = vm,
                    onOpenDetails = { navController.navigate(Routes.details(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
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
                    BrowseViewModel(container.libraryRepository)
                })
                BrowseScreen(viewModel = vm, onOpenDetails = { navController.navigate(Routes.details(it)) })
            }

            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = SimpleViewModelFactory {
                    SearchViewModel(container.libraryRepository)
                })
                SearchScreen(viewModel = vm, onOpenDetails = { navController.navigate(Routes.details(it)) })
            }

            composable(Routes.ACCOUNT) {
                val vm: AccountViewModel = viewModel(factory = SimpleViewModelFactory {
                    AccountViewModel(container.profileRepository)
                })
                AccountScreen(viewModel = vm, onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = SimpleViewModelFactory {
                    SettingsViewModel(context.applicationContext, container.libraryRepository)
                })
                SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
            ) { entry: NavBackStackEntry ->
                val mediaId = entry.arguments?.getLong("mediaId") ?: -1L
                val vm: DetailsViewModel = viewModel(
                    key = "details_$mediaId",
                    factory = SimpleViewModelFactory {
                        DetailsViewModel(mediaId, container.libraryRepository, container.watchRepository, container.profileRepository)
                    }
                )
                DetailsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { uriString, _ ->
                        val result = ExternalPlayerLauncher.play(context, Uri.parse(uriString))
                        if (result is PlaybackLaunchResult.NoPlayerFound) {
                            Toast.makeText(context, "No video player app found. Please install one.", Toast.LENGTH_LONG).show()
                        } else if (result is PlaybackLaunchResult.Failed) {
                            Toast.makeText(context, "Couldn't open file: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}
