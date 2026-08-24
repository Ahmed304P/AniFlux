import re

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "r") as f:
    content = f.read()

target = """        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 20.dp
                )
            )
        }"""

replacement = """        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "w") as f:
    f.write(content)
