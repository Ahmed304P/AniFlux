import re

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "r") as f:
    content = f.read()

# Make sure basicMarquee is imported
if "import androidx.compose.foundation.basicMarquee" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.basicMarquee")

# Replace Grid Card Title
grid_target = """        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
        }"""

grid_replacement = """        Box(
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

content = content.replace(grid_target, grid_replacement)

list_target = """            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )"""

list_replacement = """            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 20.sp,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )"""

content = content.replace(list_target, list_replacement)

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "w") as f:
    f.write(content)
