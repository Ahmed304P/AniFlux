package com.example.ui.screens

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.ui.components.AnimeCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isLoading: Boolean,
    allAnime: List<Anime>,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    isGridMode: Boolean,
    onToggleGridMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar matching EXACT Anime Slayer Header Layout
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Hamburger Menu Button
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Center Title: "آخر التحديثات"
                Text(
                    text = "آخر التحديثات",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                // Right Action Icons: Search & Grid/List View Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onToggleGridMode) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.GridView,
                            contentDescription = "Toggle Grid",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // 3-Column Grid matching Anime Slayer Exactly

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridMode) 3 else 1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoading) {
                    items(15) {
                        com.example.ui.components.ShimmerAnimeCard(index = it, isGridMode = isGridMode)
                    }
                } else {
                                        itemsIndexed(items = allAnime, key = { _, it -> it.id }) { index, anime ->
                    val cardModifier = Modifier.animateItem()
                    if (isGridMode) {
                        AnimeCard(index = index,
                            anime = anime,
                            onClick = { onOpenAnimeDetails(anime) },
                            showLargePoster = false,
                            modifier = cardModifier
                        )
                    } else {
                        com.example.ui.components.AnimeListCard(index = index,
                            anime = anime,
                            onClick = { onOpenAnimeDetails(anime) },
                            modifier = cardModifier
                        )
                    }
                }
                }
            }
        }
    }
}
