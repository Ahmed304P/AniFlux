import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

old_block = """@Composable
fun RelatedAnimeSection(anime: Anime, allAnime: List<Anime>, onRelatedClick: (RelatedAnime) -> Unit) {
    var relatedAnimeList by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        relatedAnimeList = null
        val fetched = AniListService.getAnimeRelations(anime.titleEn)
        // Filter out anime that are NOT added in the app (i.e. check if title matches any in allAnime)
        val available = fetched.filter { related ->
            allAnime.any { appAnime ->
                appAnime.titleEn.equals(related.titleEn, ignoreCase = true) || 
                (appAnime.titleAr.isNotBlank() && appAnime.titleAr.equals(related.titleAr, ignoreCase = true))
            }
        }
        relatedAnimeList = available
    }
    
    if (relatedAnimeList == null) {"""

new_block = """@Composable
fun RelatedAnimeSection(anime: Anime, allAnime: List<Anime>, onRelatedClick: (RelatedAnime) -> Unit) {
    var fetchedRelations by remember { mutableStateOf<List<RelatedAnime>?>(null) }
    
    LaunchedEffect(anime.titleEn) {
        fetchedRelations = null
        fetchedRelations = AniListService.getAnimeRelations(anime.titleEn)
    }

    val relatedAnimeList = remember(fetchedRelations, allAnime) {
        fetchedRelations?.filter { related ->
            allAnime.any { appAnime ->
                appAnime.titleEn.equals(related.titleEn, ignoreCase = true) || 
                (appAnime.titleAr.isNotBlank() && appAnime.titleAr.equals(related.titleAr, ignoreCase = true))
            }
        }
    }
    
    if (relatedAnimeList == null) {"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(content)
