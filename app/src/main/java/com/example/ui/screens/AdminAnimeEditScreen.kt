package com.example.ui.screens
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.ui.components.bounceClick
import coil.compose.AsyncImage

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AniListResult

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.Person
import com.example.data.model.Anime

import com.example.util.AniListService
import com.example.util.GeminiTranslationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnimeEditScreen(viewModel: com.example.ui.viewmodel.AnimeViewModel, 
    animeId: String?,
    onBack: () -> Unit,
    onManageEpisodes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1-to-1 Mirror Fields
    var titleAr by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var synopsisAr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("مستمر") }
    var type by remember { mutableStateOf("TV") }
    var year by remember { mutableStateOf("") }
    var studio by remember { mutableStateOf("") }
    var genres by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var ageRating by remember { mutableStateOf("+13") }
    var season by remember { mutableStateOf("ربيع") }
    
    // Additional fields for Staff, Characters, Trailer
    var characters by remember { mutableStateOf<List<Person>>(emptyList()) }
    var staff by remember { mutableStateOf<List<Person>>(emptyList()) }
    var trailerUrl by remember { mutableStateOf("") }

    var isCommentsLocked by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentAnimeId by remember { mutableStateOf(animeId) }
    val isEditMode = currentAnimeId != null

    // Search and Auto-fill state
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
                        searchErrorMessage = "لم يتم العثور على نتائج لـ \"$trimmed\". جرب كتابة الاسم بطريقة أخرى أو بالإنجليزية."
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
            if (fullAnime.genres.isNotBlank()) {
                genres = fullAnime.genres
            } else {
                launch {
                    try {
                        val ani = AniListService.searchAniList(fullAnime.titleEn).firstOrNull()
                        if (ani != null && ani.genres.isNotBlank()) {
                            genres = ani.genres
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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
    }
    
    LaunchedEffect(animeId) {
        currentAnimeId = animeId
        if (animeId != null) {
            val existingAnime = viewModel.allAnime.value.find { it.id == animeId }
            if (existingAnime != null) {
                titleAr = existingAnime.titleAr
                titleEn = existingAnime.titleEn
                posterUrl = existingAnime.posterUrl
                bannerUrl = existingAnime.bannerUrl
                synopsisAr = existingAnime.synopsisAr
                status = existingAnime.status
                type = existingAnime.type
                year = existingAnime.year.toString()
                studio = existingAnime.studio
                genres = existingAnime.genres.joinToString(", ")
                rating = existingAnime.rating.toString()
                ageRating = existingAnime.ageRating
                season = existingAnime.season
                characters = existingAnime.characters
                staff = existingAnime.staff
                trailerUrl = existingAnime.trailerUrl
                isCommentsLocked = existingAnime.isCommentsLocked
            }
        } else {
            titleAr = ""
            titleEn = ""
            posterUrl = ""
            bannerUrl = ""
            synopsisAr = ""
            status = "مستمر"
            type = "TV"
            year = ""
            studio = ""
            genres = ""
            rating = ""
            ageRating = "+13"
            season = "ربيع"
            characters = emptyList()
            staff = emptyList()
            trailerUrl = ""
            isCommentsLocked = false
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                title = { Text(if (isEditMode) "تعديل علي الانمي" else "نشر انمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    delay(1500)
                                    isLoading = false
                                    snackbarHostState.showSnackbar("تم الحذف بنجاح")
                                    onBack()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Smart Auto-Fill & Metadata Import Card (AniList + MyAnimeList)
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
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            Text("البيانات الأساسية", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            AdminTextField("اسم الأنمي (عربي) *", titleAr) { titleAr = it }
            AdminTextField("اسم الأنمي (إنجليزي) *", titleEn) { titleEn = it }
            AdminTextField("سنة الإصدار", year, keyboardType = KeyboardType.Number) { year = it }
            AdminTextField("الاستوديو المنتج", studio) { studio = it }
            
            Text("التصنيفات والتقييم", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            AdminTextField("التصنيفات (مفصولة بفاصلة)", genres) { genres = it }
            AdminTextField("تقييم الأنمي (من 10)", rating, keyboardType = KeyboardType.Decimal) { rating = it }
            
            AdminDropdown("حالة الأنمي", listOf("مستمر", "مكتمل", "قريباً"), status) { status = it }
            AdminDropdown("نوع الإصدار", listOf("TV", "Movie", "OVA", "ONA"), type) { type = it }
            AdminDropdown("التصنيف العمري", listOf("للجميع", "+13", "+17", "+18"), ageRating) { ageRating = it }
            AdminDropdown("موسم العرض", listOf("ربيع", "صيف", "خريف", "شتاء"), season) { season = it }

            Text("الوسائط المتعددة والقصة", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            AdminTextField("رابط صورة الغلاف (Poster) *", posterUrl) { posterUrl = it }
            AdminTextField("رابط صورة الخلفية (Banner)", bannerUrl) { bannerUrl = it }
            
            OutlinedTextField(
                value = synopsisAr,
                onValueChange = { synopsisAr = it },
                label = { Text(if (isTranslating) "جاري الترجمة..." else "قصة الأنمي (Synopsis)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFFD700)
                ),
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Text("الطاقم والشخصيات", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (characters.isNotEmpty()) {
                Text("الشخصيات:", color = Color.LightGray, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(characters) { person ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("لا توجد شخصيات محملة", color = Color.Gray)
            }
            
            if (staff.isNotEmpty()) {
                Text("طاقم العمل:", color = Color.LightGray, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(staff) { person ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("لا يوجد طاقم عمل محمل", color = Color.Gray)
            }
            
            Text("العرض الدعائي", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            AdminTextField("رابط العرض الدعائي (Trailer URL)", trailerUrl) { trailerUrl = it }
            
            // Comment Lock Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إغلاق التعليقات لهذا الأنمي", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("سيتم منع المستخدمين من التعليق", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isCommentsLocked,
                        onCheckedChange = { isCommentsLocked = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red)
                    )
                }
            }
            
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2196F3))
                        .bounceClick { currentAnimeId?.let { onManageEpisodes(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text("إدارة الحلقات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // Form Validation Logic
                    if (titleAr.isBlank() || titleEn.isBlank() || posterUrl.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("الرجاء ملء الحقول الإجبارية (*)") }
                        return@Button
                    }
                    
                    scope.launch {
                        isLoading = true
                        delay(500)

                    val rawInputGenres = genres.split(Regex("[,،]")).map { it.trim() }.filter { it.isNotEmpty() }
                    val cleanedGenres = com.example.util.AniListService.cleanAndFilterGenres(rawInputGenres)
                    val finalGenres = if (cleanedGenres.isNotEmpty()) cleanedGenres else rawInputGenres

                    val newAnime = Anime(
                        id = currentAnimeId ?: java.util.UUID.randomUUID().toString(),
                        titleAr = titleAr,
                        titleEn = titleEn,
                        posterUrl = posterUrl,
                        bannerUrl = bannerUrl,
                        rating = rating.toDoubleOrNull() ?: 0.0,
                        totalEpisodes = 12,
                        currentEpisodes = 1,
                        status = status,
                        type = type,
                        year = year.toIntOrNull() ?: 2024,
                        studio = studio,
                        synopsisAr = synopsisAr,
                        genres = finalGenres,
                        releaseDay = "الأحد",
                        season = season,
                        ageRating = ageRating,
                        isCommentsLocked = isCommentsLocked,
                        characters = characters,
                        staff = staff,
                        trailerUrl = trailerUrl
                    )
                    viewModel.saveAnime(newAnime)
                    viewModel.adminEditingAnimeId = newAnime.id

                    isLoading = false
                    if (currentAnimeId == null) {
                        currentAnimeId = newAnime.id
                        snackbarHostState.showSnackbar("تم نشر الأنمي بنجاح")
                    } else {
                        snackbarHostState.showSnackbar("تم تحديث الأنمي بنجاح")
                    }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isEditMode) "حفظ التغييرات" else "نشر", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AdminTextField(
    label: String, 
    value: String, 
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFFD700),
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFFFD700)
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun AdminDropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        // Invisible box to catch clicks over the entire TextField
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E1E1E)).fillMaxWidth(0.9f)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = Color.White) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

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
