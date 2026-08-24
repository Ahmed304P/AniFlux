with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.foundation.lazy.LazyRow" not in content:
    content = content.replace("import androidx.compose.foundation.lazy.LazyColumn", "import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.LazyRow\nimport androidx.compose.foundation.lazy.items")

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
