package com.example.ui.components

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
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
    onSelectEpisode: (Episode, com.example.data.model.StreamServer?) -> Unit,
    onAddComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onRecordProgress: (Int, Int) -> Unit,
    skipTime: Int = 10,
    initialServer: com.example.data.model.StreamServer? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    var skipUsed by remember(currentEpisode) { mutableStateOf(false) }
    val totalSeconds = (currentEpisode.durationMinutes * 60).toFloat()
    var selectedServer by remember(currentEpisode, initialServer) {
        mutableStateOf(initialServer ?: currentEpisode.servers.firstOrNull())
    }
    var showServerSelector by remember { mutableStateOf(true) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playbackErrorText by remember(selectedServer) { mutableStateOf<String?>(null) }
    var failoverNoticeText by remember(currentEpisode) { mutableStateOf<String?>(null) }
    var retryToken by remember { mutableIntStateOf(0) }
    val failedServerIds = remember(currentEpisode) { mutableStateListOf<String>() }
    val serverFailureLogs = remember(currentEpisode) { mutableStateMapOf<String, String>() }

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

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Player Canvas Header Box
            Box(
                modifier = if (isFullscreen) Modifier.fillMaxSize() else Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                val activeServer = selectedServer
                if (activeServer != null && activeServer.streamUrl.isNotBlank()) {
                    if (activeServer.streamType.equals("EMBED", ignoreCase = true)) {
                        WebViewPlayer(
                            url = activeServer.streamUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ExoPlayerView(
                            url = activeServer.streamUrl,
                            modifier = Modifier.fillMaxSize(),
                            isFullscreen = isFullscreen,
                            playbackSpeed = selectedSpeed,
                            streamType = activeServer.streamType,
                            headers = activeServer.headers,
                            subtitleTracks = activeServer.subtitleTracks,
                            retryToken = retryToken,
                            onFullscreenToggle = { isFullscreen = it },
                            onPlaybackReady = {
                                playbackErrorText = null
                            },
                            onServerFailureLog = { reason ->
                                serverFailureLogs[activeServer.id] = reason
                            },
                            onPlaybackError = { errMsg ->
                                val currentId = activeServer.id
                                if (currentId !in failedServerIds) {
                                    failedServerIds.add(currentId)
                                }
                                val nextFallback = currentEpisode.servers.firstOrNull {
                                    it.id !in failedServerIds && it.streamUrl.isNotBlank()
                                }
                                if (nextFallback != null && nextFallback.id != activeServer.id) {
                                    failoverNoticeText = "تعذر تشغيل (${activeServer.nameAr})، تم التبديل تلقائياً إلى (${nextFallback.nameAr})"
                                    selectedServer = nextFallback
                                    playbackErrorText = null
                                } else {
                                    playbackErrorText = errMsg
                                }
                            }
                        )
                    }
                } else {
                    // Simulated Anime Video Frame fallback
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
                                text = "${anime.primaryTitle} - EP ${currentEpisode.episodeNumber}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "السيرفر: ${selectedServer?.nameAr ?: "سيرفر AniFlux FHD"}",
                                color = AniFluxCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Error Banner & Retry Overlay inside player if all servers fail
                if (playbackErrorText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.82f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playbackErrorText ?: "تعذر تشغيل السيرفر الحالي",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        playbackErrorText = null
                                        retryToken++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AniFluxCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة المحاولة", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        failedServerIds.clear()
                                        val alt = currentEpisode.servers.firstOrNull { it.id != selectedServer?.id } ?: currentEpisode.servers.firstOrNull()
                                        selectedServer = alt
                                        playbackErrorText = null
                                        showServerSelector = true
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تغيير السيرفر", color = Color.White, fontSize = 12.sp)
                                }
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EP ${currentEpisode.episodeNumber} • ${selectedServer?.quality ?: "1080p"} (${selectedServer?.streamType ?: "MP4"})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!selectedServer?.nameAr.isNullOrBlank()) {
                            Text(
                                text = selectedServer?.nameAr ?: "",
                                color = AniFluxCyan,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = { showServerSelector = !showServerSelector }) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Servers",
                            tint = AniFluxCyan
                        )
                    }
                }

                // Bottom Overlay Player Controls (Only if simulated)
                if (selectedServer?.streamUrl?.isBlank() == true || selectedServer == null) {
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
                            val currentMins = (currentProgressSeconds / 60).toInt()
                            val currentSecs = (currentProgressSeconds % 60).toInt()
                            val totalMins = (totalSeconds / 60).toInt()
                            val totalSecs = (totalSeconds % 60).toInt()

                            Text(
                                text = String.format("%02d:%02d / %02d:%02d", currentMins, currentSecs, totalMins, totalSecs),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )

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
            }

            if (!isFullscreen) {
                // Automatic Failover Notice Banner (if triggered)
                if (failoverNoticeText != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2B2111)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = failoverNoticeText ?: "",
                                color = Color(0xFFFFC107),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { failoverNoticeText = null }) {
                                Text("إغلاق", color = AniFluxCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Server Selector Bar & Universal Stream Controls
                AnimatedVisibility(
                    visible = showServerSelector,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF151921)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = AniFluxCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "سيرفرات المشاهدة المتاحة (${currentEpisode.servers.size} سيرفرات)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E3A2F),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "مستخرج ذكي أصلي",
                                            color = Color(0xFF81C784),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(currentEpisode.servers) { server ->
                                    val isSelected = selectedServer?.id == server.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) AniFluxCyan else Color(0xFF232834),
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF373E4E)),
                                        modifier = Modifier.bounceClick {
                                            selectedServer = server
                                            playbackErrorText = null
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = server.nameAr,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${server.quality} • ${server.streamType} • ${server.subtitleLang}",
                                                    color = if (isSelected) Color.Black.copy(alpha = 0.75f) else AniFluxCyan.copy(alpha = 0.85f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
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
                        // Anime Original Title & Episode Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.primaryTitle,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (anime.titleAr.isNotBlank() && anime.titleAr != anime.primaryTitle) {
                                    Text(
                                        text = anime.titleAr,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                    onClick = {
                                        val matchingServer = nextEp.servers.find { it.nameAr == selectedServer?.nameAr } ?: nextEp.servers.firstOrNull()
                                        onSelectEpisode(nextEp, matchingServer)
                                    },
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
                                modifier = Modifier.bounceClick { 
                                    val matchingServer = ep.servers.find { it.nameAr == selectedServer?.nameAr } ?: ep.servers.randomOrNull()
                                    onSelectEpisode(ep, matchingServer)
                                }
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
    }
