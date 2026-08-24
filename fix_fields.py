import re

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    content = f.read()

effect_replacement = """
    LaunchedEffect(animeId) {
        currentAnimeId = animeId
        if (animeId != null) {
            val existingAnime = viewModel.allAnime.value.find { it.id == animeId }
            if (existingAnime != null) {
                titleAr = existingAnime.titleAr
                titleEn = existingAnime.titleEn
                posterUrl = existingAnime.posterUrl
                bannerUrl = existingAnime.bannerUrl
                synopsisAr = existingAnime.synopsisAr
                status = existingAnime.status
                type = existingAnime.type
                year = existingAnime.year.toString()
                studio = existingAnime.studio
                genres = existingAnime.genres.joinToString(", ")
                rating = existingAnime.rating.toString()
                ageRating = existingAnime.ageRating
                season = existingAnime.season
                characters = existingAnime.characters
                staff = existingAnime.staff
                trailerUrl = existingAnime.trailerUrl
                isCommentsLocked = existingAnime.isCommentsLocked
            }
        } else {
            titleAr = ""
            titleEn = ""
            posterUrl = ""
            bannerUrl = ""
            synopsisAr = ""
            status = "مستمر"
            type = "TV"
            year = ""
            studio = ""
            genres = ""
            rating = ""
            ageRating = "+13"
            season = "ربيع"
            characters = emptyList()
            staff = emptyList()
            trailerUrl = ""
            isCommentsLocked = false
        }
    }
"""

content = re.sub(r'LaunchedEffect\(animeId\) \{.*?(?=\n    Scaffold)', effect_replacement.strip() + "\n\n", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(content)
