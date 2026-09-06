import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

# Add the imports if not present
if "import com.example.util.RelatedAnime" not in content:
    content = content.replace("import com.example.data.model.Episode", "import com.example.data.model.Episode\nimport com.example.util.RelatedAnime\nimport com.example.util.AniListService")

# Prepare the new sections code
sections_code = """
        item {
            RelatedAnimeSection(anime, onRelatedAnimeClick)
        }
        
        item {
            RecommendationsSection(anime, allAnime, onRecommendationClick)
        }
"""

content = content.replace("            TrailerSection(anime)\n        }", "            TrailerSection(anime)\n        }\n" + sections_code)

# Add the Composable functions at the end of the file
new_composables = """
@Composable
fun RelatedAnimeSection(anime: Anime, onRelatedClick: (RelatedAnime) -> Unit) {
    var relatedAnimeList by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        relatedAnimeList = null
        relatedAnimeList = AniListService.getAnimeRelations(anime.titleEn)
    }
    
    if (relatedAnimeList == null) {
        // Loading state
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AniFluxCyan)
        }
    } else if (relatedAnimeList!!.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = "ذات صلة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, bottom = 12.dp),
                textAlign = TextAlign.End
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(relatedAnimeList!!) { related ->
                    RelatedAnimeCard(related = related, onClick = { onRelatedClick(related) })
                }
            }
        }
    }
}

@Composable
fun RelatedAnimeCard(related: RelatedAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AniFluxCardBg)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(related.posterUrl)
                    .crossfade(true)
                    .build(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                contentDescription = related.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Top Right Badge (Relation Type)
            val badgeColor = when (related.relationType) {
                "SEQUEL" -> Color(0xFF1E88E5) // Blue
                "PREQUEL" -> Color(0xFFE53935) // Red
                "SIDE_STORY" -> Color(0xFF8E24AA) // Purple
                "SPIN_OFF" -> Color(0xFFFDD835) // Yellow
                else -> Color.DarkGray
            }
            
            Surface(
                shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 6.dp),
                color = badgeColor,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = related.relationTypeAr,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // Bottom Right Badge (Rating / Status)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = related.status,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        val titleText = related.titleAr.ifEmpty { related.titleEn }
        val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            )
        }
        
        if (related.rating.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = related.rating,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RecommendationsSection(anime: Anime, allAnime: List<Anime>, onRecommendationClick: (Anime) -> Unit) {
    val recommendations = remember(anime, allAnime) {
        val currentGenres = anime.genres.map { it.trim().lowercase() }.toSet()
        allAnime.filter { it.id != anime.id }
            .map { other ->
                val otherGenres = other.genres.map { it.trim().lowercase() }.toSet()
                val sharedCount = otherGenres.intersect(currentGenres).size
                Pair(other, sharedCount)
            }
            .filter { it.second >= 2 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(5)
    }
    
    if (recommendations.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Text(
                text = "توصيات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, bottom = 12.dp),
                textAlign = TextAlign.End
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { recAnime ->
                    RecommendationAnimeCard(anime = recAnime, onClick = { onRecommendationClick(recAnime) })
                }
            }
        }
    }
}

@Composable
fun RecommendationAnimeCard(anime: Anime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AniFluxCardBg)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.posterUrl)
                    .crossfade(true)
                    .build(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Bottom Right Badge (Episodes / Status)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = if (anime.status == "مستمر") "الحلقة : ${anime.currentEpisodes}" else anime.status,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        val titleText = anime.titleAr.ifEmpty { anime.titleEn }
        val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            )
        }
    }
}
"""

content = content + "\n" + new_composables

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(content)
