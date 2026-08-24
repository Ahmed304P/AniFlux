import sys

try:
    with open("app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt", "r") as f:
        content = f.read()

    # We will replace the entire box of the auto generator and the list below it
    target_start = """            item {
                // Premium Auto Generator UI"""
    target_end = """                        }
                    }
                }
            }
        }
    }
}"""
    
    start_idx = content.find(target_start)
    if start_idx == -1:
        print("Could not find start_idx")
        sys.exit(1)
        
    end_idx = content.rfind(target_end)
    if end_idx == -1:
        print("Could not find end_idx")
        sys.exit(1)
        
    end_idx += len(target_end)

    new_content = """            item {
                // World-Class Auto Generator Engine UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E222A), Color(0xFF16191F))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, Color(0xFF333A47), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(Color(0xFFE91E63), Color(0xFFFF8A80))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "المحرك الذكي (Auto Engine)", 
                                    color = Color.White, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "جلب وربط الحلقات عبر السيرفر الخارجي تلقائياً",
                                    color = Color(0xFFA0AABF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        // Inputs (Side by side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Server Code Input
                            Column(modifier = Modifier.weight(1f)) {
                                Text("كود السيرفر", color = Color(0xFFA0AABF), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                OutlinedTextField(
                                    value = serverIdInput,
                                    onValueChange = { serverIdInput = it },
                                    placeholder = { Text("11419", color = Color.DarkGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF11141A),
                                        unfocusedContainerColor = Color(0xFF11141A),
                                        focusedBorderColor = Color(0xFFE91E63),
                                        unfocusedBorderColor = Color(0xFF2C323E),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, tint = Color.Gray, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                            
                            // Episode Count Input
                            Column(modifier = Modifier.weight(1f)) {
                                Text("عدد الحلقات", color = Color(0xFFA0AABF), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                OutlinedTextField(
                                    value = currentEpisodesInput,
                                    onValueChange = { currentEpisodesInput = it },
                                    placeholder = { Text("12", color = Color.DarkGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF11141A),
                                        unfocusedContainerColor = Color(0xFF11141A),
                                        focusedBorderColor = Color(0xFFE91E63),
                                        unfocusedBorderColor = Color(0xFF2C323E),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Add, tint = Color.Gray, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                        
                        // Action Button
                        Button(
                            onClick = {
                                if (anime != null && serverIdInput.isNotBlank() && currentEpisodesInput.isNotBlank()) {
                                    scope.launch {
                                        isSaving = true
                                        val episodesCount = currentEpisodesInput.toIntOrNull() ?: 1
                                        val updatedAnime = anime.copy(
                                            serverId = serverIdInput,
                                            currentEpisodes = episodesCount
                                        )
                                        viewModel.saveAnime(updatedAnime)
                                        delay(800)
                                        isSaving = false
                                        snackbarHostState.showSnackbar("تم توليد وربط الحلقات بنجاح للمشاهدة!")
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("يرجى إدخال الكود وعدد الحلقات") }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("تفعيل وتشغيل الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة الحلقات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    
                    // Manual Add Button
                    Button(
                        onClick = {
                            // Increment manual episode
                            if (anime != null) {
                                scope.launch {
                                    val currentEps = currentEpisodesInput.toIntOrNull() ?: 0
                                    val newCount = currentEps + 1
                                    currentEpisodesInput = newCount.toString()
                                    val updatedAnime = anime.copy(currentEpisodes = newCount)
                                    viewModel.saveAnime(updatedAnime)
                                    snackbarHostState.showSnackbar("تمت إضافة حلقة جديدة يدوياً")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C323E)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة حلقة", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة يدوية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            val epsCount = currentEpisodesInput.toIntOrNull() ?: 0
            if (epsCount == 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حلقات. استخدم المحرك أو الإضافة اليدوية.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(epsCount) { index ->
                    val epNum = index + 1
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
                                    Text("$epNum", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("الحلقة $epNum", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("جودة متعددة 1080p/720p", color = Color(0xFFA0AABF), fontSize = 12.sp)
                                }
                            }
                            Row {
                                IconButton(onClick = { /* Edit Manual */ }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (anime != null && epsCount > 0) {
                                            scope.launch {
                                                val newCount = epsCount - 1
                                                currentEpisodesInput = newCount.toString()
                                                val updatedAnime = anime.copy(currentEpisodes = newCount)
                                                viewModel.saveAnime(updatedAnime)
                                                snackbarHostState.showSnackbar("تم الحذف بنجاح")
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
}"""
    
    final_content = content[:start_idx] + new_content
    with open("app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt", "w") as f:
        f.write(final_content)
    print("Success: AdminEpisodeManagerScreen patched")

except Exception as e:
    print(f"Error: {e}")
