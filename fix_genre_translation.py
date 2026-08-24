import re

with open("app/src/main/java/com/example/util/AniListService.kt", "r") as f:
    content = f.read()

translations_map = """    private val genreTranslations = mapOf(
        "Action" to "أكشن",
        "Adventure" to "مغامرات",
        "Comedy" to "كوميديا",
        "Drama" to "دراما",
        "Ecchi" to "إيتشي",
        "Fantasy" to "خيال",
        "Horror" to "رعب",
        "Mahou Shoujo" to "سحر",
        "Mecha" to "آلات",
        "Music" to "موسيقى",
        "Mystery" to "غموض",
        "Psychological" to "نفسي",
        "Romance" to "رومانسي",
        "Sci-Fi" to "خيال علمي",
        "Slice of Life" to "شريحة من الحياة",
        "Sports" to "رياضة",
        "Supernatural" to "خارق للطبيعة",
        "Thriller" to "إثارة",
        "Isekai" to "إيسيكاي",
        "Boys Love" to "حب أولاد",
        "Girls Love" to "حب فتيات",
        "Gore" to "دموي"
    )

    suspend fun searchAnime"""

content = content.replace("    suspend fun searchAnime", translations_map)

genres_replacement = """                    val genresArray = item.optJSONArray("genres")
                    val genresList = mutableListOf<String>()
                    if (genresArray != null) {
                        for (j in 0 until genresArray.length()) {
                            val englishGenre = genresArray.getString(j)
                            genresList.add(genreTranslations[englishGenre] ?: englishGenre)
                        }
                    }"""

content = re.sub(r'                    val genresArray = item\.optJSONArray\("genres"\)\n                    val genresList = mutableListOf<String>\(\)\n                    if \(genresArray != null\) \{\n                        for \(j in 0 until genresArray\.length\(\)\) genresList\.add\(genresArray\.getString\(j\)\)\n                    \}', genres_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/util/AniListService.kt", "w") as f:
    f.write(content)

