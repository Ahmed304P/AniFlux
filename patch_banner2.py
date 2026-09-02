import sys, re

code = '''@Composable
fun AnimeDetailsHeader(anime: Anime, lazyListState: androidx.compose.foundation.lazy.LazyListState? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clipToBounds() // CRITICAL FIX: prevents the image from bleeding out to the bottom of the screen
    ) {
        // Main Background Banner (Clear, not blurred)
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.bannerUrl.ifEmpty { anime.posterUrl })
                .crossfade(true)
                .build(),
            loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
            contentDescription = anime.titleAr,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true // Double protection against bleeding
                    if (lazyListState != null && lazyListState.firstVisibleItemIndex == 0) {
                        translationY = lazyListState.firstVisibleItemScrollOffset * 0.4f
                    }
                }
        )
        
        // Netflix-style Horizontal Vignette (Dark on sides)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Vertical gradient for readability at the bottom, blending smoothly into the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 100f
                    )
                )
        )

        // Content Row (Title and Small Poster)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poster Image
            SubcomposeAsyncImage(
                model = anime.posterUrl,
                loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            )

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = anime.titleAr.ifEmpty { anime.titleEn },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 3,
                    lineHeight = 26.sp,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${anime.status} | ${anime.year}",
                    color = Color(0xFF4CAF50), // Green accent for status
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("13+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${anime.currentEpisodes} حلقة | ${anime.type}",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}'''

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'@Composable\s+fun AnimeDetailsHeader.*?^}', re.MULTILINE | re.DOTALL)
new_content = pattern.sub(code, content)

# Check if clipToBounds is imported
if 'import androidx.compose.ui.draw.clipToBounds' not in new_content:
    new_content = new_content.replace('import androidx.compose.ui.draw.clip', 'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.clipToBounds')

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(new_content)
print("Patched AnimeDetailsHeader again!")
