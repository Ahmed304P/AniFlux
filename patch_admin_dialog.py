import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace state variable
content = content.replace(
    'var dialogEpisodeLink by remember { mutableStateOf("") }',
    'var dialogServerLinks by remember { mutableStateOf(listOf("")) }'
)

# 2. On Add Manual Episode
content = content.replace(
    'dialogEpisodeLink = ""',
    'dialogServerLinks = listOf("")'
)

# 3. On Edit Episode
old_edit = """                                        isEditingExisting = true
                                        dialogEpisodeNum = epNumStr
                                        dialogEpisodeLink = customLink ?: ""
                                        showEpisodeDialog = true"""

new_edit = """                                        isEditingExisting = true
                                        dialogEpisodeNum = epNumStr
                                        dialogServerLinks = if (customLink.isNullOrBlank()) listOf("") 
                                            else customLink.split(",").map { 
                                                val parts = it.split("|", limit = 2)
                                                if (parts.size == 2) parts[1].trim() else parts[0].trim() 
                                            }
                                        showEpisodeDialog = true"""
content = content.replace(old_edit, new_edit)

# 4. Dialog body
old_dialog = """                    // Episode Stream Link Input
                    OutlinedTextField(
                        value = dialogEpisodeLink,
                        onValueChange = { dialogEpisodeLink = it },
                        label = { Text("روابط السيرفرات (مثال: ST|رابط, MF2|رابط)", color = Color.Gray, fontSize = 13.sp) },
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

                    Spacer(modifier = Modifier.height(24.dp))"""

new_dialog = """                    // Episode Stream Link Inputs
                    Text("السيرفرات:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    dialogServerLinks.forEachIndexed { index, link ->
                        OutlinedTextField(
                            value = link,
                            onValueChange = { newLink ->
                                val newList = dialogServerLinks.toMutableList()
                                newList[index] = newLink
                                dialogServerLinks = newList
                            },
                            label = { Text("رابط سيرفر ${index + 1}", color = Color.Gray, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (dialogServerLinks.size > 1) {
                                    IconButton(onClick = {
                                        val newList = dialogServerLinks.toMutableList()
                                        newList.removeAt(index)
                                        dialogServerLinks = newList
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف السيرفر", tint = Color.Red)
                                    }
                                }
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val newList = dialogServerLinks.toMutableList()
                            newList.add("")
                            dialogServerLinks = newList
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة سيرفر إضافي", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))"""
content = content.replace(old_dialog, new_dialog)

# 5. On Save
old_save = """                                if (epNumInt != null && epNumInt > 0) {
                                    val newMap = customEpisodesState.toMutableMap()
                                    if (dialogEpisodeLink.isNotBlank()) {
                                        newMap[epNum] = dialogEpisodeLink.trim()
                                    } else {
                                        newMap.remove(epNum)
                                    }
                                    customEpisodesState = newMap"""

new_save = """                                if (epNumInt != null && epNumInt > 0) {
                                    val newMap = customEpisodesState.toMutableMap()
                                    val validLinks = dialogServerLinks.filter { it.isNotBlank() }
                                    if (validLinks.isNotEmpty()) {
                                        val formattedString = validLinks.mapIndexed { i, url -> "سيرفر ${i + 1}|$url" }.joinToString(", ")
                                        newMap[epNum] = formattedString
                                    } else {
                                        newMap.remove(epNum)
                                    }
                                    customEpisodesState = newMap"""
content = content.replace(old_save, new_save)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
