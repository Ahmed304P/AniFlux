import re

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add isFullscreen state
state_block = """    var showServerSelector by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }"""

content = re.sub(r'    var showServerSelector.*?\n    var selectedSpeed.*?\n', state_block + '\n', content, flags=re.MULTILINE)

# Replace Box and Column logic
# We need to change the layout slightly if isFullscreen is true

old_layout = """    Surface(
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
                // Real WebView Player or Simulated Frame
                if (selectedServer?.streamUrl?.isNotBlank() == true) {
                    WebViewPlayer(
                        url = selectedServer!!.streamUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {"""

new_layout = """    Surface(
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
                if (selectedServer?.streamUrl?.isNotBlank() == true) {
                    ExoPlayerView(
                        url = selectedServer!!.streamUrl,
                        modifier = Modifier.fillMaxSize(),
                        isFullscreen = isFullscreen,
                        onFullscreenToggle = { isFullscreen = it }
                    )
                } else {"""

content = content.replace(old_layout, new_layout)

# We need to hide the rest of the column if isFullscreen is true
# Find where the Player Canvas Header Box ends.
old_box_end = """                        }
                    }
                }
                
                // Top Action Bar (Overlay on video)"""

new_box_end = """                        }
                    }
                }
                
                if (!isFullscreen) {
                    // Top Action Bar (Overlay on video)"""

content = content.replace(old_box_end, new_box_end)

# Find the end of the top action bar overlay
old_overlay_end = """                        }
                    }
                }
            }
            
            // Server Selection row (Below Video)"""

new_overlay_end = """                        }
                    }
                }
                }
            }
            
            if (!isFullscreen) {
            // Server Selection row (Below Video)"""

content = content.replace(old_overlay_end, new_overlay_end)

# Find the end of the Column
old_column_end = """                }
            }
        }
    }
}"""

new_column_end = """                }
            }
            }
        }
    }
}"""

content = content.replace(old_column_end, new_column_end)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
