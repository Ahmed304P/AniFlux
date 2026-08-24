import re
with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.layout.ContentScale\n", "")
content = content.replace("import androidx.compose.ui.draw.clip\n", "")
content = content.replace("import coil.compose.AsyncImage\n", "")
# re-add them once at the top
imports = "import androidx.compose.ui.layout.ContentScale\nimport androidx.compose.ui.draw.clip\nimport coil.compose.AsyncImage\n"
content = imports + content

# Fix trailerUrl issue
content = content.replace("trailerUrl = anime.trailerUrl", "trailerUrl = anime.trailerUrl")

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(content)
