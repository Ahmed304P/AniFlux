import re

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "r") as f:
    content = f.read()

# Fix Title in AnimeCard
title_target = """        Text(
            text = anime.titleAr,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            lineHeight = 18.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )"""

title_replacement = """        Box(
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

content = content.replace(title_target, title_replacement)

list_title_target = """            Text(
                text = anime.titleAr,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 22.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )"""

list_title_replacement = """            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )"""

content = content.replace(list_title_target, list_title_replacement)

# Fix horizontal padding for the bottom row
bottom_row_target = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,"""

bottom_row_replacement = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,"""

content = content.replace(bottom_row_target, bottom_row_replacement)

with open("app/src/main/java/com/example/ui/components/AnimeCard.kt", "w") as f:
    f.write(content)
