import sys, re

code = '''@Composable
fun AnimeDetailsHeader(anime: Anime, lazyListState: androidx.compose.foundation.lazy.LazyListState? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
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

        // Original layout: Row with Poster and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            SubcomposeAsyncImage(
                model = anime.posterUrl,
                loading = { androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().shimmerEffect()) },
                contentDescription = anime.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = anime.titleAr,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "(${anime.status}) ${anime.season} ${anime.year}", color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${anime.currentEpisodes} | ${anime.type} حلقة | +13", color = Color.LightGray, fontSize = 14.sp)
            }
        }
    }
}'''

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'@Composable\s+fun AnimeDetailsHeader.*?^}', re.MULTILINE | re.DOTALL)
new_content = pattern.sub(code, content)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(new_content)
print("Patched AnimeDetailsHeader back to original layout!")
