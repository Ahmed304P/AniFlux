package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Anime
import com.example.ui.components.bounceClick
import com.example.ui.theme.AniFluxCardBg
import com.example.ui.theme.AniFluxStar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    watchHistoryList: List<WatchHistoryEntity>,
    allAnimeList: List<Anime>,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    onRemoveFromHistory: (String) -> Unit // pass animeId
, isGridMode: Boolean = true) {
    var isLoading by remember { mutableStateOf(true) }
    var visibleAnimeList by remember { mutableStateOf<List<Anime>>(emptyList()) }

    // Derive distinct anime list from history
    val historyAnimeIds = watchHistoryList.map { it.animeId }.distinct()
    val fullAnimeList = allAnimeList.filter { it.id in historyAnimeIds }

    LaunchedEffect(fullAnimeList) {
        isLoading = true
        delay(600) // Simulate the loading shown in the video
        visibleAnimeList = fullAnimeList
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar matching video
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "اخر المشاهدات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { /* Toggle view type if needed */ }) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List View",
                        tint = Color.White
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGridMode) 3 else 1),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleAnimeList.size, key = { visibleAnimeList[it].id }) { index ->
                        val anime = visibleAnimeList[index]
                        // Simple enter animation
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 50L) // Staggered delay
                            isVisible = true
                        }
                        
                        Box {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
                            ) {
                                HistoryAnimeCard(index = index,
                                    anime = anime,
                                    onClick = { onOpenAnimeDetails(anime) },
                                    onRemove = { onRemoveFromHistory(anime.id) },
                                    isGridMode = isGridMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryAnimeCard(index: Int = 0,
    anime: Anime,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
, isGridMode: Boolean = true) {
    var showDeleteMenu by remember { mutableStateOf(false) }

    if (isGridMode) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f) // Portrait aspect ratio for Anime Slayer style
                .clip(RoundedCornerShape(6.dp))
                .background(AniFluxCardBg)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Delete Dropdown Menu overlay
            Box(modifier = Modifier.align(Alignment.Center)) {
                DropdownMenu(
                    expanded = showDeleteMenu,
                    onDismissRequest = { showDeleteMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) 
                        },
                        onClick = { 
                            showDeleteMenu = false
                            onRemove()
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            // Year Badge (Bottom Right)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = anime.year.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Title
        val titleText = anime.titleAr.ifEmpty { anime.titleEn }
        val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                modifier = Modifier.padding(horizontal = 2.dp).fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        
        // Status and Rating
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = anime.status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AniFluxStar,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${anime.rating}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    } else {
        // List Mode
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showDeleteMenu = true }
                )
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(anime.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.titleAr,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.align(Alignment.Center)) {
                    DropdownMenu(
                        expanded = showDeleteMenu,
                        onDismissRequest = { showDeleteMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) 
                            },
                            onClick = { 
                                showDeleteMenu = false
                                onRemove()
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = anime.year.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f).height(140.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val titleText = anime.titleAr.ifEmpty { anime.titleEn ?: "" }
                val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }

                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isArabicText) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AniFluxStar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${anime.rating}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = anime.status,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}
