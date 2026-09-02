import sys

code = '''@Composable
fun AnimeDetailsHeader(anime: Anime, lazyListState: androidx.compose.foundation.lazy.LazyListState? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp) // Taller Netflix-style banner
    ) {
        // Main Artwork (Clear, not blurred)
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                // Prefer poster for vertical Netflix-style, fallback to banner
                .data(anime.posterUrl.ifEmpty { anime.bannerUrl })
                .crossfade(true)
                .build(),
            loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
            contentDescription = anime.titleAr,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (lazyListState != null && lazyListState.firstVisibleItemIndex == 0) {
                        translationY = lazyListState.firstVisibleItemScrollOffset * 0.4f
                    }
                }
        )
        
        // Horizontal vignette (blackness on the sides)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Vertical gradient for readability at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
        )

        // Title and Metadata at the bottom center
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = anime.titleAr.ifEmpty { anime.titleEn },
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                lineHeight = 34.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Inline Metadata like Netflix
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${anime.year}",
                    color = Color(0xFF4CAF50), // Netflix matching text color for match/year
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("13+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${anime.currentEpisodes} حلقة",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("HD", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Genres inline
            Text(
                text = "${anime.type} • ${anime.status}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}'''

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

import re

pattern = re.compile(r'@Composable\s+fun AnimeDetailsHeader.*?^}', re.MULTILINE | re.DOTALL)
new_content = pattern.sub(code, content)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(new_content)
print("Patched AnimeDetailsHeader!")
