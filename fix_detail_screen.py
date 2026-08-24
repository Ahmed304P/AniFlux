import re

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

target = """                Text(
                    text = anime.titleAr,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis
                )"""

replacement = """                Text(
                    text = anime.titleAr.ifEmpty { anime.titleEn },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )"""

content = content.replace(target, replacement)

# We also need to fix the TopAppBar title to use titleAr.ifEmpty
target_appbar = """                    Text(
                        text = anime.titleAr,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,"""

replacement_appbar = """                    Text(
                        text = anime.titleAr.ifEmpty { anime.titleEn },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,"""

content = content.replace(target_appbar, replacement_appbar)

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
