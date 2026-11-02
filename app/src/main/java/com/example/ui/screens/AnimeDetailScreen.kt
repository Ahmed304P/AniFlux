package com.example.ui.screens
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SocialViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.StarHalf
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.util.shimmerEffect
import coil.request.ImageRequest
import com.example.data.model.Anime
import com.example.data.model.Episode
import com.example.util.RelatedAnime
import com.example.util.AniListService
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    allAnime: List<Anime>,
    onRelatedAnimeClick: (com.example.util.RelatedAnime) -> Unit,
    onRecommendationClick: (Anime) -> Unit,
    anime: Anime,
    episodes: List<Episode>,
    isSaved: Boolean,
    savedStatus: String?,
    userRating: Int?,
    watchedEpisodes: Set<Int>,
    onToggleWatched: (Int, Boolean) -> Unit,
    onRateAnime: (Int) -> Unit = {},
    onBack: () -> Unit,
    onToggleSave: (String) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onOpenPlayer: (Episode, com.example.data.model.StreamServer?) -> Unit,
    onGenreClick: (String) -> Unit = {},
    onStudioClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val detailsListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val socialViewModel: SocialViewModel = viewModel()
    val animeViewModel: com.example.ui.viewmodel.AnimeViewModel = viewModel()
    val showComments by socialViewModel.shouldOpenComments.collectAsState()
    val enabledExtensions by animeViewModel.enabledExtensions.collectAsState()

    // Auto-heal genres if empty (pulls verified standard genres automatically)
    LaunchedEffect(anime.id) {
        if (anime.genres.isEmpty() && (anime.titleEn.isNotBlank() || anime.titleAr.isNotBlank())) {
            try {
                val query = anime.titleEn.ifBlank { anime.titleAr }
                val results = com.example.util.AniListService.searchAnime(query)
                val match = results.firstOrNull { 
                    it.genres.isNotBlank() && (
                        it.titleEn.equals(anime.titleEn, ignoreCase = true) || 
                        it.titleAr.equals(anime.titleAr, ignoreCase = true) ||
                        it.titleEn.contains(anime.titleEn, ignoreCase = true)
                    )
                } ?: results.firstOrNull { it.genres.isNotBlank() }

                if (match != null && match.genres.isNotBlank()) {
                    val rawParsed = match.genres.split(Regex("[,،]")).map { it.trim() }.filter { it.isNotBlank() }
                    val cleaned = com.example.util.AniListService.cleanAndFilterGenres(rawParsed)
                    val resolved = if (cleaned.isNotEmpty()) cleaned else rawParsed
                    if (resolved.isNotEmpty()) {
                        animeViewModel.saveAnime(anime.copy(genres = resolved))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var showMyListSheet by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    val tabs = listOf("التفاصيل", "الحلقات", "الاحصائيات", "الشخصيات والطاقم")
    val snackbarHostState = remember { SnackbarHostState() }
    var episodeActionSheet by remember { mutableStateOf<Episode?>(null) }
    var serverActionSheet by remember { mutableStateOf<Pair<Episode, com.example.data.model.StreamServer>?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                title = {
                    Column {
                        Text(
                            text = anime.primaryTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (anime.titleAr.isNotBlank() && anime.titleAr != anime.primaryTitle) {
                            Text(
                                text = anime.titleAr,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val coroutineScope = rememberCoroutineScope()
                    IconButton(onClick = {
                        if (anime.isCommentsLocked) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("التعليقات مغلقه من الاداره")
                            }
                        } else {
                            socialViewModel.setShouldOpenComments(true) 
                        }
                    }) { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat") }
                    val isFavorite = savedStatus == "FAVORITE"
                    IconButton(onClick = { 
                        if (isFavorite) {
                            onRemoveSaved(anime.id)
                        } else {
                            onToggleSave("FAVORITE")
                        }
                    }) { 
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else Color.White
                        ) 
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color.White
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) Color.White else Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "TabContentAnimation"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> AnimeDetailsTabContent(anime = anime, isSaved = isSaved, savedStatus = savedStatus, userRating = userRating, onOpenMyListOptions = { showMyListSheet = true }, onRateClick = { showRatingDialog = true }, onGenreClick = onGenreClick, onStudioClick = onStudioClick, lazyListState = detailsListState, allAnime = allAnime, onRelatedAnimeClick = onRelatedAnimeClick, onRecommendationClick = onRecommendationClick)
                    1 -> AnimeEpisodesTabContent(episodes, watchedEpisodes, onToggleWatched, onEpisodeClick = { episodeActionSheet = it }, enabledExtensions = enabledExtensions)
                    2 -> AnimeStatsTabContent(anime, isSaved, savedStatus, userRating)
                    3 -> AnimeCharactersTabContent(anime)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showComments,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        AnimeCommentsScreen(
            viewModel = socialViewModel,
            onBack = { socialViewModel.setShouldOpenComments(false) }
        )
    }
    
    
    if (showRatingDialog) {
        var storyRating by remember { mutableFloatStateOf((userRating ?: 0).toFloat()) }
        var charactersRating by remember { mutableFloatStateOf((userRating ?: 0).toFloat()) }
        var artRating by remember { mutableFloatStateOf((userRating ?: 0).toFloat()) }
        var musicRating by remember { mutableFloatStateOf((userRating ?: 0).toFloat()) }

        val totalAverage = ((storyRating + charactersRating + artRating + musicRating) / 4f).toInt()

        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val categories = listOf(
                        "القصة" to storyRating,
                        "الشخصيات" to charactersRating,
                        "الرسم والتحريك" to artRating,
                        "الموسيقى" to musicRating
                    )
                    
                    categories.forEachIndexed { index, (name, value) ->
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                Slider(
                                    value = value,
                                    onValueChange = { newValue ->
                                        when (index) {
                                            0 -> storyRating = newValue
                                            1 -> charactersRating = newValue
                                            2 -> artRating = newValue
                                            3 -> musicRating = newValue
                                        }
                                    },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF1E88E5), 
                                        activeTrackColor = Color(0xFF1E88E5),
                                        inactiveTrackColor = Color.DarkGray,
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    ),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                // To show the thumb value exactly like video (above thumb), requires custom logic.
                                // Instead, showing a badge above the slider is harder without Custom Slider. 
                                // We'll just show the value next to the name, or in a badge in the UI if possible.
                                Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp)) {
                                    Surface(
                                        color = Color(0xFF1E88E5),
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "${value.toInt()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (i in 0..10) {
                                    Text(
                                        text = "$i",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$totalAverage/10",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onRateAnime(totalAverage)
                    showRatingDialog = false 
                }) {
                    Text("حسنا", color = Color(0xFF1E88E5), fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("إلغاء", color = Color(0xFF1E88E5), fontSize = 16.sp)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }

        if (episodeActionSheet != null) {
            val selectedEpSnapshot = episodeActionSheet!!
            val ep = episodes.find { it.episodeNumber == selectedEpSnapshot.episodeNumber } ?: selectedEpSnapshot
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { episodeActionSheet = null },
                containerColor = Color(0xFF232834),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "اختر السيرفر",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    ep.servers.forEach { server ->
                        val isExtensionServer = server.id.startsWith("ext_")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    serverActionSheet = Pair(ep, server)
                                    episodeActionSheet = null
                                }
                                .padding(vertical = 16.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isExtensionServer) Color(0xFF9C27B0).copy(alpha = 0.22f) else Color(0xFFE91E63).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isExtensionServer) Icons.Default.Extension else Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = if (isExtensionServer) Color(0xFFCE93D8) else Color(0xFFE91E63),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.nameAr,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isExtensionServer) {
                                    Text(
                                        text = "إضافة مفعّلة ومفحوصة أمنياً",
                                        color = Color(0xFFCE93D8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = Color(0xFF373E4E), thickness = 1.dp)
                    }
                }
            }
        }

        if (serverActionSheet != null) {
            val (ep, server) = serverActionSheet!!
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { serverActionSheet = null },
                containerColor = Color(0xFF232834),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "ماذا تريد أن تفعل؟",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenPlayer(ep, server)
                                serverActionSheet = null
                            }
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("مشاهدة الحلقة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF373E4E), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val originalUrl = server.streamUrl
                                val downloadUrl = if (originalUrl.contains("pixeldrain.com") && !originalUrl.contains("?download")) {
                                    "$originalUrl?download"
                                } else {
                                    originalUrl
                                }
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(downloadUrl), "video/*")
                                    intent.setPackage("com.dv.adm")
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "تطبيق ADM غير مثبت", android.widget.Toast.LENGTH_SHORT).show()
                                    try {
                                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {}
                                }
                                serverActionSheet = null
                            }
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2196F3).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF2196F3))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("تحميل عبر ADM", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showMyListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMyListSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "قائمتي",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(
                        "PLAN_TO_WATCH" to "ارغب بمشاهدتها",
                        "WATCHING" to "اشاهدها حاليا",
                        "ON_HOLD" to "اكملها لاحقا",
                        "DROPPED" to "لا ارغب بمشاهدتها",
                        "COMPLETED" to "تم مشاهدتها"
                    )
                    
                    options.forEach { (status, text) ->
                        val isSelected = savedStatus == status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        onRemoveSaved(anime.id)
                                    } else {
                                        onToggleSave(status)
                                    }
                                    showMyListSheet = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = text,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun AnimeDetailsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?, onOpenMyListOptions: () -> Unit, onRateClick: () -> Unit, onGenreClick: (String) -> Unit = {}, onStudioClick: (String) -> Unit = {}, lazyListState: androidx.compose.foundation.lazy.LazyListState, allAnime: List<Anime>, onRelatedAnimeClick: (com.example.util.RelatedAnime) -> Unit, onRecommendationClick: (Anime) -> Unit) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            AnimeDetailsHeader(anime, lazyListState)
        }
        
        item {
            AnimeActionsRow(anime = anime, isSaved = isSaved, savedStatus = savedStatus, userRating = userRating, onOpenMyListOptions = onOpenMyListOptions, onRateClick = onRateClick)
        }
        
        item {
            AnimeSynopsisSection(anime, onGenreClick)
        }
        
        item {
            AnimeInfoGrid(anime, onStudioClick)
        }
        
        item {
            ContentRatingSection(anime)
        }
        
        item {
            TrailerSection(anime)
        }

        item {
            RelatedAnimeSection(anime, allAnime, onRelatedAnimeClick)
        }
        
        item {
            RecommendationsSection(anime, allAnime, onRecommendationClick)
        }

    }
}

@Composable
fun AnimeDetailsHeader(anime: Anime, lazyListState: androidx.compose.foundation.lazy.LazyListState? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clipToBounds() // CRITICAL FIX: prevents the image from bleeding out
    ) {
        // Main Background Banner (Clear, not blurred)
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.bannerUrl.ifEmpty { anime.posterUrl })
                .crossfade(true)
                .build(),
            loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
            contentDescription = anime.titleAr,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    if (lazyListState != null && lazyListState.firstVisibleItemIndex == 0) {
                        translationY = lazyListState.firstVisibleItemScrollOffset * 0.4f
                    }
                }
        )
        
        // Netflix-style Horizontal Vignette (Dark on sides)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Vertical gradient for readability at the bottom, blending smoothly into the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 100f
                    )
                )
        )

        // Exact Layout from the Old Version Screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Right Side: Poster
            SubcomposeAsyncImage(
                model = anime.posterUrl,
                loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(115.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            )

            // Left Side: Text Column
            Column(
                modifier = Modifier.weight(1f) // Defaults to Alignment.Start (Right in RTL)
            ) {
                Text(
                    text = anime.titleAr.ifEmpty { anime.titleEn },
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 26.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(${anime.status}) ${anime.season} ${anime.year}", 
                    color = Color.LightGray, 
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${anime.currentEpisodes} | ${anime.type} حلقة | +13", 
                    color = Color.LightGray, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AnimeActionsRow(
    anime: Anime, 
    isSaved: Boolean, 
    savedStatus: String?, 
    userRating: Int?,
    onOpenMyListOptions: () -> Unit,
    onRateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rating
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Star, contentDescription = "Rating", tint = AniFluxStar, modifier = Modifier.size(32.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "${anime.rating}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "/10", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(text = "114", color = Color.Gray, fontSize = 12.sp) // Mock vote count
        }
        
        // Add Rating
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick { onRateClick() }) {
            if (userRating != null && userRating > 0) {
                Icon(Icons.Default.Star, contentDescription = "Edit Rating", tint = Color(0xFF8B0000), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "التقييم الجمعي", color = Color.Gray, fontSize = 10.sp)
                Text(text = "$userRating/10", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.StarBorder, contentDescription = "Add Rating", tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "إضافة تقييم", color = Color.White, fontSize = 14.sp)
            }
        }
        
        // My List
        val statusTextMap = mapOf(
            "PLAN_TO_WATCH" to "ارغب بمشاهدتها",
            "WATCHING" to "اشاهدها حاليا",
            "ON_HOLD" to "اكملها لاحقا",
            "DROPPED" to "لا ارغب بمشاهدتها",
            "COMPLETED" to "تم مشاهدتها"
        )
        val currentStatusText = if (isSaved && savedStatus != null) statusTextMap[savedStatus] ?: "قائمتي" else "قائمتي"
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier.bounceClick { onOpenMyListOptions() }
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add, 
                contentDescription = "My List", 
                tint = Color.White, 
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = currentStatusText, color = Color.White, fontSize = 14.sp)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AnimeSynopsisSection(anime: Anime, onGenreClick: (String) -> Unit = {}) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = anime.synopsisAr,
            color = Color.LightGray,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tags/Genres
        if (anime.genres.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                anime.genres.forEach { genre ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.35f)),
                        onClick = { onGenreClick(genre) }
                    ) {
                        Text(
                            text = genre, 
                            color = Color.White, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeInfoGrid(anime: Anime, onStudioClick: (String) -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(title = "المصدر", value = "مانجا", modifier = Modifier.weight(1f))
                InfoItem(title = "مدة الحلقة", value = "24 دقيقة", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(title = "عرض من", value = anime.year.toString(), modifier = Modifier.weight(1f))
                InfoItem(title = "الى", value = "?", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(title = "الاستوديو", value = anime.studio, isChip = true, modifier = Modifier.weight(1f), onClick = { onStudioClick(anime.studio) })
                InfoItem(title = "العنوان الانجليزي", value = anime.titleEn.ifEmpty { anime.titleAr }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun InfoItem(title: String, value: String, isChip: Boolean = false, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(text = title, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        if (isChip) {
            if (onClick != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onClick
                ) {
                    Text(
                        text = value, 
                        color = Color.White, 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = value, 
                        color = Color.White, 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ContentRatingSection(anime: Anime) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("anime_content_ratings", android.content.Context.MODE_PRIVATE)

    data class ContentRatingCategory(
        val title: String,
        val defaultOption: String,
        val baseCount: Int
    )

    val hash = anime.id.hashCode().toLong()
    val categories = remember {
        listOf(
            ContentRatingCategory("عنف ودموية", if (hash % 2L == 0L) "شديد" else "خفيف", 89 + (hash % 20).toInt()),
            ContentRatingCategory("ألفاظ نابية", if (hash % 3L == 0L) "متوسط" else "لا يوجد", 77 + (hash % 30).toInt()),
            ContentRatingCategory("مشاهد غير لائقة", if (hash % 4L == 0L) "شديد" else "لا يوجد", 162 + (hash % 40).toInt()),
            ContentRatingCategory("مشاهد مخيفة او صادمة", if (hash % 5L == 0L) "متوسط" else "شديد", 87 + (hash % 25).toInt()),
            ContentRatingCategory("مخدرات وكحول وتدخين", if (hash % 2L != 0L) "خفيف" else "لا يوجد", 72 + (hash % 30).toInt())
        )
    }

    var selectedCategory by remember { mutableStateOf<ContentRatingCategory?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun getColorForOption(option: String): Color {
        return when (option) {
            "لا يوجد" -> Color(0xFF2196F3) // Blue
            "خفيف" -> Color(0xFF4CAF50) // Green
            "متوسط" -> Color(0xFFFF9800) // Orange
            "شديد" -> Color(0xFFF44336) // Red
            else -> Color.Gray
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تصنيف المحتوى",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Right
            )

            categories.forEach { category ->
                // Read from SharedPreferences
                val votedOption = prefs.getString("${anime.id}_${category.title}_vote", null)
                val hasVoted = prefs.getBoolean("${anime.id}_${category.title}_has_voted", false)
                
                // Determine current predominant and count
                val currentOption = votedOption ?: category.defaultOption
                val currentCount = category.baseCount + if (votedOption != null) 1 else 0
                val currentColor = getColorForOption(currentOption)
                
                RatingRow(
                    title = category.title,
                    value = "$currentOption ($currentCount)",
                    color = currentColor,
                    hasVoted = hasVoted,
                    onAddClick = {
                        if (!hasVoted) {
                            selectedCategory = category
                            showBottomSheet = true
                        }
                    },
                    dummyTrigger = refreshTrigger
                )
            }
        }
    }

    if (showBottomSheet && selectedCategory != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val cat = selectedCategory!!
            val votedOption = prefs.getString("${anime.id}_${cat.title}_vote", null)
            val currentOption = votedOption ?: cat.defaultOption
            val currentCount = cat.baseCount + if (votedOption != null) 1 else 0
            val currentColor = getColorForOption(currentOption)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cat.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = currentOption,
                    color = currentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "عدد المصوتين $currentCount",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "هل يحتوي على ${cat.title}؟",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val options = listOf("لا يوجد", "خفيف", "متوسط", "شديد")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    options.forEach { option ->
                        val optColor = getColorForOption(option)
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                prefs.edit()
                                    .putBoolean("${anime.id}_${cat.title}_has_voted", true)
                                    .putString("${anime.id}_${cat.title}_vote", option)
                                    .apply()
                                refreshTrigger++ // trigger recomposition
                                showBottomSheet = false
                                android.widget.Toast.makeText(context, "تم تسجيل تقييمك بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, optColor),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = optColor,
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
                        ) {
                            Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingRow(title: String, value: String, color: Color, hasVoted: Boolean, onAddClick: () -> Unit, dummyTrigger: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !hasVoted,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rating", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier
            .size(8.dp, 24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color))
    }
}

@Composable
fun TrailerSection(anime: Anime) {
    if (anime.trailerUrl.isBlank()) return
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "العرض الدعائي",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Right
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .bounceClick {
                    try {
                        var url = anime.trailerUrl
                        if (!url.startsWith("http")) {
                            url = "https://www.youtube.com/watch?v=$url"
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore if no app handles it
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.bannerUrl.ifEmpty { anime.posterUrl })
                    .crossfade(true)
                    .build(),
                loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
            contentDescription = "Trailer",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE91E63).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Trailer",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp).padding(start = 4.dp) // Optical alignment
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeEpisodesTabContent(
    episodes: List<Episode>, 
    watchedEpisodes: Set<Int>, 
    onToggleWatched: (Int, Boolean) -> Unit, 
    onEpisodeClick: (Episode) -> Unit,
    enabledExtensions: List<com.example.data.local.AnimeExtensionEntity> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    // false = oldest first (1 -> N), true = newest first (N -> 1)
    var isNewestFirst by remember { mutableStateOf(false) }

    // Smart filtering and ordering of episodes with full Arabic numeral & text normalization
    val filteredAndSortedEpisodes = remember(episodes, searchQuery, isNewestFirst) {
        val trimmed = searchQuery.trim()
        val normalized = trimmed
            .replace('٠', '0').replace('١', '1').replace('٢', '2')
            .replace('٣', '3').replace('٤', '4').replace('٥', '5')
            .replace('٦', '6').replace('٧', '7').replace('٨', '8')
            .replace('٩', '9')
            .lowercase()
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ة', 'ه').replace('ى', 'ي')

        val queryDigits = normalized.filter { it.isDigit() }
        val queryWords = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val filtered = if (trimmed.isEmpty()) {
            episodes
        } else {
            episodes.filter { ep ->
                val epNumStr = ep.episodeNumber.toString()
                val normTitle = ep.titleAr.lowercase()
                    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                    .replace('ة', 'ه').replace('ى', 'ي')

                // 1. Direct number match (e.g. typing "5" or "٥" matches episode 5)
                val matchesNumber = queryDigits.isNotEmpty() && epNumStr == queryDigits

                // 2. Exact or substring match in normalized title
                val matchesExactQuery = normTitle.contains(normalized)

                // 3. Multi-word fuzzy match (e.g. "حلقة 3" or "الحلقة 12")
                val matchesWords = queryWords.isNotEmpty() && queryWords.all { word ->
                    val cleanWord = word.filter { it.isDigit() }
                    if (cleanWord.isNotEmpty()) {
                        epNumStr == cleanWord || epNumStr.contains(cleanWord)
                    } else {
                        normTitle.contains(word) || "الحلقه".contains(word) || "حلقه".contains(word) || word in listOf("ep", "episode")
                    }
                }

                matchesNumber || matchesExactQuery || matchesWords
            }
        }

        if (isNewestFirst) {
            filtered.sortedByDescending { it.episodeNumber }
        } else {
            filtered.sortedBy { it.episodeNumber }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Sort Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Modern, sleek smart search field matching app theme
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("episodes_search_input")
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotBlank()) AniFluxCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                placeholder = {
                    Text(
                        text = "ابحث برقم الحلقة أو العنوان...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث الحلقات",
                        tint = if (searchQuery.isNotBlank()) AniFluxCyan else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AniFluxCyan
                )
            )

            // Attached, neatly styled Sort Order Toggle Button
            Surface(
                modifier = Modifier
                    .height(52.dp)
                    .testTag("episodes_sort_toggle_button")
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (isNewestFirst) AniFluxCyan.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { isNewestFirst = !isNewestFirst },
                shape = RoundedCornerShape(12.dp),
                color = if (isNewestFirst) AniFluxCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "عكس ترتيب الحلقات",
                        tint = if (isNewestFirst) AniFluxCyan else Color.LightGray,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isNewestFirst) 180f else 0f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNewestFirst) "الأحدث" else "الأقدم",
                        color = if (isNewestFirst) AniFluxCyan else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Episodes Count / Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredAndSortedEpisodes.size} حلقة",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (isNewestFirst) {
                Text(
                    text = "من الحلقة الأخيرة إلى الأولى (عكسي)",
                    color = AniFluxCyan.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "من الحلقة الأولى إلى الأخيرة (تصاعدي)",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Active Enabled Extensions Strip (Only visible when Admin has enabled compatible extensions; disappears immediately when disabled)
        AnimatedVisibility(visible = enabledExtensions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الإضافات النشطة:",
                            color = Color(0xFFCE93D8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(enabledExtensions, key = { it.extensionId }) { ext ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF9C27B0).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${ext.name} (v${ext.version})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Empty state when search produces no results
        if (filteredAndSortedEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لم يتم العثور على أي حلقة مطابقة لـ \"$searchQuery\"",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                itemsIndexed(items = filteredAndSortedEpisodes, key = { _, it -> it.id }) { index, ep ->
                    com.example.ui.components.StaggeredAnimatedItem(index = index) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .bounceClick { onEpisodeClick(ep) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${ep.episodeNumber}", 
                                            color = MaterialTheme.colorScheme.primary, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.titleAr, 
                                        color = Color.White, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "المدة: ${ep.durationMinutes} دقيقة • سيرفرات متعددة", 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        fontSize = 12.sp
                                    )
                                }

                                val isWatched = watchedEpisodes.contains(ep.episodeNumber)
                                IconButton(onClick = { onToggleWatched(ep.episodeNumber, !isWatched) }) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility, 
                                        contentDescription = "Watched", 
                                        tint = if (isWatched) Color(0xFFE53935) else Color.LightGray
                                    )
                                }
                                IconButton(onClick = { onEpisodeClick(ep) }) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline, 
                                        contentDescription = "Comments", 
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {
    // Global Stats Simulation (Ready for Firebase)
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 4818 + (hash % 100).toInt()
    val baseWatching = 418 + (hash % 50).toInt()
    val basePlan = 6018 + (hash % 200).toInt()
    val baseCompleted = 2018 + (hash % 100).toInt()
    val baseHold = 178 + (hash % 30).toInt()
    val baseDropped = 398 + (hash % 40).toInt()

    // Add local user's status to the base numbers (Independent from Detailed Ratings)
    val favorites = baseFavorites + if (savedStatus == "FAVORITE") 1 else 0
    val watching = baseWatching + if (savedStatus == "WATCHING") 1 else 0
    val plan = basePlan + if (savedStatus == "PLAN_TO_WATCH") 1 else 0
    val completed = baseCompleted + if (savedStatus == "COMPLETED") 1 else 0
    val hold = baseHold + if (savedStatus == "ON_HOLD") 1 else 0
    val dropped = baseDropped + if (savedStatus == "DROPPED") 1 else 0

    val stats = listOf(
        Pair("أنمياتي المفضلة", favorites) to Color(0xFF64B5F6),
        Pair("اشاهدها حاليا", watching) to Color(0xFF81C784),
        Pair("ارغب بمشاهدتها", plan) to Color(0xFFE57373),
        Pair("تم مشاهدتها", completed) to Color(0xFFBA68C8),
        Pair("اكملها لاحقا", hold) to Color(0xFFFFCA28),
        Pair("لا ارغب بمشاهدتها", dropped) to Color(0xFFF06292)
    )

    // Global Ratings Simulation (Independent of user interaction)
    val storyAvg = 9.50f - (Math.abs(hash % 15)) / 10f
    val charAvg = 9.20f - (Math.abs((hash / 2) % 15)) / 10f
    val artAvg = 9.80f - (Math.abs((hash / 3) % 15)) / 10f

    // Matching app native colors
    val cardColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val borderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val onSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        // Card 1: Detailed Ratings
        item {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "متوسط تقييم القصة، الشخصيات، والرسم",
                        color = onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        RatingColumnUI("القصة", String.format(java.util.Locale.US, "%.2f", storyAvg), onSurfaceVariant)
                        RatingColumnUI("الشخصيات", String.format(java.util.Locale.US, "%.2f", charAvg), onSurfaceVariant)
                        RatingColumnUI("الرسم", String.format(java.util.Locale.US, "%.2f", artAvg), onSurfaceVariant)
                    }
                }
            }
        }

        // Card 2: Donut Chart
        item {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top side: Donut Chart (Medium size)
                    Box(modifier = Modifier.size(120.dp).padding(bottom = 20.dp)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val total = stats.sumOf { it.first.second.toDouble() }.toFloat()
                            val strokeWidth = 28f // Medium stroke
                            stats.forEach { (pair, color) ->
                                val sweepAngle = (pair.second / total) * 360f
                                if (sweepAngle > 0) {
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth
                                        )
                                    )
                                }
                                startAngle += sweepAngle
                            }
                        }
                    }

                    // Bottom side: Legend
                    Column(modifier = Modifier.fillMaxWidth()) {
                        stats.forEach { (pair, color) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = pair.first,
                                    color = onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = pair.second.toString(),
                                    color = onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingColumnUI(title: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = textColor, fontSize = 12.sp) 
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold) 
    }
}
@Composable
fun AnimeCharactersTabContent(anime: Anime) {
    val hasCharacters = anime.characters.isNotEmpty()
    val hasStaff = anime.staff.isNotEmpty()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("الشخصيات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (hasCharacters) {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(anime.characters) { person ->
                        Box(modifier = Modifier.width(110.dp).height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .align(Alignment.BottomCenter)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    ))
                            )
                            Text(
                                text = person.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text("لا يوجد شخصيات حتى الان", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        item {
            Text("طاقم العمل", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (hasStaff) {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(anime.staff) { person ->
                        Box(modifier = Modifier.width(110.dp).height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .align(Alignment.BottomCenter)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    ))
                            )
                            Text(
                                text = person.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text("لا يوجد طاقم عمل حتى الان", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CharacterCard(name: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (subtitle != null) {
            Text(text = subtitle, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}


@Composable
fun RelatedAnimeSection(anime: Anime, allAnime: List<Anime>, onRelatedClick: (RelatedAnime) -> Unit) {
    var fetchedRelations by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        fetchedRelations = null
        fetchedRelations = AniListService.getAnimeRelations(anime.titleEn)
    }

    val relatedAnimeList = remember(fetchedRelations, allAnime) {
        fetchedRelations?.filter { related ->
            allAnime.any { appAnime ->
                appAnime.titleEn.equals(related.titleEn, ignoreCase = true) || 
                (appAnime.titleAr.isNotBlank() && appAnime.titleAr.equals(related.titleAr, ignoreCase = true))
            }
        }
    }
    
    if (relatedAnimeList == null) {
        // Loading state
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AniFluxCyan)
        }
    } else if (relatedAnimeList!!.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = "ذات صلة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(bottom = 12.dp),
                textAlign = TextAlign.Right
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(relatedAnimeList!!) { related ->
                    RelatedAnimeCard(related = related, onClick = { onRelatedClick(related) })
                }
            }
        }
    }
}

@Composable
fun RelatedAnimeCard(related: RelatedAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AniFluxCardBg)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(related.posterUrl)
                    .crossfade(true)
                    .build(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                contentDescription = related.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Top Right Badge (Relation Type)
            val badgeColor = when (related.relationType) {
                "SEQUEL" -> Color(0xFF1E88E5) // Blue
                "PREQUEL" -> Color(0xFFE53935) // Red
                "SIDE_STORY" -> Color(0xFF8E24AA) // Purple
                "SPIN_OFF" -> Color(0xFFFDD835) // Yellow
                else -> Color.DarkGray
            }
            
            Surface(
                shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 6.dp),
                color = badgeColor,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = related.relationTypeAr,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // Bottom Right Badge (Rating / Status)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = related.status,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        val titleText = related.titleAr.ifEmpty { related.titleEn }
        val isArabicText = titleText.any { it in '؀'..'ۿ' }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            )
        }
        
        if (related.rating.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = related.rating,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RecommendationsSection(anime: Anime, allAnime: List<Anime>, onRecommendationClick: (Anime) -> Unit) {
    val recommendations = remember(anime, allAnime) {
        val currentGenres = anime.genres.map { it.trim().lowercase() }.toSet()
        allAnime.filter { it.id != anime.id }
            .map { other ->
                val otherGenres = other.genres.map { it.trim().lowercase() }.toSet()
                val sharedCount = otherGenres.intersect(currentGenres).size
                Pair(other, sharedCount)
            }
            .filter { it.second >= 2 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(5)
    }
    
    if (recommendations.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Text(
                text = "توصيات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(bottom = 12.dp),
                textAlign = TextAlign.Right
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { recAnime ->
                    RecommendationAnimeCard(anime = recAnime, onClick = { onRecommendationClick(recAnime) })
                }
            }
        }
    }
}

@Composable
fun RecommendationAnimeCard(anime: Anime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AniFluxCardBg)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.posterUrl)
                    .crossfade(true)
                    .build(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Bottom Right Badge (Episodes / Status)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = if (anime.status == "مستمر") "الحلقة : ${anime.currentEpisodes}" else anime.status,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        val titleText = anime.titleAr.ifEmpty { anime.titleEn }
        val isArabicText = titleText.any { it in '؀'..'ۿ' }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            )
        }
    }
}
