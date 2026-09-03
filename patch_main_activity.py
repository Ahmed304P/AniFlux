with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '''                            onSelectEpisode = { ep ->
                                val activeServerForEp = null // we can keep it null or pass something
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, activeServerForEp)
                            },''',
    '''                            onSelectEpisode = { ep, autoServer ->
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, autoServer)
                            },'''
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
