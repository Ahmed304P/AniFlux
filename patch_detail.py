import re

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "r") as f:
    content = f.read()

# Replace AnimeCharactersTabContent completely
char_tab_replace = """
@Composable
fun AnimeCharactersTabContent(anime: Anime) {
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
                        Column(modifier = Modifier.width(90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray)) {
                                AsyncImage(
                                    model = person.imageUrl,
                                    contentDescription = person.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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
                        Column(modifier = Modifier.width(90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray)) {
                                AsyncImage(
                                    model = person.imageUrl,
                                    contentDescription = person.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("لا يوجد طاقم عمل حتى الان", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        if (anime.trailerUrl.isNotBlank()) {
            item {
                Text("العرض الدعائي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = anime.posterUrl,
                            contentDescription = "Trailer background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.5f
                        )
                        IconButton(onClick = { /* Could open in browser/youtube app */ }, modifier = Modifier.size(64.dp).background(Color.Red.copy(alpha=0.8f), CircleShape)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Trailer", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
"""
content = re.sub(
    r'@Composable\s+fun AnimeCharactersTabContent\(anime: Anime\) \{.*?(?=@Composable\s+fun|\Z)',
    char_tab_replace.strip() + "\n\n",
    content,
    flags=re.DOTALL
)

# Also need to import PlayArrow and AsyncImage
if "import androidx.compose.material.icons.filled.PlayArrow" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.ArrowBack", "import androidx.compose.material.icons.filled.ArrowBack\nimport androidx.compose.material.icons.filled.PlayArrow")
if "import coil.compose.AsyncImage" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp\nimport coil.compose.AsyncImage")

with open("app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt", "w") as f:
    f.write(content)
