import re

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add to arguments
content = content.replace(
    'skipTime: Int = 10,',
    'skipTime: Int = 10,\n    initialServer: com.example.data.model.StreamServer? = null,'
)

# Use initialServer
content = content.replace(
    'var selectedServer by remember(currentEpisode) { mutableStateOf(currentEpisode.servers.firstOrNull()) }',
    'var selectedServer by remember(currentEpisode, initialServer) { mutableStateOf(initialServer ?: currentEpisode.servers.firstOrNull()) }'
)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
