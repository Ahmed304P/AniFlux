package com.example.ui.screens

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed



import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import com.example.data.model.Anime
import com.example.data.model.FilterState
import com.example.ui.components.bounceClick
import com.example.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    filteredAnimeList: List<Anime>,
    filterState: FilterState,
    onSearchQueryChange: (String) -> Unit,
    onGenreFilterChange: (String?) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    onOpenMenu: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    title: String = "لائحة الانمي",
    isGridMode: Boolean = true,
    onToggleGridMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar exactly like HomeScreen
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
            // Center Title
            Text(
                text = title,
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
                        contentDescription = "Toggle View",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Content Area
        AnimatedContent(
            targetState = isGridMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ViewToggleAnimation"
        ) { gridView ->
            if (gridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items = filteredAnimeList, key = { _, it -> it.id }) { index, anime ->
                        AnimeExploreGridCard(anime = anime, onClick = { onOpenAnimeDetails(anime) })
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items = filteredAnimeList, key = { _, it -> it.id }) { index, anime ->
                        AnimeExploreListCard(anime = anime, onClick = { onOpenAnimeDetails(anime) })
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeExploreGridCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(100f) }
    
    LaunchedEffect(anime.id) {
        launch { animatedAlpha.animateTo(1f, tween(500)) }
        launch { animatedTranslationY.animateTo(0f, tween(500)) }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        // Poster Container with Year Badge at Bottom-Center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
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
            // Bottom Right Overlaid Badge: Year (e.g. 2010)
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
        // Title below poster
        Text(
            text = anime.titleAr,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Status and Rating Row
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
                    contentDescription = "Rating",
                    tint = AniFluxStar,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${anime.rating}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimeExploreListCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(100f) }
    
    LaunchedEffect(anime.id) {
        launch { animatedAlpha.animateTo(1f, tween(500)) }
        launch { animatedTranslationY.animateTo(0f, tween(500)) }
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
            .fillMaxWidth()
            .height(140.dp)
            .bounceClick { onClick() },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
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
        }
        
        // Details
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = anime.titleAr,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = anime.status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = anime.genres.joinToString("، "),
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = anime.year.toString(),
                color = Color.White,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = AniFluxStar,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${anime.rating}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
