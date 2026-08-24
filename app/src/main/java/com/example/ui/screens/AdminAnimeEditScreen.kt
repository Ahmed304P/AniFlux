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

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AniListResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    var searchJob by remember { mutableStateOf<Job?>(null) }
    
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
            // AniList Search Box
            Text("البحث التلقائي في AniList (للتعبئة الذاتية)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if (showDropdown && (searchResults.isNotEmpty() || isSearching)) 8.dp else 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFF00E676))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E1E))
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        showDropdown = true
                        searchJob?.cancel()
                        if (it.length >= 2) {
                            searchJob = scope.launch {
                                isSearching = true
                                delay(500) // debounce
                                searchResults = AniListService.searchAnime(it)
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    placeholder = { Text("ابحث عن اسم الأنمي هنا...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFF00E676)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                showDropdown = false
                                searchResults = emptyList() 
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                AnimatedVisibility(
                    visible = showDropdown && (searchResults.isNotEmpty() || isSearching),
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                        
                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00E676))
                            }
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                searchResults.forEach { anime ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showDropdown = false
                                                searchQuery = anime.titleEn
                                                
                                                // Autofill all data
                                                titleEn = anime.titleEn
                                                titleAr = anime.titleAr
                                                posterUrl = anime.posterUrl
                                                bannerUrl = anime.bannerUrl
                                                genres = anime.genres
                                                rating = anime.rating
                                                status = anime.status
                                                type = anime.format
                                                season = anime.season
                                                year = anime.year
                                                if (anime.studio.isNotBlank()) studio = anime.studio
                                                characters = anime.charactersList
                                                staff = anime.staffList
                                                trailerUrl = anime.trailer
                                                
                                                // Trigger translation for synopsis
                                                if (anime.synopsisEn.isNotBlank()) {
                                                    synopsisAr = "جاري الترجمة الذكية للقصة..."
                                                    isTranslating = true
                                                    scope.launch {
                                                        val translated = GeminiTranslationService.translateToArabic(anime.synopsisEn)
                                                        synopsisAr = translated
                                                        isTranslating = false
                                                    }
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = anime.posterUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp, 85.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.DarkGray)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = anime.titleEn, 
                                                color = Color.White, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = anime.titleAr, 
                                                color = Color.LightGray, 
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = anime.rating.ifBlank { "N/A" }, 
                                                    color = Color.White, 
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = anime.year, 
                                                    color = Color.Gray, 
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    Divider(color = Color.DarkGray.copy(alpha = 0.3f))
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
                        genres = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
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
