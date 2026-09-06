import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

# 1. Update RelatedAnimeSection signature and logic
old_section = """fun RelatedAnimeSection(anime: Anime, onRelatedClick: (RelatedAnime) -> Unit) {
    var relatedAnimeList by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        relatedAnimeList = null
        relatedAnimeList = AniListService.getAnimeRelations(anime.titleEn)
    }"""

new_section = """fun RelatedAnimeSection(anime: Anime, allAnime: List<Anime>, onRelatedClick: (RelatedAnime) -> Unit) {
    var relatedAnimeList by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        relatedAnimeList = null
        val fetched = AniListService.getAnimeRelations(anime.titleEn)
        // Filter out anime that are NOT added in the app (i.e. check if title matches any in allAnime)
        val available = fetched.filter { related ->
            allAnime.any { it.titleEn.equals(related.titleEn, ignoreCase = true) || it.titleAr.equals(related.titleAr, ignoreCase = true) }
        }
        relatedAnimeList = available
    }"""

content = content.replace(old_section, new_section)

# 2. Update RelatedAnimeSection call in AnimeDetailScreen
content = content.replace("RelatedAnimeSection(anime, onRelatedAnimeClick)", "RelatedAnimeSection(anime, allAnime, onRelatedAnimeClick)")

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(content)
