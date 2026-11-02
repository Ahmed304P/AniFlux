import re

with open('app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt', 'r') as f:
    content = f.read()

# 1. Add required imports
imports_to_add = """import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
"""

content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\n" + imports_to_add)

# 2. Add search states inside AdminAnimeEditScreen
old_search_state = """    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AniListResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    var searchJob by remember { mutableStateOf<Job?>(null) }"""

new_search_state = """    // Search and Auto-fill state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AniListResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedSourceFilter by remember { mutableStateOf("ALL") } // "ALL", "ANILIST", "MYANIMELIST"
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val performSearch: (String) -> Unit = { query ->
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            showDropdown = true
            searchErrorMessage = null
            searchJob = scope.launch {
                isSearching = true
                try {
                    val results = AniListService.searchAnime(trimmed, selectedSourceFilter)
                    searchResults = results
                    if (results.isEmpty()) {
                        searchErrorMessage = "لم يتم العثور على نتائج لـ \\"$trimmed\\". جرب كتابة الاسم بطريقة أخرى أو بالإنجليزية."
                    }
                } catch (e: Exception) {
                    searchErrorMessage = "حدث خطأ أثناء جلب النتائج. تحقق من الاتصال بالإنترنت."
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchResults = emptyList()
            showDropdown = false
        }
    }

    val onSelectAnime: (AniListResult) -> Unit = { anime ->
        showDropdown = false
        searchQuery = anime.titleEn

        scope.launch {
            // If from MyAnimeList, fetch detailed info (full synopsis, studio, genres, trailer, characters)
            val fullAnime = if (anime.source.equals("MyAnimeList", ignoreCase = true)) {
                AniListService.fetchFullMyAnimeListDetails(anime)
            } else {
                anime
            }

            // Autofill all data
            titleEn = fullAnime.titleEn
            titleAr = fullAnime.titleAr
            posterUrl = fullAnime.posterUrl
            if (fullAnime.bannerUrl.isNotBlank()) {
                bannerUrl = fullAnime.bannerUrl
            } else if (bannerUrl.isBlank()) {
                bannerUrl = fullAnime.posterUrl
            }
            if (fullAnime.genres.isNotBlank()) genres = fullAnime.genres
            if (fullAnime.rating.isNotBlank()) rating = fullAnime.rating
            if (fullAnime.status.isNotBlank()) status = fullAnime.status
            if (fullAnime.format.isNotBlank()) type = fullAnime.format
            if (fullAnime.season.isNotBlank()) season = fullAnime.season
            if (fullAnime.year.isNotBlank()) year = fullAnime.year
            if (fullAnime.studio.isNotBlank()) studio = fullAnime.studio
            if (fullAnime.charactersList.isNotEmpty()) characters = fullAnime.charactersList
            if (fullAnime.staffList.isNotEmpty()) staff = fullAnime.staffList
            if (fullAnime.trailer.isNotBlank()) trailerUrl = fullAnime.trailer

            // Trigger translation for synopsis
            if (fullAnime.synopsisEn.isNotBlank()) {
                synopsisAr = "جاري الترجمة الذكية للقصة..."
                isTranslating = true
                launch {
                    val translated = GeminiTranslationService.translateToArabic(fullAnime.synopsisEn)
                    synopsisAr = translated
                    isTranslating = false
                }
            }

            snackbarHostState.showSnackbar(
                message = "تم استيراد بيانات (${fullAnime.titleEn}) بنجاح من ${fullAnime.source}!",
                duration = SnackbarDuration.Short
            )
        }
    }"""

assert old_search_state in content, "old_search_state not found!"
content = content.replace(old_search_state, new_search_state)

# 3. Replace the Search Box block with the new professional Card and Source Selector
old_search_box_start = '            // AniList Search Box\n            Text("البحث التلقائي في AniList (للتعبئة الذاتية)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 16.sp)'

# Find from old_search_box_start up to Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
split_idx1 = content.find(old_search_box_start)
split_idx2 = content.find('            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))', split_idx1)

assert split_idx1 != -1 and split_idx2 != -1, f"Indices not found: {split_idx1}, {split_idx2}"

new_search_box = """            // Smart Auto-Fill & Metadata Import Card (AniList + MyAnimeList)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E232A),
                                Color(0xFF15181E)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00E676).copy(alpha = 0.6f),
                                Color(0xFF02A9FF).copy(alpha = 0.6f),
                                Color(0xFF2E51A2).copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Modern Icon & Professional Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00E676).copy(alpha = 0.25f),
                                        Color(0xFF02A9FF).copy(alpha = 0.25f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "استيراد وتعبئة البيانات تلقائياً",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "سحب القصة والصور والتصنيفات من AniList و MyAnimeList",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp
                        )
                    }
                }

                // Source Selector Chips: [الكل (شامل) | AniList | MyAnimeList]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceFilterChip(
                        title = "الكل (شامل)",
                        isSelected = selectedSourceFilter == "ALL",
                        selectedColor = Color(0xFF00E676),
                        onClick = {
                            selectedSourceFilter = "ALL"
                            if (searchQuery.trim().length >= 2) performSearch(searchQuery)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SourceFilterChip(
                        title = "AniList",
                        isSelected = selectedSourceFilter == "ANILIST",
                        selectedColor = Color(0xFF02A9FF),
                        logoText = "AL",
                        onClick = {
                            selectedSourceFilter = "ANILIST"
                            if (searchQuery.trim().length >= 2) performSearch(searchQuery)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SourceFilterChip(
                        title = "MyAnimeList",
                        isSelected = selectedSourceFilter == "MYANIMELIST",
                        selectedColor = Color(0xFF2E51A2),
                        logoText = "MAL",
                        onClick = {
                            selectedSourceFilter = "MYANIMELIST"
                            if (searchQuery.trim().length >= 2) performSearch(searchQuery)
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // Search Bar with Search Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF101317))
                        .border(1.dp, Color(0xFF2B303B), RoundedCornerShape(14.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length >= 2) {
                                searchJob?.cancel()
                                searchJob = scope.launch {
                                    delay(500)
                                    performSearch(it)
                                }
                            } else {
                                searchResults = emptyList()
                                showDropdown = false
                                searchErrorMessage = null
                            }
                        },
                        placeholder = {
                            Text(
                                "اكتب اسم الأنمي بالعربية أو الإنجليزية...",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = if (selectedSourceFilter == "MYANIMELIST") Color(0xFF2E51A2) 
                                       else if (selectedSourceFilter == "ANILIST") Color(0xFF02A9FF) 
                                       else Color(0xFF00E676)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    showDropdown = false
                                    searchResults = emptyList()
                                    searchErrorMessage = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00E676),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                        singleLine = true
                    )

                    // Explicit Search Action Button
                    Button(
                        onClick = { performSearch(searchQuery) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSourceFilter == "MYANIMELIST") Color(0xFF2E51A2)
                                             else if (selectedSourceFilter == "ANILIST") Color(0xFF02A9FF)
                                             else Color(0xFF00E676),
                            contentColor = if (selectedSourceFilter == "ALL") Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text("بحث", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Dropdown and Search Results Container
                AnimatedVisibility(
                    visible = showDropdown && (searchResults.isNotEmpty() || isSearching || searchErrorMessage != null),
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14171D))
                            .border(1.dp, Color(0xFF282D37), RoundedCornerShape(14.dp))
                    ) {
                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = if (selectedSourceFilter == "MYANIMELIST") Color(0xFF2E51A2)
                                                else if (selectedSourceFilter == "ANILIST") Color(0xFF02A9FF)
                                                else Color(0xFF00E676),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "جاري البحث في ${if (selectedSourceFilter == "MYANIMELIST") "MyAnimeList" else if (selectedSourceFilter == "ANILIST") "AniList" else "AniList و MyAnimeList"}...",
                                        color = Color.LightGray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else if (searchErrorMessage != null && searchResults.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = searchErrorMessage ?: "",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = { performSearch(searchQuery) }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة المحاولة", color = Color(0xFF00E676), fontSize = 13.sp)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                searchResults.forEach { anime ->
                                    AnimeSearchItemCard(
                                        anime = anime,
                                        onClick = { onSelectAnime(anime) }
                                    )
                                    Divider(color = Color.DarkGray.copy(alpha = 0.25f))
                                }
                            }
                        }
                    }
                }
            }
"""

content = content[:split_idx1] + new_search_box + content[split_idx2:]

# 4. Add the bottom composable components (AnimeSearchItemCard, SourceBadge, SourceFilterChip)
bottom_components = """
@Composable
fun AnimeSearchItemCard(
    anime: AniListResult,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // Poster Image
        coil.compose.AsyncImage(
            model = anime.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(62.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF262626))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Title & Source Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anime.titleEn,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Source Logo / Badge
                SourceBadge(source = anime.source)
            }

            if (anime.titleAr.isNotBlank() && anime.titleAr != anime.titleEn) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = anime.titleAr,
                    color = Color(0xFFB0B0B0),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Badges (Rating, Year, Format, Status)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (anime.rating.isNotBlank() && anime.rating != "0" && anime.rating != "N/A") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2A2207))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = anime.rating,
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (anime.year.isNotBlank()) {
                    Text(
                        text = anime.year,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22262F))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (anime.format.isNotBlank()) {
                    Text(
                        text = anime.format,
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0C2436))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (anime.status.isNotBlank()) {
                    Text(
                        text = anime.status,
                        color = if (anime.status == "مستمر") Color(0xFF00E676) else Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (anime.status == "مستمر") Color(0xFF003314) else Color(0xFF222222)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val isAniList = source.equals("AniList", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAniList) Color(0xFF0A1929) else Color(0xFF142442),
        border = BorderStroke(
            1.dp,
            if (isAniList) Color(0xFF02A9FF).copy(alpha = 0.7f) else Color(0xFF2E51A2).copy(alpha = 0.9f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isAniList) Color(0xFF02A9FF) else Color(0xFF4A7BD9))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isAniList) "AniList" else "MyAnimeList",
                color = if (isAniList) Color(0xFF02A9FF) else Color(0xFF6C99F0),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SourceFilterChip(
    title: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoText: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.18f) else Color(0xFF161920),
        border = BorderStroke(
            1.dp,
            if (isSelected) selectedColor else Color(0xFF2A2E3B)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (logoText != null) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) selectedColor else Color(0xFF323846)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = logoText,
                        color = if (isSelected) (if (selectedColor == Color(0xFF00E676)) Color.Black else Color.White) else Color.LightGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
"""

content = content + bottom_components

with open('app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt', 'w') as f:
    f.write(content)

print("AdminAnimeEditScreen.kt patched successfully")
