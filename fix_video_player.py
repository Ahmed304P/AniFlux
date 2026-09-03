import re

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the simulated frame with a conditional for WebViewPlayer
simulated_frame = """// Simulated Anime Video Frame
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
                }"""

new_frame = """// Real WebView Player or Simulated Frame
                if (selectedServer?.streamUrl?.isNotBlank() == true) {
                    WebViewPlayer(
                        url = selectedServer!!.streamUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                    }
                }"""
                
bottom_controls = """// Bottom Overlay Player Controls
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
                ) {"""

new_bottom_controls = """// Bottom Overlay Player Controls (Only if simulated)
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
                    ) {"""

content = content.replace(simulated_frame, new_frame)
content = content.replace(bottom_controls, new_bottom_controls)

# We need to close the `if` block for bottom controls. The bottom controls block ends right before Server Selector Overlay.
# Let's just find the Server Selector Overlay comment and inject a `}` before it.
server_selector_comment = "// Server Selector Overlay"
new_server_selector_comment = "}\n                }\n\n                // Server Selector Overlay"
content = content.replace(server_selector_comment, new_server_selector_comment)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
