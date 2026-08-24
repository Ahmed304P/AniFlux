with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.layout.ContentScale\nimport androidx.compose.ui.draw.clip\nimport coil.compose.AsyncImage\npackage com.example.ui.screens\n", "package com.example.ui.screens\nimport androidx.compose.ui.layout.ContentScale\nimport androidx.compose.ui.draw.clip\nimport coil.compose.AsyncImage\n")

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(content)
