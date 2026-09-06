import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

old_filter = """        val available = fetched.filter { related ->
            allAnime.any { appAnime ->
                val appEn = appAnime.titleEn.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                val appAr = appAnime.titleAr.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                val relEn = related.titleEn.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                val relAr = related.titleAr.lowercase().replace(Regex("[^a-z0-9\\\\u0600-\\\\u06FF]"), "")
                
                val matchEn = appEn.isNotEmpty() && relEn.isNotEmpty() && (appEn == relEn || appEn.contains(relEn) || relEn.contains(appEn))
                val matchAr = appAr.isNotEmpty() && relAr.isNotEmpty() && (appAr == relAr || appAr.contains(relAr) || relAr.contains(appAr))
                
                matchEn || matchAr
            }
        }"""

new_filter = """        val available = fetched.filter { related ->
            allAnime.any { appAnime ->
                appAnime.titleEn.equals(related.titleEn, ignoreCase = true) || 
                (appAnime.titleAr.isNotBlank() && appAnime.titleAr.equals(related.titleAr, ignoreCase = true))
            }
        }"""

content = content.replace(old_filter, new_filter)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(content)
