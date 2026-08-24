import sys

content = """package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    // Manual Form State
    var isManualFormVisible by remember { mutableStateOf(false) }
    var manualEpNum by remember { mutableStateOf("") }
    var manualEpLink by remember { mutableStateOf("") }

    val handleSaveGlobally = {
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
                
                delay(500)
                isSaving = false
                snackbarHostState.showSnackbar("تم حفظ التعديلات وتحديث الحلقات في التطبيق بالكامل!")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                title = { Text("إدارة الحلقات", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = handleSaveGlobally) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.Green, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "حفظ", tint = Color.Green)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFF0F1115)
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191F)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFE91E63))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المحرك الذكي (Auto Engine)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = serverIdInput,
                                onValueChange = { serverIdInput = it },
                                label = { Text("كود السيرفر", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color.DarkGray,
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
                                label = { Text("عدد الحلقات", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFE91E63)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = handleSaveGlobally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("توليد وحفظ الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة الحلقات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            isManualFormVisible = !isManualFormVisible
                            if (isManualFormVisible) {
                                val nextEp = (currentEpisodesInput.toIntOrNull() ?: 0) + 1
                                manualEpNum = nextEp.toString()
                                manualEpLink = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C323E)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة يدوية", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة يدوية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = isManualFormVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("إضافة / تعديل حلقة يدوياً", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = manualEpNum,
                                onValueChange = { manualEpNum = it },
                                label = { Text("رقم الحلقة", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = manualEpLink,
                                onValueChange = { manualEpLink = it },
                                label = { Text("رابط الحلقة (مباشر)", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE91E63),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { isManualFormVisible = false }) {
                                    Text("إلغاء", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (manualEpNum.isNotBlank() && manualEpLink.isNotBlank()) {
                                            val newMap = customEpisodesState.toMutableMap()
                                            newMap[manualEpNum] = manualEpLink
                                            customEpisodesState = newMap
                                            
                                            // Update current max
                                            val epNumInt = manualEpNum.toIntOrNull() ?: 0
                                            val currentInt = currentEpisodesInput.toIntOrNull() ?: 0
                                            if (epNumInt > currentInt) {
                                                currentEpisodesInput = epNumInt.toString()
                                            }
                                            
                                            isManualFormVisible = false
                                            scope.launch { snackbarHostState.showSnackbar("تم حفظ الحلقة، اضغط (حفظ) بالأعلى لتطبيقها.") }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                                ) {
                                    Text("تأكيد", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            val epsCount = currentEpisodesInput.toIntOrNull() ?: 0
            val maxCustom = customEpisodesState.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
            val totalRender = maxOf(epsCount, maxCustom)

            if (totalRender == 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حلقات. استخدم المحرك أو الإضافة اليدوية.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(totalRender) { index ->
                    val epNum = (index + 1).toString()
                    val hasCustomLink = customEpisodesState.containsKey(epNum)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16191F)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF2C323E), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(epNum, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("الحلقة $epNum", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    if (hasCustomLink) {
                                        Text("مربوطة يدوياً (مخصصة)", color = Color(0xFF4CAF50), fontSize = 12.sp)
                                    } else {
                                        Text("جودة متعددة (تلقائي)", color = Color(0xFFA0AABF), fontSize = 12.sp)
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { 
                                    manualEpNum = epNum
                                    manualEpLink = customEpisodesState[epNum] ?: ""
                                    isManualFormVisible = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (hasCustomLink) {
                                            val newMap = customEpisodesState.toMutableMap()
                                            newMap.remove(epNum)
                                            customEpisodesState = newMap
                                            scope.launch { snackbarHostState.showSnackbar("تمت إزالة الرابط اليدوي، سيعود للوضع التلقائي") }
                                        } else {
                                            if (epNum.toInt() == totalRender) {
                                                currentEpisodesInput = (totalRender - 1).toString()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt", "w") as f:
    f.write(content)

print("Rewrote AdminEpisodeManagerScreen.kt")
