with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_selector = """            // Server Selector Bar (Collapsible)
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
            }"""

new_selector = """            // Server Selector Bar (Collapsible)
            AnimatedVisibility(
                visible = showServerSelector,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF151921)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = AniFluxCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اختر سيرفر المشاهدة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(currentEpisode.servers) { server ->
                                val isSelected = selectedServer?.id == server.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) AniFluxCyan else Color(0xFF232834),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF373E4E)),
                                    modifier = Modifier.bounceClick {
                                        selectedServer = server
                                        showServerSelector = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = server.nameAr,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }"""

content = content.replace(old_selector, new_selector)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
