import re

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add activePlayerServer
if 'val activePlayerServer' not in content:
    content = content.replace(
        'private val _activePlayerEpisode = MutableStateFlow<Pair<Anime, Episode>?>(null)',
        'private val _activePlayerEpisode = MutableStateFlow<Pair<Anime, Episode>?>(null)\n    val activePlayerEpisode = _activePlayerEpisode.asStateFlow()\n    private val _activePlayerServer = MutableStateFlow<com.example.data.model.StreamServer?>(null)\n    val activePlayerServer = _activePlayerServer.asStateFlow()'
    )
    content = content.replace(
        'val activePlayerEpisode = _activePlayerEpisode.asStateFlow()\n    val activePlayerEpisode = _activePlayerEpisode.asStateFlow()',
        'val activePlayerEpisode = _activePlayerEpisode.asStateFlow()'
    )

content = content.replace(
    'fun openPlayer(anime: Anime, episode: Episode) {',
    'fun openPlayer(anime: Anime, episode: Episode, server: com.example.data.model.StreamServer? = null) {\n        _activePlayerServer.value = server'
)

content = content.replace(
    'fun closePlayer() {\n        _activePlayerEpisode.value = null',
    'fun closePlayer() {\n        _activePlayerEpisode.value = null\n        _activePlayerServer.value = null'
)

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
