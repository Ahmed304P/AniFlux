with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = """                                        val currentMax = currentEpisodesInput.toIntOrNull() ?: 0
                                        val newMax = maxOf(currentMax, epNumInt)
                                        currentEpisodesInput = newMax.toString()

                                        showEpisodeDialog = false
                                    }
                                    isCheckingLinks = false
                                }"""

new_logic = """                                        val currentMax = currentEpisodesInput.toIntOrNull() ?: 0
                                        val newMax = maxOf(currentMax, epNumInt)
                                        currentEpisodesInput = newMax.toString()

                                        if (anime != null) {
                                            val updatedAnime = anime.copy(
                                                currentEpisodes = newMax,
                                                customEpisodes = newMap
                                            )
                                            viewModel.saveAnime(updatedAnime)
                                        }

                                        showEpisodeDialog = false
                                        snackbarHostState.showSnackbar("تم الحفظ بنجاح!")
                                    } else {
                                        snackbarHostState.showSnackbar("يرجى إدخال رقم حلقة صحيح")
                                    }
                                    isCheckingLinks = false
                                }"""

content = content.replace(old_logic, new_logic)

old_btn_text = """                            Text(
                                text = if (isEditingExisting) "حفظ التعديل" else "إضافة الحلقة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )"""

new_btn_text = """                            if (isCheckingLinks) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isEditingExisting) "حفظ التعديل" else "إضافة الحلقة",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }"""
                            
content = content.replace(old_btn_text, new_btn_text)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
