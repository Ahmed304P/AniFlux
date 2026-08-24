import re

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    content = f.read()

replacement = """                    viewModel.saveAnime(newAnime)
                    viewModel.adminEditingAnimeId = newAnime.id

                    isLoading = false
                    if (currentAnimeId == null) {
                        currentAnimeId = newAnime.id
                        snackbarHostState.showSnackbar("تم نشر الأنمي بنجاح")
                    } else {
                        snackbarHostState.showSnackbar("تم تحديث الأنمي بنجاح")
                    }
"""

content = re.sub(r'                    viewModel\.saveAnime\(newAnime\).*?isLoading = false.*?snackbarHostState\.showSnackbar\("تم تحديث الأنمي بنجاح"\)\n                        \}\n                    \}', replacement + '                    }', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(content)

