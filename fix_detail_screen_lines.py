import re

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

target = """                Text(
                    text = anime.titleAr.ifEmpty { anime.titleEn },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )"""

replacement = """                Text(
                    text = anime.titleAr.ifEmpty { anime.titleEn },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    overflow = TextOverflow.Ellipsis
                )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
