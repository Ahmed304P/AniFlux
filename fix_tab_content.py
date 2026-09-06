import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "0 -> AnimeDetailsTabContent(anime = anime, isSaved = isSaved, savedStatus = savedStatus, userRating = userRating, onOpenMyListOptions = { showMyListSheet = true }, onRateClick = { showRatingDialog = true }, onGenreClick = onGenreClick, onStudioClick = onStudioClick, lazyListState = detailsListState)",
    "0 -> AnimeDetailsTabContent(anime = anime, isSaved = isSaved, savedStatus = savedStatus, userRating = userRating, onOpenMyListOptions = { showMyListSheet = true }, onRateClick = { showRatingDialog = true }, onGenreClick = onGenreClick, onStudioClick = onStudioClick, lazyListState = detailsListState, allAnime = allAnime, onRelatedAnimeClick = onRelatedAnimeClick, onRecommendationClick = onRecommendationClick)"
)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w') as f:
    f.write(content)
