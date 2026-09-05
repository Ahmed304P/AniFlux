package com.example.ui.screens

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.ui.components.AnimeCard
import com.example.data.model.FilterState
import com.example.ui.components.FilterBottomSheet

@Composable
fun SearchScreenOverlay(
    visible: Boolean,
    isDetailScreenOpen: Boolean = false,
    onClose: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<Anime>,
    onOpenAnimeDetails: (Anime) -> Unit,
    allAnime: List<Anime>,
    filterState: FilterState,
    onGenreFilterChange: (String?) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onYearFilterChange: (Int?) -> Unit,
    onStudioFilterChange: (String?) -> Unit,
    onAgeRatingFilterChange: (String?) -> Unit,
    onSeasonFilterChange: (String?) -> Unit,
    onResetFilters: () -> Unit
, isGridMode: Boolean = true, title: String? = null) {
    var showFilterSheet by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        LaunchedEffect(visible, isDetailScreenOpen) {
            if (visible && !isDetailScreenOpen && title == null) {
                kotlinx.coroutines.delay(300) // Wait for transition
                focusRequester.requestFocus()
                keyboardController?.show()
            } else {
                keyboardController?.hide()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Top App Bar matching video
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "أدخل اسم الانمي...",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            maxLines = 1
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = { /* Sort */ }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "Filter",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (showFilterSheet) {
                FilterBottomSheet(
                    allAnime = allAnime,
                    filterState = filterState,
                    onDismiss = { showFilterSheet = false },
                    onGenreFilterChange = onGenreFilterChange,
                    onStatusFilterChange = onStatusFilterChange,
                    onTypeFilterChange = onTypeFilterChange,
                    onYearFilterChange = onYearFilterChange,
                    onStudioFilterChange = onStudioFilterChange,
                    onAgeRatingFilterChange = onAgeRatingFilterChange,
                    onSeasonFilterChange = onSeasonFilterChange,
                    onResetFilters = onResetFilters
                )
            }

            // Results Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridMode) 3 else 1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items = searchResults, key = { _, it -> it.id }) { index, anime ->
                    if (isGridMode) {
                        AnimeCard(index = index,
                            anime = anime,
                            onClick = { onOpenAnimeDetails(anime) },
                            showLargePoster = false
                        )
                    } else {
                        com.example.ui.components.AnimeListCard(index = index,
                            anime = anime,
                            onClick = { onOpenAnimeDetails(anime) }
                        )
                    }
                }
            }
        }
    }
}
