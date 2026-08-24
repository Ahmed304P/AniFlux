import sys

try:
    with open("app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt", "r") as f:
        content = f.read()

    # Fix TopAppBar insets
    top_bar_target = """        topBar = {
            TopAppBar(
                title = { Text("إدارة الحلقات", color = Color.White, fontWeight = FontWeight.Bold) },"""
    top_bar_replacement = """        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                title = { Text("إدارة الحلقات", color = Color.White, fontWeight = FontWeight.Bold) },"""
    
    if top_bar_target in content:
        content = content.replace(top_bar_target, top_bar_replacement)
    else:
        print("Could not find TopAppBar in AdminEpisodeManagerScreen")

    # Now rewrite the card
    card_target_start = """            item {
                // Auto Generator Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),"""
    card_target_end = """                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("حفظ وتوليد الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }"""

    start_idx = content.find(card_target_start)
    end_idx = content.find(card_target_end)
    
    if start_idx != -1 and end_idx != -1:
        end_idx += len(card_target_end)
        
        new_card = """            item {
                // Premium Auto Generator UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF222831), Color(0xFF14171A))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE91E63).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE91E63).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "محرك ربط الحلقات", 
                                    color = Color.White, 
                                    fontSize = 17.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "قم بربط مصدر الحلقات مباشرة ليتم توليدها تلقائياً",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        // Input Group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1115), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            TextField(
                                value = serverIdInput,
                                onValueChange = { serverIdInput = it },
                                placeholder = { Text("كود السيرفر (مثال: 11419)", color = Color.Gray, fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            )
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            
                            TextField(
                                value = currentEpisodesInput,
                                onValueChange = { currentEpisodesInput = it },
                                placeholder = { Text("إجمالي عدد الحلقات", color = Color.Gray, fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                        
                        // Action Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFE91E63), Color(0xFFC2185B))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
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
                                        scope.launch { snackbarHostState.showSnackbar("يرجى تعبئة كافة البيانات المطلوبة") }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("تفعيل وتوليد الحلقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }"""
        
        content = content[:start_idx] + new_card + content[end_idx:]
        
        # Need to add HorizontalDivider import if it's missing
        if "androidx.compose.material3.HorizontalDivider" not in content and "import androidx.compose.material3.*" not in content:
            content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport androidx.compose.material3.HorizontalDivider")
        
        with open("app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt", "w") as f:
            f.write(content)
        print("Success: AdminEpisodeManagerScreen patched")
    else:
        print("Could not find card target in AdminEpisodeManagerScreen")

except Exception as e:
    print(f"Error: {e}")
