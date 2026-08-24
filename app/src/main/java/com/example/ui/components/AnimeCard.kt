package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.util.shimmerEffect
import coil.request.ImageRequest
import com.example.data.model.Anime
import com.example.ui.theme.*

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showLargePoster: Boolean = false,
    index: Int = 0
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(100f) }
    
    LaunchedEffect(anime.id) {
        val delay = (index * 50).coerceAtMost(400)
        launch { animatedAlpha.animateTo(1f, tween(durationMillis = 400, delayMillis = delay)) }
        launch { animatedTranslationY.animateTo(0f, tween(durationMillis = 400, delayMillis = delay)) }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .bounceClick(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Poster Container with Exact Badge at Bottom-Right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showLargePoster) 220.dp else 165.dp)
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

            // Bottom Right Overlaid Badge: "الحلقة : X" (Exact Anime Slayer Style)
            Surface(
                shape = RoundedCornerShape(topStart = 6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "الحلقة : ${anime.currentEpisodes}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title below poster
        val titleText = anime.titleAr.ifEmpty { anime.titleEn }
        val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }
        
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text(
                text = titleText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArabicText) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Status and Rating Row: "مستمر  ⭐ 9.14"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = anime.status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AniFluxStar,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${anime.rating}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimeListCard(
    anime: Anime,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(100f) }
    
    LaunchedEffect(anime.id) {
        val delay = (index * 50).coerceAtMost(400)
        launch { animatedAlpha.animateTo(1f, tween(durationMillis = 400, delayMillis = delay)) }
        launch { animatedTranslationY.animateTo(0f, tween(durationMillis = 400, delayMillis = delay)) }
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .bounceClick(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // Poster on the left
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(140.dp)
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
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info on the right
        Column(
            modifier = Modifier
                .weight(1f)
                .height(140.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val titleText = anime.titleAr.ifEmpty { anime.titleEn }
            val isArabicText = titleText.any { it in '\u0600'..'\u06FF' }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides if (isArabicText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isArabicText) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp).fillMaxWidth()
                )
            }

            Text(
                text = "الحلقة : ${anime.currentEpisodes}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = anime.type, // e.g., "مسلسل"
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AniFluxStar,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${anime.rating}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
