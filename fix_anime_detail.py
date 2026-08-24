import re

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

# Remove Trailer from Characters Tab
trailer_regex = r'        if \(anime\.trailerUrl\.isNotBlank\(\)\) \{\n            item \{\n                Text\("العرض الدعائي".*?\}\n            \}\n        \}'
content = re.sub(trailer_regex, '', content, flags=re.DOTALL)

# Redesign Characters and Staff cards
new_characters_tab = """fun AnimeCharactersTabContent(anime: Anime) {
    val hasCharacters = anime.characters.isNotEmpty()
    val hasStaff = anime.staff.isNotEmpty()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("الشخصيات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (hasCharacters) {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(anime.characters) { person ->
                        Box(modifier = Modifier.width(110.dp).height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .align(Alignment.BottomCenter)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    ))
                            )
                            Text(
                                text = person.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text("لا يوجد شخصيات حتى الان", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        item {
            Text("طاقم العمل", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (hasStaff) {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(anime.staff) { person ->
                        Box(modifier = Modifier.width(110.dp).height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .align(Alignment.BottomCenter)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    ))
                            )
                            Text(
                                text = person.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text("لا يوجد طاقم عمل حتى الان", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}"""

content = re.sub(r'fun AnimeCharactersTabContent\(anime: Anime\) \{.*?(?=\n\n@Composable\nfun CharacterCard)', new_characters_tab, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
