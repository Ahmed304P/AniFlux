import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_code = """                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }
                        val isSaved by viewModel.isAnimeSaved(anime.id).collectAsStateWithLifecycle(initialValue = false)
                        val savedStatus by viewModel.getAnimeSavedStatus(anime.id).collectAsStateWithLifecycle(initialValue = null)
                        val userRating by viewModel.getUserRating(anime.id).collectAsStateWithLifecycle(initialValue = null)
                        val watchedEpisodes by viewModel.getWatchedEpisodes(anime.id).collectAsStateWithLifecycle(initialValue = emptySet())"""

new_code = """                        val episodes = remember(anime) { viewModel.getEpisodesForAnime(anime) }
                        
                        val isSavedFlow = remember(anime.id) { viewModel.isAnimeSaved(anime.id) }
                        val isSaved by isSavedFlow.collectAsStateWithLifecycle(initialValue = false)
                        
                        val savedStatusFlow = remember(anime.id) { viewModel.getAnimeSavedStatus(anime.id) }
                        val savedStatus by savedStatusFlow.collectAsStateWithLifecycle(initialValue = null)
                        
                        val userRatingFlow = remember(anime.id) { viewModel.getUserRating(anime.id) }
                        val userRating by userRatingFlow.collectAsStateWithLifecycle(initialValue = null)
                        
                        val watchedEpisodesFlow = remember(anime.id) { viewModel.getWatchedEpisodes(anime.id) }
                        val watchedEpisodes by watchedEpisodesFlow.collectAsStateWithLifecycle(initialValue = emptySet())"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
