with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'onSelectEpisode: (Episode) -> Unit,',
    'onSelectEpisode: (Episode, com.example.data.model.StreamServer?) -> Unit,'
)

content = content.replace(
    'onClick = { onSelectEpisode(nextEp) }',
    'onClick = { \n                                    val matchingServer = nextEp.servers.find { it.nameAr == selectedServer?.nameAr } ?: nextEp.servers.randomOrNull()\n                                    onSelectEpisode(nextEp, matchingServer)\n                                }'
)

content = content.replace(
    'modifier = Modifier.bounceClick { onSelectEpisode(ep) }',
    'modifier = Modifier.bounceClick { \n                                    val matchingServer = ep.servers.find { it.nameAr == selectedServer?.nameAr } ?: ep.servers.randomOrNull()\n                                    onSelectEpisode(ep, matchingServer)\n                                }'
)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
