import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'onOpenPlayer = { ep ->',
    'onOpenPlayer = { ep, server ->'
)
content = content.replace(
    'viewModel.openPlayer(anime, ep)',
    'viewModel.openPlayer(anime, ep, server)'
)

content = content.replace(
    'val currentSkipTime by viewModel.skipTime.collectAsStateWithLifecycle()',
    'val currentSkipTime by viewModel.skipTime.collectAsStateWithLifecycle()\n                        val activeServer by viewModel.activePlayerServer.collectAsStateWithLifecycle()'
)
content = content.replace(
    'skipTime = currentSkipTime,\n                            anime = anime,',
    'skipTime = currentSkipTime,\n                            initialServer = activeServer,\n                            anime = anime,'
)
content = content.replace(
    'onSelectEpisode = { ep ->',
    'onSelectEpisode = { ep ->\n                                val activeServerForEp = null // we can keep it null or pass something'
)
with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
