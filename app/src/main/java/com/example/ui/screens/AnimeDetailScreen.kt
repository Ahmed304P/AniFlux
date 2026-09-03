package com.example.ui.screens
import kotlinx.coroutines.launch


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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
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
    val showComments by socialViewModel.shouldOpenComments.collectAsState()
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
                    Text(
                        text = anime.titleAr.ifEmpty { anime.titleEn },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
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
                    0 -> AnimeDetailsTabContent(anime = anime, isSaved = isSaved, savedStatus = savedStatus, userRating = userRating, onOpenMyListOptions = { showMyListSheet = true }, onRateClick = { showRatingDialog = true }, onGenreClick = onGenreClick, onStudioClick = onStudioClick, lazyListState = detailsListState)
                    1 -> AnimeEpisodesTabContent(episodes, watchedEpisodes, onToggleWatched, onEpisodeClick = { episodeActionSheet = it })
                    2 -> AnimeStatsTabContent(anime, isSaved, savedStatus)
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
            val ep = episodeActionSheet!!
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
                                color = Color(0xFFE91E63).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = server.nameAr,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                        textAlign = TextAlign.End
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
fun AnimeDetailsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?, onOpenMyListOptions: () -> Unit, onRateClick: () -> Unit, onGenreClick: (String) -> Unit = {}, onStudioClick: (String) -> Unit = {}, lazyListState: androidx.compose.foundation.lazy.LazyListState) {
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
            ContentRatingSection()
        }
        
        item {
            TrailerSection(anime)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            anime.genres.take(4).forEach { genre ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    onClick = { onGenreClick(genre) }
                ) {
                    Text(
                        text = genre, 
                        color = Color.White, 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
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
                InfoItem(title = "العنوان الانجليزي", value = anime.titleAr, modifier = Modifier.weight(1f))
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
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContentRatingSection() {
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
                textAlign = TextAlign.End
            )
            
            RatingRow("عنف ودموية", "شديد (27)", MaterialTheme.colorScheme.primary)
            RatingRow("ألفاظ نابية", "لا يوجد (23)", AniFluxGreen)
            RatingRow("مشاهد غير لائقة", "لا يوجد (36)", AniFluxGreen)
            RatingRow("مشاهد مخيفة او صادمة", "شديد (21)", MaterialTheme.colorScheme.primary)
            RatingRow("مخدرات وكحول وتدخين", "لا يوجد (19)", AniFluxGreen)
        }
    }
}

@Composable
fun RatingRow(title: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
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
            textAlign = TextAlign.End
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
fun AnimeEpisodesTabContent(episodes: List<Episode>, watchedEpisodes: Set<Int>, onToggleWatched: (Int, Boolean) -> Unit, onEpisodeClick: (Episode) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        itemsIndexed(items = episodes, key = { _, it -> it.id }) { index, ep ->
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
                            Text("${ep.episodeNumber}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ep.titleAr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "المدة: ${ep.durationMinutes} دقيقة • سيرفرات متعددة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color.LightGray)
                    }
                }
            }
        }
    }
}
}
@Composable
fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?) {
    // Generate pseudo-random base stats derived from anime.id
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 5071 + (hash % 100).toInt()
    val baseWatching = 517 + (hash % 50).toInt()
    val basePlan = 6229 + (hash % 200).toInt()
    val baseCompleted = 2200 + (hash % 100).toInt()
    val baseHold = 239 + (hash % 30).toInt()
    val baseDropped = 415 + (hash % 40).toInt()

    // Add local user's status to the base numbers
    val favorites = baseFavorites + if (savedStatus == "FAVORITE") 1 else 0
    val watching = baseWatching + if (savedStatus == "WATCHING") 1 else 0
    val plan = basePlan + if (savedStatus == "PLAN_TO_WATCH") 1 else 0
    val completed = baseCompleted + if (savedStatus == "COMPLETED") 1 else 0
    val hold = baseHold + if (savedStatus == "ON_HOLD") 1 else 0
    val dropped = baseDropped + if (savedStatus == "DROPPED") 1 else 0

    val stats = listOf(
        Pair("أنمياتي المفضلة", favorites) to Color(0xFF0277BD),
        Pair("اشاهدها حاليا", watching) to Color(0xFF388E3C),
        Pair("ارغب بمشاهدتها", plan) to Color(0xFF1976D2),
        Pair("تم مشاهدتها", completed) to Color(0xFF7B1FA2),
        Pair("اكملها لاحقا", hold) to Color(0xFFFBC02D),
        Pair("لا ارغب بمشاهدتها", dropped) to Color(0xFFD32F2F)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("التصويتات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            // Bar chart for ratings (1 to 10)
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 1..10) {
                    val heightRatio = if (i == 10) 1f else (Math.abs(hash % (i * 10)) / 100f).coerceIn(0.1f, 0.5f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(heightRatio)
                                .background(Color(0xFF1976D2), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$i", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular pseudo-pie chart
                Box(
                    modifier = Modifier.size(120.dp).border(12.dp, Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(120.dp).padding(12.dp).border(12.dp, Color(0xFF1976D2), CircleShape))
                }
                
                Column(modifier = Modifier.weight(1f).padding(start = 24.dp)) {
                    stats.forEach { (pair, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(modifier = Modifier.size(12.dp).background(color))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${pair.first} : ${pair.second}", color = Color.LightGray, fontSize = 12.sp)
                        }
}
}
}
}
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
