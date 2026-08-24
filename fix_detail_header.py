import re

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

target = """                Text(
                    text = anime.titleAr,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )"""

replacement = """                Text(
                    text = anime.titleAr,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis
                )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
