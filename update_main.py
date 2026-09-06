import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# The AnimeDetailScreen is called inside NavHost
# We need to find `AnimeDetailScreen(` and insert our new parameters.
# Wait, we saw it earlier:
# AnimeDetailScreen(
#     anime = anime,
#     episodes = episodes,
#     ...

new_params = """AnimeDetailScreen(
                            allAnime = allAnime,
                            onRelatedAnimeClick = { related ->
                                val found = allAnime.find { it.titleEn.equals(related.titleEn, ignoreCase = true) || it.titleAr.equals(related.titleAr, ignoreCase = true) }
                                if (found != null) {
                                    viewModel.openAnimeDetails(found)
                                } else {
                                    android.widget.Toast.makeText(this@MainActivity, "هذا الأنمي غير متوفر حالياً", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRecommendationClick = { rec -> viewModel.openAnimeDetails(rec) },
                            anime = anime,"""

content = content.replace("AnimeDetailScreen(\n                            anime = anime,", new_params)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
