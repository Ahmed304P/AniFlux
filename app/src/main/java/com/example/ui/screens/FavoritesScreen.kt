package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedAnimeEntity
import com.example.data.model.Anime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    savedEntities: List<SavedAnimeEntity>,
    allAnimeList: List<Anime>,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    onRemoveFromFavorites: (String) -> Unit // pass animeId
, isGridMode: Boolean = true) {
    var isLoading by remember { mutableStateOf(true) }
    var visibleAnimeList by remember { mutableStateOf<List<Anime>>(emptyList()) }

    // Derive favorites list
    val favoriteAnimeIds = savedEntities.filter { it.status == "FAVORITE" }.map { it.animeId }
    val fullAnimeList = allAnimeList.filter { it.id in favoriteAnimeIds }

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
                text = "أنمياتي المفضلة",
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
                                // Reuse the card from WatchHistoryScreen
                                HistoryAnimeCard(index = index,
                                    anime = anime,
                                    onClick = { onOpenAnimeDetails(anime) },
                                    onRemove = { onRemoveFromFavorites(anime.id) },
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
