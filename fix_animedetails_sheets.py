with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

new_sheets = '''        if (episodeActionSheet != null) {
            val ep = episodeActionSheet!!
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { episodeActionSheet = null },
                containerColor = Color(0xFF232834),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "اختر السيرفر",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    ep.servers.forEach { server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    serverActionSheet = Pair(ep, server)
                                    episodeActionSheet = null
                                }
                                .padding(vertical = 16.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE91E63).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = server.nameAr,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(color = Color(0xFF373E4E), thickness = 1.dp)
                    }
                }
            }
        }

        if (serverActionSheet != null) {
            val (ep, server) = serverActionSheet!!
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { serverActionSheet = null },
                containerColor = Color(0xFF232834),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "ماذا تريد أن تفعل؟",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenPlayer(ep, server)
                                serverActionSheet = null
                            }
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("مشاهدة الحلقة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF373E4E), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(server.streamUrl), "video/*")
                                    intent.setPackage("com.dv.adm")
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "تطبيق ADM غير مثبت", android.widget.Toast.LENGTH_SHORT).show()
                                    try {
                                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(server.streamUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {}
                                }
                                serverActionSheet = null
                            }
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2196F3).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF2196F3))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("تحميل عبر ADM", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showMyListSheet) {'''

content = content.replace('    if (showMyListSheet) {', new_sheets)

if 'var serverActionSheet by remember { mutableStateOf<Pair<Episode, com.example.data.model.StreamServer>?>(null)' not in content:
    content = content.replace(
        'var episodeActionSheet by remember { mutableStateOf<Episode?>(null) }',
        'var episodeActionSheet by remember { mutableStateOf<Episode?>(null) }\n    var serverActionSheet by remember { mutableStateOf<Pair<Episode, com.example.data.model.StreamServer>?>(null) }\n    val context = androidx.compose.ui.platform.LocalContext.current'
    )

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
