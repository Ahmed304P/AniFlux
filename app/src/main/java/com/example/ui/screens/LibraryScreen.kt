package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.example.data.local.SavedAnimeEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Anime
import com.example.data.model.LibraryCategory
import com.example.ui.components.AnimeCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    selectedCategory: LibraryCategory,
    savedEntities: List<SavedAnimeEntity>,
    watchHistoryList: List<WatchHistoryEntity>,
    allAnimeList: List<Anime>,
    onSelectCategory: (LibraryCategory) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    onOpenMenu: () -> Unit = {},
    title: String = "المكتبة والمفضلة 📚",
    isGridMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val categories = LibraryCategory.entries
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val savedAnimeForCategory = savedEntities.filter { entity ->
        when (selectedCategory) {
            LibraryCategory.FAVORITES -> entity.status == "FAVORITE"
            LibraryCategory.WATCHING -> entity.status == "WATCHING"
            LibraryCategory.PLAN_TO_WATCH -> entity.status == "PLAN_TO_WATCH"
            LibraryCategory.COMPLETED -> entity.status == "COMPLETED"
            LibraryCategory.HISTORY -> false
        }
    }.mapNotNull { entity ->
        allAnimeList.find { it.id == entity.animeId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "جميع الأنميات التي قمت بحفظها وسجل مشاهداتك المحلية",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.bounceClick { onSelectCategory(cat) }
                    ) {
                        Text(
                            text = cat.titleAr,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (selectedCategory == LibraryCategory.HISTORY) {
            // Watch History View
            if (watchHistoryList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        ,
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("سجل المشاهدة فارغ، ابدأ بمشاهدة أي حلقة الآن!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(watchHistoryList) { history ->
                        val anime = allAnimeList.find { it.id == history.animeId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(anime?.posterUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp, 70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = anime?.titleAr ?: "أنمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = history.episodeTitle, color = AniFluxCyan, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val progressPercent = if (history.totalSeconds > 0) (history.progressSeconds.toFloat() / history.totalSeconds.toFloat()) else 0f
                                    LinearProgressIndicator(
                                        progress = { progressPercent },
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Saved Anime View
            if (savedAnimeForCategory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        ,
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد أنمي مضاف في قسم \"${selectedCategory.titleAr}\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val chunks = if (isGridMode) 2 else 1
                    itemsIndexed(items = savedAnimeForCategory.chunked(chunks), key = { _, it -> it.first().id }) { index, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (anime in pair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (isGridMode) {
                                        AnimeCard(index = index,
                                            anime = anime,
                                            onClick = { onOpenAnimeDetails(anime) }
                                        )
                                    } else {
                                        com.example.ui.components.AnimeListCard(index = index,
                                            anime = anime,
                                            onClick = { onOpenAnimeDetails(anime) }
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveSaved(anime.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
