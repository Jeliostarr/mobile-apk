package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.PendingDeepLink
import com.example.data.api.ApiKeyIssue
import com.example.data.model.AppUpdateState
import com.example.data.model.DASHBOARD_URL
import com.example.data.model.MOVIES_DEMO_NIGERIA_COUNTRY
import com.example.repository.YocinemaRepository
import com.example.ui.components.ApiKeyIssueDialog
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomTab
import com.example.ui.components.LoadingScreen
import com.example.ui.components.MoviesDemoAccessDialog
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CastDetailScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoviesDemoBrowseScreen
import com.example.ui.screens.MoviesDemoDetailScreen
import com.example.ui.screens.MoviesDemoHomeScreen
import com.example.ui.screens.MoviesDemoPlayerScreen
import com.example.ui.screens.MoviesDemoSearchScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SportsMatchDetailScreen
import com.example.ui.screens.SportsPlayerScreen
import com.example.ui.screens.SportsScreen
import com.example.ui.screens.VJCatalogueScreen
import com.example.ui.screens.VJListScreen
import com.example.ui.theme.YoBaseBackground
import kotlinx.coroutines.delay

private enum class NavDirection { FORWARD, BACKWARD, LATERAL }

sealed class Screen {
    object Home : Screen()
    data class Explore(
        val title: String? = null,
        val sort: String? = null,
        val type: String? = null,
        val genre: String? = null
    ) : Screen()
    object Downloads : Screen()
    object Library : Screen()
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

    // ── Movies-demo (non-translated / Nigerian) ──
    data class MoviesDemoBrowse(val title: String, val countryFilter: String? = null) : Screen()
    data class MoviesDemoList(
        val title: String,
        val countryFilter: String? = null,
        val genre: String? = null,
        val sort: String? = null,
        val searchQuery: String? = null
    ) : Screen()
    data class MoviesDemoSearch(val title: String, val countryFilter: String? = null) : Screen()
    data class MoviesDemoDetail(val detailPath: String) : Screen()
    data class MoviesDemoPlayer(
        val detailPath: String,
        val title: String,
        val seasonNum: Int? = null,
        val epNum: Int? = null
    ) : Screen()

    // ── Sports ──
    object SportsHome : Screen()
    data class SportsDetail(val matchId: String) : Screen()
    data class SportsPlayer(
        val matchId: String,
        val streamUrl: String,
        val title: String
    ) : Screen()
}

@Composable
fun MainAppNav() {
    val context = LocalContext.current
    val repository = remember { YocinemaRepository(context) }
    // SportsRepository is built inside YocinemaRepository alongside the
    // other Retrofit services — same okHttpClient, same AuthInterceptor,
    // same X-API-Key. Nothing to construct here.
    val sportsRepository = repository.sportsRepository

    val apiKeyIssue by repository.apiKeyIssueFlow.collectAsState()

    val initialScreen = if (repository.isLoggedIn()) Screen.Home else Screen.Login
    val screenBackStack = remember { mutableStateListOf<Screen>(initialScreen) }
    val currentScreen = screenBackStack.lastOrNull() ?: initialScreen
    var currentTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        showSplash = false
    }

    var appUpdateState by remember { mutableStateOf<AppUpdateState?>(null) }
    var updateDialogDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val versionResponse = repository.checkAppVersion() ?: return@LaunchedEffect
        appUpdateState = repository.classifyAppUpdate(
            currentVersionCode = com.example.BuildConfig.VERSION_CODE,
            response = versionResponse
        )
    }

    var navDirection by remember { mutableStateOf(NavDirection.LATERAL) }

    fun navigateTo(screen: Screen) {
        navDirection = NavDirection.FORWARD
        screenBackStack.add(screen)
    }

    fun navigateBack() {
        if (screenBackStack.size > 1) {
            navDirection = NavDirection.BACKWARD
            screenBackStack.removeAt(screenBackStack.lastIndex)
        }
    }

    fun switchRoot(screen: Screen) {
        navDirection = NavDirection.LATERAL
        screenBackStack.clear()
        screenBackStack.add(screen)
    }

    BackHandler(enabled = screenBackStack.size > 1) {
        navigateBack()
    }

    LaunchedEffect(PendingDeepLink.movieId) {
        val movieId = PendingDeepLink.movieId
        if (!movieId.isNullOrBlank()) {
            navigateTo(Screen.Detail(movieId))
            PendingDeepLink.movieId = null
        }
    }

    val showBottomNav = when (currentScreen) {
        is Screen.Home,
        is Screen.Explore,
        is Screen.Downloads,
        is Screen.Library,
        is Screen.Account,
        is Screen.SportsHome -> true
        else -> false
    }

    val currentRoute = when (currentScreen) {
        is Screen.Home -> "home"
        is Screen.Explore -> "explore"
        is Screen.Downloads -> "downloads"
        is Screen.Library -> "library"
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
                            BottomTab.Library -> Screen.Library
                            BottomTab.Account -> Screen.Account
                        }
                        if (currentScreen != targetScreen) {
                            switchRoot(targetScreen)
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
                transitionSpec = {
                    when (navDirection) {
                        NavDirection.FORWARD -> (
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) { fullWidth -> fullWidth } + fadeIn(tween(220))
                        ) togetherWith (
                            fadeOut(tween(180)) + slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) { fullWidth -> -fullWidth / 4 }
                        )

                        NavDirection.BACKWARD -> (
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) { fullWidth -> -fullWidth / 4 } + fadeIn(tween(220))
                        ) togetherWith (
                            fadeOut(tween(180)) + slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) { fullWidth -> fullWidth }
                        )

                        NavDirection.LATERAL ->
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    }
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(
                            repository = repository,
                            sportsRepository = sportsRepository,
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) },
                            onSearchClick = { navigateTo(Screen.Search()) },
                            onWatchlistClick = { navigateTo(Screen.Library) },
                            onDownloadsClick = {
                                currentTab = BottomTab.Downloads
                                navigateTo(Screen.Downloads)
                            },
                            onAccountClick = {
                                currentTab = BottomTab.Account
                                navigateTo(Screen.Account)
                            },
                            onNonTranslatedClick = {
                                navigateTo(Screen.MoviesDemoBrowse(title = "Non-Translated Movies"))
                            },
                            onNigerianClick = {
                                navigateTo(
                                    Screen.MoviesDemoBrowse(
                                        title = "Nigerian Movies",
                                        countryFilter = MOVIES_DEMO_NIGERIA_COUNTRY
                                    )
                                )
                            },
                            onViewAllVJsClick = { navigateTo(Screen.VJList) },
                            onVJClick = { vjName -> navigateTo(Screen.VJCatalogue(vjName)) },
                            onViewAllCategoryClick = { title, sort, type, genre ->
                                navigateTo(Screen.Explore(title = title, sort = sort, type = type, genre = genre))
                            },
                            onMatchClick = { matchId -> navigateTo(Screen.SportsDetail(matchId)) },
                            onViewAllSportsClick = {
                                currentTab = BottomTab.Sports
                                navigateTo(Screen.SportsHome)
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
                            onLibraryClick = { navigateTo(Screen.Library) }
                        )
                    }

                    is Screen.Library -> {
                        LibraryScreen(
                            repository = repository,
                            onMovieClick = { movieId -> navigateTo(Screen.Detail(movieId)) },
                            onPlayHistoryClick = { movieId, seasonNum, epNum, _ ->
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
                                switchRoot(Screen.Home)
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

                    is Screen.MoviesDemoBrowse -> {
                        MoviesDemoHomeScreen(
                            title = screen.title,
                            countryFilter = screen.countryFilter,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onItemClick = { detailPath -> navigateTo(Screen.MoviesDemoDetail(detailPath)) },
                            onSearchClick = {
                                navigateTo(
                                    Screen.MoviesDemoSearch(
                                        title = screen.title,
                                        countryFilter = screen.countryFilter
                                    )
                                )
                            },
                            onViewAllClick = { railTitle, genre, sort ->
                                navigateTo(
                                    Screen.MoviesDemoList(
                                        title = railTitle,
                                        countryFilter = screen.countryFilter,
                                        genre = genre,
                                        sort = sort
                                    )
                                )
                            }
                        )
                    }

                    is Screen.MoviesDemoList -> {
                        MoviesDemoBrowseScreen(
                            title = screen.title,
                            countryFilter = screen.countryFilter,
                            initialGenre = screen.genre,
                            initialSort = screen.sort,
                            searchQuery = screen.searchQuery,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onItemClick = { detailPath -> navigateTo(Screen.MoviesDemoDetail(detailPath)) }
                        )
                    }

                    is Screen.MoviesDemoSearch -> {
                        MoviesDemoSearchScreen(
                            title = screen.title,
                            countryFilter = screen.countryFilter,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onItemClick = { detailPath -> navigateTo(Screen.MoviesDemoDetail(detailPath)) }
                        )
                    }

                    is Screen.MoviesDemoDetail -> {
                        MoviesDemoDetailScreen(
                            detailPath = screen.detailPath,
                            repository = repository,
                            onBackClick = { navigateBack() },
                            onPlayClick = { path, title, seasonNum, epNum ->
                                navigateTo(
                                    Screen.MoviesDemoPlayer(
                                        detailPath = path,
                                        title = title,
                                        seasonNum = seasonNum,
                                        epNum = epNum
                                    )
                                )
                            }
                        )
                    }

                    is Screen.MoviesDemoPlayer -> {
                        MoviesDemoPlayerScreen(
                            detailPath = screen.detailPath,
                            title = screen.title,
                            seasonNum = screen.seasonNum,
                            epNum = screen.epNum,
                            repository = repository,
                            onBackClick = { navigateBack() }
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
                            onWatch = { matchId, streamUrl ->
                                navigateTo(
                                    Screen.SportsPlayer(
                                        matchId = matchId,
                                        streamUrl = streamUrl,
                                        title = ""
                                    )
                                )
                            }
                        )
                    }

                    is Screen.SportsPlayer -> {
                        SportsPlayerScreen(
                            matchId = screen.matchId,
                            streamUrl = screen.streamUrl,
                            title = screen.title,
                            repository = repository,   
                            onBackClick = { navigateBack() }
                        )
                    }
                }
            }
        }
    }

    apiKeyIssue?.let { issue ->
        if (issue is ApiKeyIssue.MoviesDemoNotEnabled) {
            MoviesDemoAccessDialog(onDismiss = { repository.clearApiKeyIssue() })
        } else {
            ApiKeyIssueDialog(
                issue = issue,
                onSwitchKey = {
                    repository.clearApiKeyIssue()
                    currentTab = BottomTab.Account
                    if (currentScreen !is Screen.Account) {
                        switchRoot(Screen.Account)
                    }
                },
                onPurchase = {
                    repository.clearApiKeyIssue()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DASHBOARD_URL)))
                },
                onDismiss = { repository.clearApiKeyIssue() }
            )
        }
    }

    when (val state = appUpdateState) {
        is AppUpdateState.Required -> {
            UpdateDialog(
                versionName = state.versionName,
                releaseNotes = state.releaseNotes,
                apkUrl = state.apkUrl,
                isForced = true,
                onDismiss = {}
            )
        }
        is AppUpdateState.Optional -> {
            if (apiKeyIssue == null && !updateDialogDismissed) {
                UpdateDialog(
                    versionName = state.versionName,
                    releaseNotes = state.releaseNotes,
                    apkUrl = state.apkUrl,
                    isForced = false,
                    onDismiss = { updateDialogDismissed = true }
                )
            }
        }
        else -> {}
    }
}