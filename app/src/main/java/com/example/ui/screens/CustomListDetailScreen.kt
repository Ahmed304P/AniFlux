package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomListEntity
import com.example.data.local.CustomListItemEntity
import com.example.data.model.Anime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListDetailScreen(
    list: CustomListEntity,
    listItems: List<CustomListItemEntity>,
    allAnimeList: List<Anime>,
    onBack: () -> Unit,
    onAddAnimeClick: () -> Unit,
    onOpenAnimeDetails: (Anime) -> Unit,
    onDeleteList: () -> Unit,
    onRemoveAnime: (String) -> Unit
, isGridMode: Boolean = true) {
    var isLoading by remember { mutableStateOf(true) }
    var visibleAnimeList by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var showMenu by remember { mutableStateOf(false) }

    val currentAnimeIds = listItems.map { it.animeId }
    val fullAnimeList = allAnimeList.filter { it.id in currentAnimeIds }

    LaunchedEffect(fullAnimeList) {
        isLoading = true
        delay(600)
        visibleAnimeList = fullAnimeList
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(list.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (list.description.isNotBlank()) {
                            Text(list.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Toggle view type */ }) {
                        Icon(Icons.Default.List, contentDescription = "List View", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تعديل القائمة") },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("مسح القائمة", color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    showMenu = false
                                    onDeleteList()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = onAddAnimeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اضافة انمي", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else if (visibleAnimeList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "لا يوجد بيانات",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 50L)
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
                                    onRemove = { onRemoveAnime(anime.id) },
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
