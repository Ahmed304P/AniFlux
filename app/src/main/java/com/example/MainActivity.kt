package com.example
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically


import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AnimeSlayerDrawerContent
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.*
import com.example.ui.screens.SeasonsScreen
import com.example.ui.screens.RankingScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.PopularCharactersScreen
import com.example.ui.screens.PlaceholderScreen
import com.example.ui.theme.AniFluxCyan
import com.example.ui.theme.AniFluxTheme
import com.example.ui.viewmodel.AnimeViewModel
import com.example.ui.viewmodel.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AnimeViewModel = viewModel()
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val networkMonitor = remember { com.example.util.NetworkMonitor(context) }
            val isOnline by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

            AniFluxTheme(appTheme = currentTheme) {
                if (!isOnline) {
                    com.example.ui.screens.NoInternetScreen(onRetry = {
                        // The NetworkMonitor will automatically detect when it's back online
                    })
                } else {
                    AniFluxAppShell()
                }


            }
        }
    }
}

@Composable
fun AniFluxAppShell(
    viewModel: AnimeViewModel = viewModel(),
    socialViewModel: com.example.ui.viewmodel.SocialViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allAnime by viewModel.allAnime.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val filteredAnimeList by viewModel.filteredAnimeList.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
                val isGridMode by viewModel.isGridMode.collectAsStateWithLifecycle()
    val overlayStack by viewModel.overlayStack.collectAsStateWithLifecycle()
    val isSearchOverlayVisible = overlayStack.any { it is com.example.ui.viewmodel.AnimeViewModel.OverlayState.Search }
    
    val selectedAnimeDetails by viewModel.selectedAnimeForDetails.collectAsStateWithLifecycle()
    val activePlayerPair by viewModel.activePlayerEpisode.collectAsStateWithLifecycle()
    val savedEntities by viewModel.savedAnimeEntities.collectAsStateWithLifecycle()
    val watchHistoryList by viewModel.watchHistory.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val selectedScheduleDay by viewModel.selectedScheduleDay.collectAsStateWithLifecycle()
    val scheduledAnimeList by viewModel.scheduledAnimeList.collectAsStateWithLifecycle()
    val selectedLibraryCategory by viewModel.selectedLibraryCategory.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val skipTime by viewModel.skipTime.collectAsStateWithLifecycle()


    val tabHistorySize by viewModel.tabHistorySize.collectAsStateWithLifecycle()
    val shouldOpenNotifications by socialViewModel.shouldOpenNotifications.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = true) {
        if (activePlayerPair != null) {
            viewModel.closePlayer()
        } else if (viewModel.popOverlay()) {
            // overlay stack handled the back press
        } else if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (tabHistorySize > 0) {
            viewModel.popTabStack()
        } else if (selectedTab != 0) {
            viewModel.setSelectedTab(0, addToHistory = false)
        } else {
            showExitDialog = true
        }
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val userBanner by viewModel.userBanner.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedAnimeDetails == null && activePlayerPair == null,
        drawerContent = {

            AnimeSlayerDrawerContent(
                selectedTab = selectedTab,
                isLoggedIn = isLoggedIn,
                userName = userName,
                userAvatar = userAvatar,
                userBanner = userBanner,
                onSelectTab = { tabIndex -> viewModel.setSelectedTab(tabIndex) },
                onCloseDrawer = { scope.launch { drawerState.close() } },
                socialViewModel = socialViewModel,
                onNotificationClicked = {
                    if (allAnime.isNotEmpty() && selectedAnimeDetails == null) {
                        viewModel.openAnimeDetails(allAnime.first())
                    }
                    socialViewModel.setShouldOpenComments(true)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Navigation Screen Selection
                androidx.compose.animation.AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                    0 -> HomeScreen(
                        allAnime = allAnime,
                        isLoading = isLoading,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenSearch = { viewModel.openSearch() },
                        onOpenAnimeDetails = { viewModel.openAnimeDetails(it) },
                        isGridMode = isGridMode,
                        onToggleGridMode = { viewModel.toggleGridMode() }
                    )

                    1 -> ExploreScreen(
                        filteredAnimeList = filteredAnimeList,
                        filterState = filterState,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onGenreFilterChange = { viewModel.updateGenreFilter(it) },
                        onStatusFilterChange = { viewModel.updateStatusFilter(it) },
                        onTypeFilterChange = { viewModel.updateTypeFilter(it) },
                        onResetFilters = { viewModel.resetFilters() },
                        onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenSearch = { viewModel.openSearch() },
                        title = "لائحة الانمي",
                        isGridMode = isGridMode,
                        onToggleGridMode = { viewModel.toggleGridMode() }
                    )

                    2 -> ScheduleScreen(
                        selectedDay = selectedScheduleDay,
                        scheduledAnimeList = scheduledAnimeList,
                        onSelectDay = { viewModel.setScheduleDay(it) },
                        onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                        onOpenMenu = { scope.launch { drawerState.open() } }
                    )

                    12 -> {
                        LibraryScreen(
                            selectedCategory = selectedLibraryCategory,
                            savedEntities = savedEntities,
                            watchHistoryList = watchHistoryList,
                            allAnimeList = allAnime,
                            onSelectCategory = { viewModel.setLibraryCategory(it) },
                            onRemoveSaved = { viewModel.removeSavedAnime(it) },
                            onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            title = "تحميلاتي"
                        )
                    }
                    3 -> {
                        com.example.ui.screens.MyListScreen(
                            savedEntities = savedEntities,
                            allAnimeList = allAnime,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onOpenSearch = { viewModel.openSearch() },
                            onOpenAnimeDetails = { viewModel.openAnimeDetails(it) }
                        )
                    }
                    8 -> {
                        var customListScreenState by remember { mutableStateOf("MAIN") }
                        var selectedList by remember { mutableStateOf<com.example.data.local.CustomListEntity?>(null) }
                        
                        val customLists by viewModel.customLists.collectAsStateWithLifecycle()
                        val currentListItems by viewModel.currentCustomListItems.collectAsStateWithLifecycle()
                        
                        when (customListScreenState) {
                            "MAIN" -> {
                                com.example.ui.screens.CustomListsScreen(
                                    customLists = customLists,
                                    onOpenMenu = { scope.launch { drawerState.open() } },
                                    onOpenSearch = { viewModel.openSearch() },
                                    onCreateListClick = { customListScreenState = "CREATE" },
                                    onListClick = { list -> 
                                        selectedList = list
                                        viewModel.loadCustomListItems(list.id)
                                        customListScreenState = "DETAIL"
                                    }
                                )
                            }
                            "CREATE" -> {
                                com.example.ui.screens.CreateListScreen(
                                    onBack = { customListScreenState = "MAIN" },
                                    onCreate = { name, description -> 
                                        viewModel.createCustomList(name, description)
                                        customListScreenState = "MAIN"
                                    }
                                )
                            }
                            "DETAIL" -> {
                                if (selectedList != null) {
                                    com.example.ui.screens.CustomListDetailScreen(
                                        list = selectedList!!,
                                        listItems = currentListItems,
                                        allAnimeList = allAnime,
                                        onBack = { customListScreenState = "MAIN" },
                                        onAddAnimeClick = { customListScreenState = "ADD" },
                                        onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                                        isGridMode = isGridMode,
                                        onDeleteList = {
                                            viewModel.deleteCustomList(selectedList!!.id)
                                            customListScreenState = "MAIN"
                                        },
                                        onRemoveAnime = { animeId ->
                                            viewModel.removeAnimeFromCustomList(selectedList!!.id, animeId)
                                        }
                                    )
                                }
                            }
                            "ADD" -> {
                                if (selectedList != null) {
                                    com.example.ui.screens.AddAnimeToListScreen(
                                        allAnimeList = allAnime,
                                        currentAnimeIds = currentListItems.map { it.animeId },
                                        onBack = { customListScreenState = "DETAIL" },
                                        onAdd = { animeId -> viewModel.addAnimeToCustomList(selectedList!!.id, animeId) },
                                        onRemove = { animeId -> viewModel.removeAnimeFromCustomList(selectedList!!.id, animeId) }
                                    )
                                }
                            }
                        }
                    }
                    9 -> {
                        com.example.ui.screens.FavoritesScreen(
                            savedEntities = savedEntities,
                            allAnimeList = allAnime,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onOpenSearch = { viewModel.openSearch() },
                            onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                            onRemoveFromFavorites = { viewModel.removeSavedAnime(it) },
                            isGridMode = isGridMode
                        )
                    }
                    11 -> {
                        com.example.ui.screens.WatchHistoryScreen(
                            watchHistoryList = watchHistoryList,
                            allAnimeList = allAnime,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onOpenSearch = { viewModel.openSearch() },
                            onOpenAnimeDetails = { 
                        viewModel.updateSearchQuery("")
                        viewModel.openAnimeDetails(it)
                    },
                            onRemoveFromHistory = { viewModel.removeWatchHistory(it) },
                            isGridMode = isGridMode
                        )
                    }


                    5 -> SeasonsScreen(
                        animeList = filteredAnimeList,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenSearch = { viewModel.openSearch() },
                        onOpenAnimeDetails = { viewModel.openAnimeDetails(it) },
                        isGridMode = isGridMode
                    )

                    14 -> com.example.ui.screens.SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeSelected = { viewModel.setTheme(it) },
                        currentSkipTime = skipTime,
                        onSkipTimeSelected = { viewModel.setSkipTime(it) },
                        onOpenMenu = { scope.launch { drawerState.open() } }
                    )

                    15 -> ProfileScreen(
                        onBackClick = { viewModel.setSelectedTab(0, addToHistory = false) },
                        onEditClick = { viewModel.setSelectedTab(16) }
                    )

                    16 -> EditProfileScreen(
                        onBackClick = { viewModel.setSelectedTab(15) }
                    )
                    
                    17 -> LoginScreen(
                        onBack = { viewModel.setSelectedTab(0, addToHistory = false) },
                        onLoginSuccess = { viewModel.setSelectedTab(18) },
                        onAdminBackdoor = { viewModel.setSelectedTab(19) }
                    )
                    18 -> CompleteProfileScreen(
                        onComplete = { name, avatar, banner ->
                            viewModel.completeLogin(name, avatar, banner)
                            viewModel.setSelectedTab(0, addToHistory = false)
                        }
                    )
                    19 -> AdminPasswordScreen(
                        onBack = { viewModel.popTabStack() },
                        onSuccess = { viewModel.setSelectedTab(20) }
                    )
                    20 -> MasterAdminDashboardScreen(
                        onBack = { viewModel.popTabStack() },
                        onNavigateToAnimeManager = { viewModel.setSelectedTab(21) }
                    )
                    21 -> AdminAnimeManagerScreen(
                        onBack = { viewModel.popTabStack() },
                        onEditAnime = { id -> 
                            viewModel.adminEditingAnimeId = id
                            viewModel.setSelectedTab(22) 
                        },
                        viewModel = viewModel
                    )
                    22 -> AdminAnimeEditScreen(viewModel, 
                        animeId = viewModel.adminEditingAnimeId,
                        onBack = { viewModel.popTabStack() },
                        onManageEpisodes = { id -> viewModel.setSelectedTab(23) }
                    )
                    23 -> AdminEpisodeManagerScreen(
                        animeId = viewModel.adminEditingAnimeId ?: "",
                        onBack = { viewModel.popTabStack() },
                        viewModel = viewModel
                    )
                }

                SearchScreenOverlay(
                    visible = isSearchOverlayVisible,
                    isDetailScreenOpen = selectedAnimeDetails != null,
                    onClose = { 
                        viewModel.popOverlay()
                        viewModel.updateSearchQuery("")
                    },
                    searchQuery = filterState.query,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    searchResults = filteredAnimeList,
                    onOpenAnimeDetails = { viewModel.openAnimeDetails(it) },
                    allAnime = allAnime,
                    filterState = filterState,
                    onGenreFilterChange = { viewModel.updateGenreFilter(it) },
                    onStatusFilterChange = { viewModel.updateStatusFilter(it) },
                    onTypeFilterChange = { viewModel.updateTypeFilter(it) },
                    onYearFilterChange = { viewModel.updateYearFilter(it) },
                    onStudioFilterChange = { viewModel.updateStudioFilter(it) },
                    onAgeRatingFilterChange = { viewModel.updateAgeRatingFilter(it) },
                    onSeasonFilterChange = { viewModel.updateSeasonFilter(it) },
                    onResetFilters = { viewModel.resetFilters() },
                    isGridMode = isGridMode
                )


                // Notifications Screen Overlay
                AnimatedVisibility(
                    visible = shouldOpenNotifications,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                ) {
                    com.example.ui.screens.NotificationsScreen(
                        viewModel = socialViewModel,
                        onBack = { socialViewModel.setShouldOpenNotifications(false) },
                        onNotificationClicked = { notif ->
                            socialViewModel.setShouldOpenNotifications(false)
                            socialViewModel.markNotificationsAsRead()
                            socialViewModel.setTargetScrollComment(notif.targetCommentId, notif.parentThreadId)
                            if (allAnime.isNotEmpty() && selectedAnimeDetails == null) {
                                viewModel.openAnimeDetails(allAnime.first())
                            }
                            socialViewModel.setShouldOpenComments(true)
                        }
                    )
                }

                // Anime Detail Screen Overlay
                AnimatedVisibility(
                    visible = selectedAnimeDetails != null,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                ) {
                    var lastAnime by remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.Anime?>(null) }
                    androidx.compose.runtime.LaunchedEffect(selectedAnimeDetails) {
                        if (selectedAnimeDetails != null) {
                            lastAnime = selectedAnimeDetails
                        }
                    }
                    val currentAnime = selectedAnimeDetails ?: lastAnime
                    if (currentAnime != null) {
                        val anime = currentAnime
                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }
                        val isSaved by viewModel.isAnimeSaved(anime.id).collectAsStateWithLifecycle(initialValue = false)
                        val savedStatus by viewModel.getAnimeSavedStatus(anime.id).collectAsStateWithLifecycle(initialValue = null)
                        val userRating by viewModel.getUserRating(anime.id).collectAsStateWithLifecycle(initialValue = null)
                        val watchedEpisodes by viewModel.getWatchedEpisodes(anime.id).collectAsStateWithLifecycle(initialValue = emptySet())
                        AnimeDetailScreen(
                            anime = anime,
                            episodes = episodes,
                            isSaved = isSaved,
                            savedStatus = savedStatus,
                            userRating = userRating,
                            watchedEpisodes = watchedEpisodes,
                            onToggleWatched = { epNum, watched -> viewModel.toggleEpisodeWatched(anime.id, epNum, watched) },
                            onRateAnime = { rating -> viewModel.rateAnime(anime.id, rating) },
                            onBack = { viewModel.closeAnimeDetails() },
                            onToggleSave = { status -> viewModel.toggleSaveAnime(anime.id, status) },
                            onRemoveSaved = { animeId -> viewModel.removeSavedAnime(animeId) },
                            onGenreClick = { genre -> viewModel.openSearchWithGenre(genre) },
                            onStudioClick = { studio -> viewModel.openSearchWithStudio(studio) },
                            onOpenPlayer = { ep, server ->
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, server)
                            }
                        )
                    }
                }

                // Video Player Modal Overlay
                AnimatedVisibility(
                    visible = activePlayerPair != null,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                ) {
                    var lastPlayerPair by remember { androidx.compose.runtime.mutableStateOf<Pair<com.example.data.model.Anime, com.example.data.model.Episode>?>(null) }
                    androidx.compose.runtime.LaunchedEffect(activePlayerPair) {
                        if (activePlayerPair != null) {
                            lastPlayerPair = activePlayerPair
                        }
                    }
                    val currentPair = activePlayerPair ?: lastPlayerPair

                    if (currentPair != null) {
                        val (anime, activeEp) = currentPair
                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }

                        val currentSkipTime by viewModel.skipTime.collectAsStateWithLifecycle()
                        val activeServer by viewModel.activePlayerServer.collectAsStateWithLifecycle()
                        VideoPlayerModal(
                            skipTime = currentSkipTime,
                            initialServer = activeServer,
                            anime = anime,
                            currentEpisode = activeEp,
                            allEpisodes = episodes,
                            comments = activeComments,
                            onClose = { viewModel.closePlayer() },
                            onSelectEpisode = { ep, autoServer ->
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, autoServer)
                            },
                            onAddComment = { content ->
                                viewModel.addComment(anime.id, activeEp.episodeNumber, "متابع AniFlux", content)
                            },
                            onLikeComment = { commentId ->
                                viewModel.likeComment(commentId)
                            },
                            onRecordProgress = { progressSec, totalSec ->
                                viewModel.recordWatchProgress(
                                    anime.id,
                                    activeEp.episodeNumber,
                                    activeEp.titleAr,
                                    progressSec,
                                    totalSec
                                )
                            }
                        )
                    }
                }
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("تنبيه", color = Color.White) },
                        text = { Text("هل تريد الخروج من التطبيق؟", color = Color.LightGray) },
                        confirmButton = {
                            TextButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                                Text("نعم", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) {
                                Text("لا", color = Color.Gray)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = Color.White,
                        textContentColor = Color.LightGray
                    )
                }


                


            }
        }
    }
}
}
