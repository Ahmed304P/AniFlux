package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.data.model.FilterState
import com.example.ui.theme.AniFluxCardBg
import com.example.ui.theme.AniFluxCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    allAnime: List<Anime>,
    filterState: FilterState,
    onDismiss: () -> Unit,
    onGenreFilterChange: (String?) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onYearFilterChange: (Int?) -> Unit,
    onStudioFilterChange: (String?) -> Unit,
    onAgeRatingFilterChange: (String?) -> Unit,
    onSeasonFilterChange: (String?) -> Unit,
    onResetFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Extract unique dynamic values
    val availableYears = remember(allAnime) { allAnime.map { it.year }.distinct().sortedDescending() }
    val availableStudios = remember(allAnime) { allAnime.map { it.studio }.distinct().sorted() }
    val availableTypes = remember(allAnime) { allAnime.map { it.type }.distinct() }
    val availableStatuses = remember(allAnime) { allAnime.map { it.status }.distinct() }
    val availableGenres = remember(allAnime) { allAnime.flatMap { it.genres }.distinct().sorted() }
    val availableSeasons = remember(allAnime) { allAnime.map { it.season }.distinct() }
    val availableAgeRatings = remember(allAnime) { allAnime.map { it.ageRating }.distinct() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تصنيف بواسطة ؟",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Section
                item {
                    FilterSection(
                        title = "الحالة",
                        items = availableStatuses,
                        selectedItem = filterState.selectedStatus,
                        onItemSelected = { onStatusFilterChange(it) }
                    )
                }

                // Type Section
                item {
                    FilterSection(
                        title = "نوع الانمي",
                        items = availableTypes,
                        selectedItem = filterState.selectedType,
                        onItemSelected = { onTypeFilterChange(it) }
                    )
                }

                // Season Section
                item {
                    FilterSection(
                        title = "الموسم",
                        items = availableSeasons,
                        selectedItem = filterState.selectedSeason,
                        onItemSelected = { onSeasonFilterChange(it) }
                    )
                }

                // Age Rating Section
                item {
                    FilterSection(
                        title = "العمر",
                        items = availableAgeRatings,
                        selectedItem = filterState.selectedAgeRating,
                        onItemSelected = { onAgeRatingFilterChange(it) }
                    )
                }

                // Year Section (Expandable)
                item {
                    ExpandableFilterSection(
                        title = "السنة",
                        items = availableYears.map { it.toString() },
                        selectedItem = filterState.selectedYear?.toString(),
                        onItemSelected = { onYearFilterChange(it?.toIntOrNull()) }
                    )
                }

                // Studio Section (Expandable)
                item {
                    ExpandableFilterSection(
                        title = "الاستوديو",
                        items = availableStudios,
                        selectedItem = filterState.selectedStudio,
                        onItemSelected = { onStudioFilterChange(it) }
                    )
                }

                // Genre Section (Expandable)
                item {
                    ExpandableFilterSection(
                        title = "النوع",
                        items = availableGenres,
                        selectedItem = filterState.selectedGenre,
                        onItemSelected = { onGenreFilterChange(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onResetFilters() }) {
                    Text(
                        text = "الغاء",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
                TextButton(onClick = { onDismiss() }) {
                    Text(
                        text = "تطبيق",
                        color = AniFluxCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // We use Row + wrap or simple Grid if many. If it's short like Status/Type/Season, use FlowRow or simple Row.
        // For simplicity and Android standard, we can use a custom FlowRow or LazyVerticalGrid but inside LazyColumn we must be careful with height.
        // Since we are inside LazyColumn item, let's use a standard FlowRow equivalent (OptIn ExperimentalLayoutApi).
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AniFluxCyan.copy(alpha = 0.2f) else AniFluxCardBg)
                        .clickable { onItemSelected(if (isSelected) null else item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        color = if (isSelected) AniFluxCyan else Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableFilterSection(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AniFluxCardBg)
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White
            )
        }

        AnimatedVisibility(visible = expanded) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AniFluxCyan.copy(alpha = 0.2f) else AniFluxCardBg)
                            .clickable { onItemSelected(if (isSelected) null else item) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) AniFluxCyan else Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
