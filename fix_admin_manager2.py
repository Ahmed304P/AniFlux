import re

with open("app/src/main/java/com/example/ui/screens/AdminAnimeManagerScreen.kt", "r") as f:
    content = f.read()

# Fix package import issue
content = content.replace("import androidx.compose.material.icons.filled.Search\npackage com.example.ui.screens", "package com.example.ui.screens\nimport androidx.compose.material.icons.filled.Search")

# Remove the junk left behind by regex
bad_code = """                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },"""
content = content.replace(bad_code, "")

with open("app/src/main/java/com/example/ui/screens/AdminAnimeManagerScreen.kt", "w") as f:
    f.write(content)

