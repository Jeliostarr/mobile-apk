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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.repository.SportsRepository
import com.example.repository.YocinemaRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomTab
import com.example.ui.components.LoadingScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CastDetailScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SportsMatchDetailScreen
import com.example.ui.screens.SportsPlayerScreen
import com.example.ui.screens.SportsScreen
import com.example.ui.screens.VJCatalogueScreen
import com.example.ui.screens.VJListScreen
import com.example.ui.theme.YoBaseBackground
import kotlinx.coroutines.delay

sealed class Screen {
    object Home : Screen()
    data class Explore(
        val title: String? = null,
        val sort: String? = null,
        val type: String? = null,
        val genre: String? = null
    ) : Screen()
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

    // ── Sports ──
    object SportsHome : Screen()
    data class SportsDetail(val matchId: String) : Screen()
    data class SportsPlayer(val matchId: String, val streamId: Int? = null) : Screen()
}

@Composable
fun MainAppNav() {
    val context = LocalContext.current
    val repository = remember { YocinemaRepository(context) }
    val sportsRepository = remember { SportsRepository(repository.api) }

    val initialScreen = if (repository.isLoggedIn()) Screen.Home else Screen.Login
    val screenBackStack = remember { mutableStateListOf<Screen>(initialScreen) }
    val currentScreen = screenBackStack.lastOrNull() ?: initialScreen
    var currentTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        showSplash = false
    }

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
        is Screen.Home, is Screen.Explore, is Screen.Downloads, is Screen.Account, is Screen.SportsHome -> true
        else -> false
    }

    val currentRoute = when (currentScreen) {
        is Screen.Home -> "home"
        is Screen.Explore -> "explore"
        is Screen.Downloads -> "downloads"
        is Screen.Account -> "account"
        is Screen.SportsHome -> "sports"
        else -> currentTab.route
    }

    if (showSplash) {
        LoadingScreen()
        return
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
                            BottomTab.Sports -> Screen.SportsHome
                            BottomTab.Explore -> Screen.Explore()
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
                                navigateTo(Screen.Explore(title = title, sort = sort, type = type, genre = genre))
                            }
                        )
                    }

                    is Screen.Explore -> {
                        ExploreScreen(
                            repository = repository,
                            initialCategoryTitle = screen.title,
                            initialSort = screen.sort,
                            initialType = screen.type,
                            initialGenre = screen.genre,
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
                            onBackClick = {
                                if (repository.isLoggedIn()) navigateBack()
                            },
                            onLoginSuccess = {
                                screenBackStack.clear()
                                screenBackStack.add(Screen.Home)
                                currentTab = BottomTab.Home
                            }
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

                    // ── Sports ──
                    is Screen.SportsHome -> {
                        SportsScreen(
                            sportsRepository = sportsRepository,
                            onMatchClick = { matchId -> navigateTo(Screen.SportsDetail(matchId)) }
                        )
                    }

                    is Screen.SportsDetail -> {
                        SportsMatchDetailScreen(
                            matchId = screen.matchId,
                            sportsRepository = sportsRepository,
                            onBackClick = { navigateBack() },
                            onWatch = { matchId, streamId ->
                                navigateTo(Screen.SportsPlayer(matchId = matchId, streamId = streamId))
                            }
                        )
                    }

                    is Screen.SportsPlayer -> {
                        SportsPlayerScreen(
                            matchId = screen.matchId,
                            initialStreamId = screen.streamId,
                            sportsRepository = sportsRepository,
                            onBackClick = { navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
