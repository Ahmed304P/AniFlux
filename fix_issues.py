import re
import os

# 1. Fix AniListService.kt
file1 = 'app/src/main/java/com/example/util/AniListService.kt'
with open(file1, 'r') as f:
    content1 = f.read()

target1 = """                    val status = node.optString("status", "")
                    val format = node.optString("format", "")
                    
                    val mappedStatus = when (status) {"""

replace1 = """                    val status = node.optString("status", "")
                    val format = node.optString("format", "")
                    
                    if (format == "MANGA" || format == "NOVEL" || format == "ONE_SHOT") continue
                    
                    val mappedStatus = when (status) {"""

content1 = content1.replace(target1, replace1)

with open(file1, 'w') as f:
    f.write(content1)


# 2. Fix AnimeDetailScreen.kt alignment
file2 = 'app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt'
with open(file2, 'r') as f:
    content2 = f.read()

content2 = content2.replace('textAlign = TextAlign.End', 'textAlign = TextAlign.Right')

with open(file2, 'w') as f:
    f.write(content2)


# 3. Fix MainActivity.kt animation
file3 = 'app/src/main/java/com/example/MainActivity.kt'
with open(file3, 'r') as f:
    content3 = f.read()

target3 = """                    val currentAnime = selectedAnimeDetails ?: lastAnime
                    if (currentAnime != null) {
                        androidx.compose.runtime.key(currentAnime.id) {
                        val anime = currentAnime
                        val context = androidx.compose.ui.platform.LocalContext.current"""

replace3 = """                    val currentAnime = selectedAnimeDetails ?: lastAnime
                    if (currentAnime != null) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = currentAnime,
                            transitionSpec = {
                                (androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))).togetherWith(
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)))
                            },
                            label = "AnimeDetailTransition"
                        ) { targetAnime ->
                        val anime = targetAnime
                        val context = androidx.compose.ui.platform.LocalContext.current"""

target3_end = """                            onOpenPlayer = { ep, server ->
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, server)
                            }
                        )
                    }
                }"""

replace3_end = """                            onOpenPlayer = { ep, server ->
                                viewModel.loadCommentsForEpisode(anime.id, ep.episodeNumber)
                                viewModel.openPlayer(anime, ep, server)
                            }
                        )
                        }
                    }
                }"""

content3 = content3.replace(target3, replace3)
content3 = content3.replace(target3_end, replace3_end)

with open(file3, 'w') as f:
    f.write(content3)

print("Done")
