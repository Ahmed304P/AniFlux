package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.data.model.CommentItem
import com.example.data.model.Episode
import com.example.data.model.StreamServer
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerModal(
    anime: Anime,
    currentEpisode: Episode,
    allEpisodes: List<Episode>,
    comments: List<CommentItem>,
    onClose: () -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onAddComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onRecordProgress: (Int, Int) -> Unit,
    skipTime: Int = 10,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    var skipUsed by remember(currentEpisode) { mutableStateOf(false) } // 2 mins in
    val totalSeconds = (currentEpisode.durationMinutes * 60).toFloat()
    var selectedServer by remember { mutableStateOf(currentEpisode.servers.firstOrNull()) }
    var showServerSelector by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }

    // Auto update progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && currentProgressSeconds < totalSeconds) {
            delay(1000L * (1f / selectedSpeed).toLong())
            currentProgressSeconds += 1f
            if (currentProgressSeconds % 10f == 0f) {
                onRecordProgress(currentProgressSeconds.toInt(), totalSeconds.toInt())
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Player Canvas Header Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                // Simulated Anime Video Frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    Color.Black
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(36.dp)
                            )
                        }
                        Text(
                            text = "${anime.titleAr} - الحلقة ${currentEpisode.episodeNumber}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "السيرفر: ${selectedServer?.nameAr ?: "سيرفر AniFlux FHD"}",
                            color = AniFluxCyan,
                            fontSize = 11.sp
                        )
                    }
                    
                    if (currentProgressSeconds < skipTime + 5f && !skipUsed) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 24.dp, end = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                .clickable {
                                    currentProgressSeconds += skipTime.toFloat()
                                    skipUsed = true
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تخطي",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Skip",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }




                // Top Overlay Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "الحلقة ${currentEpisode.episodeNumber}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { showServerSelector = !showServerSelector }) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Servers",
                            tint = AniFluxCyan
                        )
                    }
                }

                // Bottom Overlay Player Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Timeline Slider
                    Slider(
                        value = currentProgressSeconds,
                        onValueChange = { currentProgressSeconds = it },
                        valueRange = 0f..totalSeconds,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current time / Total time
                        val currentMins = (currentProgressSeconds / 60).toInt()
                        val currentSecs = (currentProgressSeconds % 60).toInt()
                        val totalMins = (totalSeconds / 60).toInt()
                        val totalSecs = (totalSeconds % 60).toInt()

                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", currentMins, currentSecs, totalMins, totalSecs),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )

                        // Center Playback Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    currentProgressSeconds = (currentProgressSeconds - 10f).coerceAtLeast(0f)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "-10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    currentProgressSeconds = (currentProgressSeconds + 10f).coerceAtMost(totalSeconds)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "+10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Speed Button
                        TextButton(
                            onClick = {
                                selectedSpeed = when (selectedSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "${selectedSpeed}x",
                                color = AniFluxCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Server Selector Bar (Collapsible)
            AnimatedVisibility(visible = showServerSelector) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "اختر سيرفر المشاهدة (AniFlux Servers):",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentEpisode.servers) { server ->
                                FilterChip(
                                    selected = selectedServer?.id == server.id,
                                    onClick = {
                                        selectedServer = server
                                        showServerSelector = false
                                    },
                                    label = { Text("${server.nameAr} (${server.quality})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Episodes Horizontal Selector & Details
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    // Anime Title & Episode Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = anime.titleAr,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "الحلقة ${currentEpisode.episodeNumber} : ${currentEpisode.titleAr}",
                                color = AniFluxCyan,
                                fontSize = 13.sp
                            )
                        }

                        // Next Episode Button
                        val nextEp = allEpisodes.find { it.episodeNumber == currentEpisode.episodeNumber + 1 }
                        if (nextEp != null) {
                            Button(
                                onClick = { onSelectEpisode(nextEp) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "الحلقة التالية", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "جميع حلقات الأنمي:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Episode Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allEpisodes) { ep ->
                            val isSelected = ep.episodeNumber == currentEpisode.episodeNumber
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.bounceClick { onSelectEpisode(ep) }
                            ) {
                                Text(
                                    text = "${ep.episodeNumber}",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comment Section
                    if (anime.isCommentsLocked) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "التعليقات مغلقه من الاداره",
                                color = Color.Red,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        CommentSection(
                            comments = comments,
                            onAddComment = onAddComment,
                            onLikeComment = onLikeComment
                        )
                    }
                }
            }
        }
    }
}
