with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'onOpenPlayer = { ep, server ->\n                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)\n                                viewModel.openPlayer(anime, ep, activeServerForEp)',
    'onOpenPlayer = { ep, server ->\n                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)\n                                viewModel.openPlayer(anime, ep, server)'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
