package com.example.ui.screens

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedAnimeEntity
import com.example.data.model.Anime
import com.example.ui.components.AnimeCard
import com.example.ui.components.bounceClick
import com.example.ui.theme.AniFluxCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    savedEntities: List<SavedAnimeEntity>,
    allAnimeList: List<Anime>,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    modifier: Modifier = Modifier
, isGridMode: Boolean = true,
    onToggleGridMode: () -> Unit = {}
) {
    val topTabs = listOf("تم مشاهدتها", "ارغب بمشاهدتها", "اشاهدها حاليا", "اكملها لاحقا", "لا ارغب بمشاهدتها")
    val tabEntityStatusMap = listOf("COMPLETED", "PLAN_TO_WATCH", "WATCHING", "ON_HOLD", "DROPPED")
    
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { topTabs.size })
    val coroutineScope = rememberCoroutineScope()

    val filterChips = listOf("مستمر", "مكتمل", "خاصة", "فيلم", "اوفا", "اونا", "مسلسل", "لم يتم بثه بعد")
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Text(
                text = "قائمتي",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(onClick = onOpenSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = onToggleGridMode) {
                    Icon(
                        imageVector = if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = Color.White
                    )
                }
            }
        }

        // Top Tabs
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = AniFluxCyan
                    )
                }
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            }
        ) {
            topTabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { 
                        Text(
                            text = title, 
                            fontSize = 14.sp, 
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) Color.White else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterChips) { chip ->
                val isSelected = selectedFilter == chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.bounceClick {
                        selectedFilter = if (isSelected) null else chip
                    }
                ) {
                    Text(
                        text = chip,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val currentStatus = tabEntityStatusMap[page]
            
            val animeForCurrentTab = savedEntities
                .filter { it.status == currentStatus }
                .mapNotNull { entity -> allAnimeList.find { it.id == entity.animeId } }

            val filteredList = animeForCurrentTab.filter { anime ->
                when (selectedFilter) {
                    null -> true
                    "مستمر" -> anime.status == "مستمر"
                    "مكتمل" -> anime.status == "مكتمل"
                    "لم يتم بثه بعد" -> anime.status == "قريباً" || anime.status == "لم يتم بثه بعد"
                    "خاصة" -> anime.type.equals("Special", ignoreCase = true) || anime.type == "خاصة"
                    "فيلم" -> anime.type.equals("Movie", ignoreCase = true) || anime.type == "فيلم"
                    "اوفا" -> anime.type.equals("OVA", ignoreCase = true)
                    "اونا" -> anime.type.equals("ONA", ignoreCase = true)
                    "مسلسل" -> anime.type.equals("TV", ignoreCase = true) || anime.type == "مسلسل"
                    else -> true
                }
            }
            
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد أنمي بنفس التصنيف",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGridMode) 3 else 1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items = filteredList, key = { _, it -> it.id }) { index, anime ->
                        Box(modifier = Modifier.animateItem()) {
                            if (isGridMode) {
                                if (isGridMode) {
                                    AnimeCard(index = index,anime = anime, onClick = { onOpenAnimeDetails(anime) })
                                } else {
                                    com.example.ui.components.AnimeListCard(index = index,anime = anime, onClick = { onOpenAnimeDetails(anime) })
                                }
                            } else {
                                com.example.ui.components.AnimeListCard(index = index,anime = anime, onClick = { onOpenAnimeDetails(anime) })
                            }
                        }
                    }
                }
            }
        }
    }
}
