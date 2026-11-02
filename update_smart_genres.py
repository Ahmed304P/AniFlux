import re

with open('app/src/main/java/com/example/util/AniListService.kt', 'r') as f:
    code = f.read()

# Replace the genreTranslations and smart filter logic
old_block_start = code.find("    val genreTranslations = mapOf(")
old_block_end = code.find("    suspend fun getAnimeRelations(", old_block_start)

assert old_block_start != -1 and old_block_end != -1, "Old block not found"

new_smart_engine = """    // Global Standard Anime Genres (used in MyAnimeList, AniList, Crunchyroll, etc.)
    val standardGenres = mapOf(
        "Action" to "أكشن",
        "Adventure" to "مغامرات",
        "Comedy" to "كوميديا",
        "Drama" to "دراما",
        "Fantasy" to "خيال",
        "Horror" to "رعب",
        "Mystery" to "غموض",
        "Psychological" to "نفسي",
        "Romance" to "رومانسية",
        "Sci-Fi" to "خيال علمي",
        "Slice of Life" to "شريحة من الحياة",
        "Sports" to "رياضة",
        "Supernatural" to "خارق للطبيعة",
        "Suspense" to "تشويق وإثارة",
        "Thriller" to "إثارة",
        "Ecchi" to "إيتشي",
        "Gourmet" to "طبخ وطعام",
        "Mecha" to "ميكا وآلات",
        "Music" to "موسيقى",
        "Award Winning" to "حائز على جوائز"
    )

    // Standard Demographics
    val standardDemographics = mapOf(
        "Shounen" to "شونين",
        "Seinen" to "سينين",
        "Shoujo" to "شوجو",
        "Josei" to "جوسي",
        "Kids" to "أطفال"
    )

    // Approved, high-quality themes (clean and globally recognized)
    val approvedThemes = mapOf(
        "School" to "مدرسي",
        "Historical" to "تاريخي",
        "Military" to "عسكري",
        "Super Power" to "قوى خارقة",
        "Martial Arts" to "فنون قتالية",
        "Isekai" to "إيسيكاي",
        "Vampire" to "مصاصي دماء",
        "Space" to "فضاء",
        "Mythology" to "أساطير",
        "Detective" to "تحري",
        "Samurai" to "ساموراي",
        "Time Travel" to "سفر عبر الزمن",
        "Survival" to "نجاة",
        "Gore" to "دموي",
        "Strategy Game" to "ألعاب استراتيجية",
        "Team Sports" to "رياضات جماعية"
    )

    // Full translations map for direct lookups
    val genreTranslations = (standardGenres + standardDemographics + approvedThemes)

    /**
     * Smart Genre Normalizer & Filter Engine.
     * Takes raw genres from AniList/MAL, cleans out weird, niche, or junk tags (like 'Otaku Culture', 'Workplace', etc.),
     * prioritizes primary genres, approved themes, and demographic, ensuring uniform international standards in pure Arabic.
     */
    fun cleanAndFilterGenres(
        primaryGenres: List<String>,
        themes: List<String> = emptyList(),
        demographics: List<String> = emptyList()
    ): List<String> {
        val result = mutableListOf<String>()

        // 1. Add primary genres first (highest priority)
        for (raw in primaryGenres) {
            val trimmed = raw.trim()
            val ar = standardGenres.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
                ?: genreTranslations.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
            if (ar != null && !result.contains(ar)) {
                result.add(ar)
            }
        }

        // 2. Add approved themes (skip obscure or junk themes)
        for (raw in themes) {
            val trimmed = raw.trim()
            val ar = approvedThemes.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
            if (ar != null && !result.contains(ar)) {
                result.add(ar)
            }
        }

        // 3. Add demographic (e.g. Shounen, Seinen)
        for (raw in demographics) {
            val trimmed = raw.trim()
            val ar = standardDemographics.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
            if (ar != null && !result.contains(ar)) {
                result.add(ar)
            }
        }

        // Fallback: If result is completely empty, try matching against any known genre
        if (result.isEmpty()) {
            for (raw in primaryGenres + themes + demographics) {
                val trimmed = raw.trim()
                val ar = genreTranslations.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
                if (ar != null && !result.contains(ar)) {
                    result.add(ar)
                }
            }
        }

        return result
    }

    /**
     * Translates an English genre/theme/demographic into Arabic reliably.
     */
    suspend fun translateGenreToArabic(rawGenre: String): String {
        val trimmed = rawGenre.trim()
        if (trimmed.isBlank()) return ""
        val found = genreTranslations.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        if (found != null) return found

        return try {
            val trans = GeminiTranslationService.translateToArabic(trimmed)
            if (trans.isNotBlank() && !trans.startsWith("Translation")) trans else trimmed
        } catch (e: Exception) {
            trimmed
        }
    }"""

code = code[:old_block_start] + new_smart_engine + "\n\n" + code[old_block_end:]

# Now update AniList media search parsing (lines around 405)
code = re.sub(
    r'val genresArray = item\.optJSONArray\("genres"\)[\s\S]*?val formattedScore =',
    '''val genresArray = item.optJSONArray("genres")
                    val rawAniGenres = mutableListOf<String>()
                    if (genresArray != null) {
                        for (j in 0 until genresArray.length()) {
                            val g = genresArray.optString(j, "").trim()
                            if (g.isNotBlank()) rawAniGenres.add(g)
                        }
                    }
                    val genresList = cleanAndFilterGenres(rawAniGenres)
                    
                    val formattedScore =''',
    code
)

# Now update fetchFullMyAnimeListDetails in code
# Look for section 3 in fetchFullMyAnimeListDetails
old_section3_start = code.find("                // 3. Genres, Themes & Demographics (100% Arabic)")
old_section3_end = code.find("                // 4. Studio", old_section3_start)

assert old_section3_start != -1 and old_section3_end != -1, "MAL section 3 not found"

new_section3 = """                // 3. Genres, Themes & Demographics with Smart Filtering & Standardization
                val rawGenres = mutableListOf<String>()
                val rawThemes = mutableListOf<String>()
                val rawDemographics = mutableListOf<String>()

                // Extract exact categorized rows from MAL sidebar
                val categoryRegex = Regex(\"\"\"<span class=\"dark_text\">(Genres?|Explicit Genres?|Themes?|Demographics?):</span>(.*?)(?:</div>|<span class=\"dark_text\")\"\"\", RegexOption.DOT_MATCHES_ALL)
                val catMatches = categoryRegex.findAll(html)
                for (cm in catMatches) {
                    val categoryName = cm.groupValues[1].lowercase()
                    val content = cm.groupValues[2]
                    val linkMatches = Regex(\"\"\"<a[^>]*>([^<]+)</a>\"\"\").findAll(content)
                    val items = linkMatches.map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()

                    if (categoryName.startsWith("genre")) {
                        rawGenres.addAll(items)
                    } else if (categoryName.startsWith("theme")) {
                        rawThemes.addAll(items)
                    } else if (categoryName.startsWith("demographic")) {
                        rawDemographics.addAll(items)
                    }
                }

                // If sidebar category parsing was empty, fallback to itemprop="genre"
                if (rawGenres.isEmpty() && rawThemes.isEmpty() && rawDemographics.isEmpty()) {
                    val itemPropMatches = Regex(\"\"\"<span itemprop=\"genre\"[^>]*>([^<]+)</span>\"\"\").findAll(html)
                    for (m in itemPropMatches) {
                        val g = m.groupValues[1].trim()
                        if (g.isNotBlank()) rawGenres.add(g)
                    }
                }

                // Apply the Smart Genre Engine
                val cleanedGenres = cleanAndFilterGenres(
                    primaryGenres = rawGenres,
                    themes = rawThemes,
                    demographics = rawDemographics
                )
"""

code = code[:old_section3_start] + new_section3 + code[old_section3_end:]

# In fetchFullMyAnimeListDetails return statement, replace translatedGenres with cleanedGenres
code = code.replace(
    "genres = if (translatedGenres.isNotEmpty()) translatedGenres.joinToString(\", \") else item.genres,",
    "genres = if (cleanedGenres.isNotEmpty()) cleanedGenres.joinToString(\", \") else item.genres,"
)

with open('app/src/main/java/com/example/util/AniListService.kt', 'w') as f:
    f.write(code)

print("Smart Genre Engine successfully integrated into AniListService!")
