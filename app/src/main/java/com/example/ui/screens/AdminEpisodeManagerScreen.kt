package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEpisodeManagerScreen(
    animeId: String,
    onBack: () -> Unit,
    viewModel: com.example.ui.viewmodel.AnimeViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allAnime by viewModel.filteredAnimeList.collectAsStateWithLifecycle(initialValue = emptyList())
    val anime = allAnime.find { it.id == animeId }

    var serverIdInput by remember(anime?.serverId) { mutableStateOf(anime?.serverId ?: "") }
    var currentEpisodesInput by remember(anime?.currentEpisodes) { mutableStateOf(anime?.currentEpisodes?.toString() ?: "1") }
    var customEpisodesState by remember(anime?.customEpisodes) { mutableStateOf(anime?.customEpisodes ?: emptyMap<String, String>()) }
    
    var isSaving by remember { mutableStateOf(false) }

    // Dialog State for Manual Add / Edit
    var showEpisodeDialog by remember { mutableStateOf(false) }
    var dialogEpisodeNum by remember { mutableStateOf("") }
    var dialogEpisodeLink by remember { mutableStateOf("") }
    var isEditingExisting by remember { mutableStateOf(false) }

    // Save changes globally
    val saveChangesGlobally = {
        if (anime != null) {
            scope.launch {
                isSaving = true
                val episodesCount = currentEpisodesInput.toIntOrNull() ?: 0
                val maxCustom = customEpisodesState.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                val finalEpisodesCount = maxOf(episodesCount, maxCustom)
                
                val updatedAnime = anime.copy(
                    serverId = serverIdInput,
                    currentEpisodes = finalEpisodesCount,
                    customEpisodes = customEpisodesState
                )
                viewModel.saveAnime(updatedAnime)
                currentEpisodesInput = finalEpisodesCount.toString()
                
                delay(400)
                isSaving = false
                snackbarHostState.showSnackbar("تم الحفظ بنجاح وتحديث الحلقات في التطبيق!")
            }
        }
    }

    // Prepare Sorted Episode List (Ascending 1, 2, 3, 4, 5...)
    val baseCount = currentEpisodesInput.toIntOrNull() ?: 0
    val customKeys = customEpisodesState.keys.mapNotNull { it.toIntOrNull() }
    val allNumbersSet = (if (baseCount > 0) (1..baseCount).toSet() else emptySet()) + customKeys
    val sortedEpisodeNumbers = allNumbersSet.sorted()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { 
                    Column {
                        Text("إدارة الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        anime?.let {
                            Text(it.titleAr.ifBlank { it.titleEn }, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { saveChangesGlobally() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF16191F))
            )
        },
        containerColor = Color(0xFF0F1115)
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Auto Engine Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191F)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE91E63).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المحرك الذكي (Auto Engine)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("توليد روابط الحلقات تلقائياً عبر كود السيرفر", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = serverIdInput,
                                onValueChange = { serverIdInput = it },
                                label = { Text("كود السيرفر", color = Color.Gray, fontSize = 13.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color(0xFF2C323E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFE91E63)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = currentEpisodesInput,
                                onValueChange = { currentEpisodesInput = it },
                                label = { Text("عدد الحلقات", color = Color.Gray, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color(0xFF2C323E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFE91E63)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { saveChangesGlobally() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تفعيل وتشغيل الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Header for Episodes List + Manual Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("قائمة الحلقات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${sortedEpisodeNumbers.size} حلقة مسجلة", color = Color.Gray, fontSize = 12.sp)
                    }

                    // Manual Add Button - Opens Popup Dialog
                    Button(
                        onClick = {
                            isEditingExisting = false
                            val nextNum = if (sortedEpisodeNumbers.isEmpty()) 1 else (sortedEpisodeNumbers.maxOrNull() ?: 0) + 1
                            dialogEpisodeNum = nextNum.toString()
                            dialogEpisodeLink = ""
                            showEpisodeDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232834)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة يدوية", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة يدوية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Episodes List (Strictly ordered 1, 2, 3, 4, 5, 6...)
            if (sortedEpisodeNumbers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16191F)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("لا توجد حلقات مسجلة حالياً", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("استخدم التوليد التلقائي أو اضغط 'إضافة يدوية'", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(sortedEpisodeNumbers, key = { it }) { epNumInt ->
                    val epNumStr = epNumInt.toString()
                    val customLink = customEpisodesState[epNumStr]
                    val hasCustomLink = !customLink.isNullOrBlank()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16191F)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (hasCustomLink) Color(0xFF4CAF50).copy(alpha = 0.18f) else Color(0xFF2C323E),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$epNumInt",
                                        color = if (hasCustomLink) Color(0xFF81C784) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("الحلقة $epNumInt", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (hasCustomLink) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رابط مباشر مخصص", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Text("جودة متعددة (سيرفر ذكي)", color = Color(0xFFA0AABF), fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isEditingExisting = true
                                        dialogEpisodeNum = epNumStr
                                        dialogEpisodeLink = customLink ?: ""
                                        showEpisodeDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val newMap = customEpisodesState.toMutableMap()
                                        newMap.remove(epNumStr)
                                        customEpisodesState = newMap
                                        
                                        // If it's the last episode in count, decrement count
                                        val maxRemaining = (sortedEpisodeNumbers.filter { it != epNumInt } + newMap.keys.mapNotNull { it.toIntOrNull() }).maxOrNull() ?: 0
                                        currentEpisodesInput = maxRemaining.toString()
                                        
                                        // Save immediately
                                        if (anime != null) {
                                            val updated = anime.copy(
                                                currentEpisodes = maxRemaining,
                                                customEpisodes = newMap
                                            )
                                            viewModel.saveAnime(updated)
                                        }
                                        scope.launch { snackbarHostState.showSnackbar("تم حذف الحلقة $epNumInt") }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // POPUP MODAL DIALOG FOR ADD / EDIT EPISODE
    // ==========================================
    if (showEpisodeDialog) {
        Dialog(onDismissRequest = { showEpisodeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE91E63).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEditingExisting) Icons.Default.Edit else Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isEditingExisting) "تعديل الحلقة $dialogEpisodeNum" else "إضافة حلقة يدوية",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showEpisodeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Episode Number Input
                    OutlinedTextField(
                        value = dialogEpisodeNum,
                        onValueChange = { dialogEpisodeNum = it },
                        label = { Text("رقم الحلقة (مثال: 1, 2, 8...)", color = Color.Gray, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color(0xFF373E4E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFE91E63)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Episode Stream Link Input
                    OutlinedTextField(
                        value = dialogEpisodeLink,
                        onValueChange = { dialogEpisodeLink = it },
                        label = { Text("رابط الفيديو المباشر (mp4/m3u8)", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF373E4E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons (Cancel / Confirm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEpisodeDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Text("إلغاء", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val epNum = dialogEpisodeNum.trim()
                                val epNumInt = epNum.toIntOrNull()
                                if (epNumInt != null && epNumInt > 0) {
                                    val newMap = customEpisodesState.toMutableMap()
                                    if (dialogEpisodeLink.isNotBlank()) {
                                        newMap[epNum] = dialogEpisodeLink.trim()
                                    } else {
                                        newMap.remove(epNum)
                                    }
                                    customEpisodesState = newMap

                                    // Update max episode count
                                    val currentMax = currentEpisodesInput.toIntOrNull() ?: 0
                                    val newMax = maxOf(currentMax, epNumInt)
                                    currentEpisodesInput = newMax.toString()

                                    // Instantly sync & save to database and ViewModel
                                    if (anime != null) {
                                        val updatedAnime = anime.copy(
                                            currentEpisodes = newMax,
                                            customEpisodes = newMap
                                        )
                                        viewModel.saveAnime(updatedAnime)
                                    }

                                    showEpisodeDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم حفظ وتثبيت الحلقة $epNum بنجاح!")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("يرجى إدخال رقم حلقة صحيح")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditingExisting) "حفظ التعديل" else "إضافة الحلقة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
