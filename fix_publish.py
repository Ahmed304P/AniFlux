import re

# Update AnimeViewModel
with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    vm_content = f.read()

vm_content = vm_content.replace(
    "currentList.add(anime)",
    "currentList.add(0, anime)"
)

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(vm_content)


# Update AdminAnimeEditScreen
with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    screen_content = f.read()

# Replace val isEditMode = animeId != null with mutable state
screen_content = screen_content.replace(
    "val isEditMode = animeId != null",
    "var currentAnimeId by remember { mutableStateOf(animeId) }\n    val isEditMode = currentAnimeId != null"
)

# Update title
screen_content = screen_content.replace(
    'title = { Text(if (isEditMode) "تعديل الأنمي الشامل" else "إضافة أنمي متكامل"',
    'title = { Text(if (isEditMode) "تعديل الأنمي الشامل" else "نشر انمي"'
)

# Fix episodes button
screen_content = screen_content.replace(
    'onClick = { onManageEpisodes(animeId!!) }',
    'onClick = { currentAnimeId?.let { onManageEpisodes(it) } }'
)
screen_content = screen_content.replace(
    'Text("إدارة حلقات الأنمي"',
    'Text("إدارة الحلقات"'
)

# Update Save Button
save_button_old = """
                        isLoading = false
                        snackbarHostState.showSnackbar(if (isEditMode) "تم تحديث الأنمي بنجاح" else "تمت إضافة الأنمي بنجاح")
                        delay(500)
                        onBack()
"""
save_button_new = """
                        isLoading = false
                        if (currentAnimeId == null) {
                            currentAnimeId = newAnime.id
                            snackbarHostState.showSnackbar("تم نشر الأنمي بنجاح")
                        } else {
                            snackbarHostState.showSnackbar("تم تحديث الأنمي بنجاح")
                        }
"""
screen_content = screen_content.replace(save_button_old, save_button_new)

# Update "حفظ التغييرات" text
screen_content = screen_content.replace(
    'Text("حفظ التغييرات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)',
    'Text(if (isEditMode) "حفظ التغييرات" else "نشر", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)'
)

# ID logic change
screen_content = screen_content.replace(
    'id = animeId ?: java.util.UUID.randomUUID().toString(),',
    'id = currentAnimeId ?: java.util.UUID.randomUUID().toString(),'
)

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(screen_content)
