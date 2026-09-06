import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_block = """                    val currentAnime = selectedAnimeDetails ?: lastAnime
                    if (currentAnime != null) {
                        val anime = currentAnime
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }"""

new_block = """                    val currentAnime = selectedAnimeDetails ?: lastAnime
                    if (currentAnime != null) {
                        androidx.compose.runtime.key(currentAnime.id) {
                        val anime = currentAnime
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }"""

content = content.replace(old_block, new_block)

old_end = """                            anime = anime,
                            episodes = episodes,
                            isSaved = isSaved,
                            savedStatus = savedStatus,
                            userRating = userRating,
                            watchedEpisodes = watchedEpisodes,
                            onToggleSave = { viewModel.toggleAnimeSaved(anime) },
                            onRatingChange = { rating -> viewModel.rateAnime(anime.id, rating) },
                            onEpisodeClick = { ep ->
                                viewModel.openPlayer(anime, ep)
                            }
                        )
                    }
                }"""

new_end = """                            anime = anime,
                            episodes = episodes,
                            isSaved = isSaved,
                            savedStatus = savedStatus,
                            userRating = userRating,
                            watchedEpisodes = watchedEpisodes,
                            onToggleSave = { viewModel.toggleAnimeSaved(anime) },
                            onRatingChange = { rating -> viewModel.rateAnime(anime.id, rating) },
                            onEpisodeClick = { ep ->
                                viewModel.openPlayer(anime, ep)
                            }
                        )
                        }
                    }
                }"""

content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
