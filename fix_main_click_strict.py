import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_click = """                            onRelatedAnimeClick = { related ->
                                val found = allAnime.find { appAnime ->
                                    val appEn = appAnime.titleEn.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                                    val appAr = appAnime.titleAr.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                                    val relEn = related.titleEn.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                                    val relAr = related.titleAr.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                                    val matchEn = appEn.isNotEmpty() && relEn.isNotEmpty() && (appEn == relEn || appEn.contains(relEn) || relEn.contains(appEn))
                                    val matchAr = appAr.isNotEmpty() && relAr.isNotEmpty() && (appAr == relAr || appAr.contains(relAr) || relAr.contains(appAr))
                                    matchEn || matchAr
                                }
                                if (found != null) {"""

new_click = """                            onRelatedAnimeClick = { related ->
                                val found = allAnime.find { appAnime ->
                                    appAnime.titleEn.equals(related.titleEn, ignoreCase = true) || 
                                    (appAnime.titleAr.isNotBlank() && appAnime.titleAr.equals(related.titleAr, ignoreCase = true))
                                }
                                if (found != null) {"""

content = content.replace(old_click, new_click)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
