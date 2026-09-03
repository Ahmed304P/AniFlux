import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'onOpenPlayer: (Episode) -> Unit,',
    'onOpenPlayer: (Episode, com.example.data.model.StreamServer?) -> Unit,'
)

# Update AnimeEpisodesTabContent signature
content = content.replace(
    'fun AnimeEpisodesTabContent(episodes: List<Episode>, watchedEpisodes: Set<Int>, onToggleWatched: (Int, Boolean) -> Unit, onOpenPlayer: (Episode) -> Unit) {',
    'fun AnimeEpisodesTabContent(episodes: List<Episode>, watchedEpisodes: Set<Int>, onToggleWatched: (Int, Boolean) -> Unit, onEpisodeClick: (Episode) -> Unit) {'
)
content = content.replace(
    '.bounceClick { onOpenPlayer(ep) }',
    '.bounceClick { onEpisodeClick(ep) }'
)
content = content.replace(
    'IconButton(onClick = { onOpenPlayer(ep) }) {',
    'IconButton(onClick = { onEpisodeClick(ep) }) {'
)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
