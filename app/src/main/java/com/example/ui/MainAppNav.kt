package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.repository.YocinemaRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomTab
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CastDetailScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VJCatalogueScreen
import com.example.ui.screens.VJListScreen
import com.example.ui.theme.YoBaseBackground

sealed class Screen {
    object Home : Screen()
    object Explore : Screen()
    object Downloads : Screen()
    object Account : Screen()
    object Login : Screen()
    object VJList : Screen()
    data class Search(val initialQuery: String = "") : Screen()
    data class Detail(val movieId: String) : Screen()
    data class Player(
        val movieId: String,
        val seasonNum: Int? = null,
        val epNum: Int? = null,
        val localFilePath: String? = null
    ) : Screen()
    data class VJCatalogue(val vjName: String) : Screen()
    data class CastDetail(val castId: String) : Screen()
}

@Composable
fun MainAppNav() {
    val context = LocalContext.current
    val repository = remember { YocinemaRepository(context) }

    val screenBackStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = screenBackStack.lastOrNull() ?: Screen.Home
    var currentTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }

    fun navigateTo(screen: Screen) {
        screenBackStack.add(screen)
    }

    fun navigateBack() {
        if (screenBackStack.size > 1) {
            screenBackStack.removeAt(screenBackStack.lastIndex)
        }
    }

    BackHandler(enabled = screenBackStack.size > 1) {
        navigateBack()
    }

    val showBottomNav = when (currentScreen) {
        is Screen.Home, is Screen.Explore, is Screen.Downloads, is Screen.Account -> true
        else -> false
    }

    val currentRoute = when (currentScreen) {
        is Screen.Home -> "home"
        is Screen.Explore -> "explore"
        is Screen.Downloads -> "downloads"
        is Screen.Account -> "account"
        else -> currentTab.route
    }

    Scaffold(
        containerColor = YoBaseBackground,
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        currentTab = tab
                        val targetScreen = when (tab) {
                            BottomTab.Home -> Screen.Home
                            BottomTab.Explore -> Screen.Explore
                            BottomTab.Downloads -> Screen.Downloads
                            BottomTab.Account -> Screen.Account
                        }
                        if (currentScreen != targetScreen) {
                            screenBackStack.clear()
                            screenBackStack.add(targetScreen)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(
                            repository = repository,
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) },
                            onSearchClick = { navigateTo(Screen.Search()) },
                            onWatchlistClick = {
                                currentTab = BottomTab.Account
                                navigateTo(Screen.Account)
                            },
                            onDownloadsClick = {
                                currentTab = BottomTab.Downloads
                                navigateTo(Screen.Downloads)
                            },
                            onAccountClick = {
                                currentTab = BottomTab.Account
                                navigateTo(Screen.Account)
                            },
                            onViewAllVJsClick = { navigateTo(Screen.VJList) },
                            onVJClick = { vjName -> navigateTo(Screen.VJCatalogue(vjName)) },
                            onViewAllCategoryClick = { title, sort, type, genre ->
                                navigateTo(Screen.Explore)
                            }
                        )
                    }

                    is Screen.Explore -> {
                        ExploreScreen(
                            repository = repository,
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) }
                        )
                    }

                    is Screen.Downloads -> {
                        DownloadsScreen(
                            repository = repository,
                            onPlayOfflineFile = { movieId, filePath ->
                                navigateTo(Screen.Player(movieId = movieId, localFilePath = filePath))
                            }
                        )
                    }

                    is Screen.Account -> {
                        AccountScreen(
                            repository = repository,
                            onLoginClick = { navigateTo(Screen.Login) },
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) },
                            onPlayHistoryClick = { movieId, seasonNum, epNum, posMs ->
                                navigateTo(
                                    Screen.Player(
                                        movieId = movieId,
                                        seasonNum = seasonNum,
                                        epNum = epNum
                                    )
                                )
                            }
                        )
                    }

                    is Screen.Login -> {
                        LoginScreen(
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onLoginSuccess = { navigateBack() }
                        )
                    }

                    is Screen.Search -> {
                        SearchScreen(
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) }
                        )
                    }

                    is Screen.Detail -> {
                        DetailScreen(
                            movieId = screen.movieId,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onPlayClick = { movieId, seasonNum, epNum ->
                                navigateTo(
                                    Screen.Player(
                                        movieId = movieId,
                                        seasonNum = seasonNum,
                                        epNum = epNum
                                    )
                                )
                            },
                            onCastClick = { castId -> navigateTo(Screen.CastDetail(castId)) },
                            onRelatedMovieClick = { id -> navigateTo(Screen.Detail(id)) },
                            onEnterApiKeyRequested = { navigateTo(Screen.Login) }
                        )
                    }

                    is Screen.Player -> {
                        PlayerScreen(
                            movieId = screen.movieId,
                            seasonNum = screen.seasonNum,
                            epNum = screen.epNum,
                            localFilePath = screen.localFilePath,
                            repository = repository,
                            onBackClick = { navigateBack() }
                        )
                    }

                    is Screen.VJList -> {
                        VJListScreen(
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onVJClick = { vjName -> navigateTo(Screen.VJCatalogue(vjName)) }
                        )
                    }

                    is Screen.VJCatalogue -> {
                        VJCatalogueScreen(
                            vjName = screen.vjName,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) }
                        )
                    }

                    is Screen.CastDetail -> {
                        CastDetailScreen(
                            castId = screen.castId,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) }
                        )
                    }
                }
            }
        }
    }
}
