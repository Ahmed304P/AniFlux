import re

with open('app/src/main/java/com/example/util/AniListService.kt', 'r') as f:
    code = f.read()

# Expanded genreTranslations map including all MAL genres, themes, and demographics
new_genre_map = """    val genreTranslations = mapOf(
        "Action" to "أكشن",
        "Adult Cast" to "شخصيات بالغة",
        "Adventure" to "مغامرات",
        "Anthropomorphic" to "حيوانات مجسمة",
        "Avant Garde" to "طليعي / تجريبي",
        "Award Winning" to "حائز على جوائز",
        "Boys Love" to "ياوي",
        "CGDCT" to "فتيات لطيفات",
        "Childcare" to "رعاية أطفال",
        "Combat Sports" to "رياضات قتالية",
        "Comedy" to "كوميديا",
        "Crossdressing" to "تنكر بالجنس الآخر",
        "Delinquents" to "جانحين",
        "Demons" to "شياطين",
        "Detective" to "تحري",
        "Drama" to "دراما",
        "Ecchi" to "إيتشي",
        "Educational" to "تعليمي",
        "Erotica" to "إيروتيك",
        "Fantasy" to "خيال",
        "Gag Humor" to "فكاهة / مقالب",
        "Game" to "ألعاب",
        "Girls Love" to "يوري",
        "Gore" to "دموي",
        "Gourmet" to "طبخ وطعام",
        "Harem" to "حريم",
        "Hentai" to "هنتاي",
        "High Stakes Game" to "ألعاب الرهان العالي",
        "Historical" to "تاريخي",
        "Horror" to "رعب",
        "Idols Female" to "آيدولز إناث",
        "Idols Male" to "آيدولز ذكور",
        "Isekai" to "عالم موازي (إيسيكاي)",
        "Iyashikei" to "استرخاء وهدوء",
        "Josei" to "جوسي",
        "Kids" to "أطفال",
        "Love Polygon" to "مثلث الحب",
        "Love Status Quo" to "علاقات معقدة",
        "Magical Sex Shift" to "تحول سحري",
        "Magic" to "سحر",
        "Mahou Shoujo" to "فتيات سحرية",
        "Martial Arts" to "فنون قتالية",
        "Mecha" to "آلات وميكا",
        "Medical" to "طبي",
        "Military" to "عسكري",
        "Music" to "موسيقى",
        "Mystery" to "غموض",
        "Mythology" to "أساطير",
        "Organized Crime" to "عصابات وجريمة منظمة",
        "Otaku Culture" to "ثقافة أوتاكو",
        "Parody" to "محاكاة ساخرة",
        "Performing Arts" to "فنون استعراضية",
        "Pets" to "حيوانات أليفة",
        "Police" to "شرطة",
        "Psychological" to "نفسي",
        "Racing" to "سباقات",
        "Reincarnation" to "تناسخ أرواح",
        "Reverse Harem" to "حريم عكسي",
        "Romance" to "رومانسية",
        "Romantic Subtext" to "إيحاء رومانسي",
        "Samurai" to "ساموراي",
        "School" to "مدرسي",
        "Sci-Fi" to "خيال علمي",
        "Seinen" to "سينين (للبالغين)",
        "Shoujo" to "شوجو (للفتيات)",
        "Shounen" to "شونين (للشباب)",
        "Showbiz" to "عالم الشهرة",
        "Slice of Life" to "شريحة من الحياة",
        "Space" to "فضاء",
        "Sports" to "رياضة",
        "Strategy Game" to "ألعاب استراتيجية",
        "Super Power" to "قوى خارقة",
        "Supernatural" to "خارق للطبيعة",
        "Survival" to "نجاة",
        "Suspense" to "تشويق وإثارة",
        "Team Sports" to "رياضات جماعية",
        "Time Travel" to "سفر عبر الزمن",
        "Urban Fantasy" to "خيال حضري",
        "Vampire" to "مصاصي دماء",
        "Video Game" to "ألعاب فيديو",
        "Villainess" to "الشريرة",
        "Visual Arts" to "فنون بصرية",
        "Workplace" to "بيئة عمل"
    )

    /**
     * Translates an English genre/theme/demographic into Arabic reliably.
     */
    suspend fun translateGenreToArabic(rawGenre: String): String {
        val trimmed = rawGenre.trim()
        if (trimmed.isBlank()) return ""
        // Check exact match (case insensitive)
        for ((key, value) in genreTranslations) {
            if (key.equals(trimmed, ignoreCase = true)) {
                return value
            }
        }
        // Fallback to online translation service if unknown genre
        return try {
            val trans = GeminiTranslationService.translateToArabic(trimmed)
            if (trans.isNotBlank() && !trans.startsWith("Translation")) trans else trimmed
        } catch (e: Exception) {
            trimmed
        }
    }"""

# Replace old genreTranslations definition
old_genre_start = code.find("    private val genreTranslations = mapOf(")
old_genre_end = code.find("    suspend fun getAnimeRelations(", old_genre_start)

assert old_genre_start != -1 and old_genre_end != -1, f"Could not find genre map: {old_genre_start}, {old_genre_end}"

code = code[:old_genre_start] + new_genre_map + "\n\n" + code[old_genre_end:]

# Now replace fetchFullMyAnimeListDetails
old_fetch_start = code.find("    suspend fun fetchFullMyAnimeListDetails(item: AniListResult): AniListResult = withContext(Dispatchers.IO) {")
assert old_fetch_start != -1, "fetchFullMyAnimeListDetails not found!"

new_fetch_func = """    suspend fun fetchFullMyAnimeListDetails(item: AniListResult): AniListResult = withContext(Dispatchers.IO) {
        if (item.sourceId.isBlank()) return@withContext item

        val url = "https://myanimelist.net/anime/${item.sourceId}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext item
                val html = response.body?.string() ?: return@withContext item

                // 1. Description / Synopsis
                val descMatch = Regex(\"\"\"<meta property=\"og:description\" content=\"([^\"]+)\"\"\"\").find(html)
                var description = descMatch?.groupValues?.getOrNull(1) ?: \"\"
                description = android.text.Html.fromHtml(description, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()

                // 2. High-res Image
                val imgMatch = Regex(\"\"\"<meta property=\"og:image\" content=\"([^\"]+)\"\"\"\").find(html)
                var fullImg = imgMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: item.posterUrl
                if (fullImg.contains(\"/r/\")) {
                    fullImg = fullImg.replace(Regex(\"\"\"/r/\\d+x\\d+/\"\"\"), \"/\")
                    val qIdx = fullImg.indexOf(\"?\")
                    if (qIdx != -1) fullImg = fullImg.substring(0, qIdx)
                }

                // 3. Genres, Themes & Demographics (100% Arabic)
                val genreSet = linkedSetOf<String>()
                
                // Itemprop=\"genre\" covers all genres, themes, and demographics
                val itemPropMatches = Regex(\"\"\"<span itemprop=\"genre\"[^>]*>([^<]+)</span>\"\"\").findAll(html)
                for (m in itemPropMatches) {
                    val g = m.groupValues[1].trim()
                    if (g.isNotBlank()) {
                        genreSet.add(g)
                    }
                }

                // Also parse any /anime/genre/ links from sidebar
                val genreLinkMatches = Regex(\"\"\"href=\"[^\"]*/anime/genre/\\d+/[^\"]*\"[^>]*>([^<]+)</a>\"\"\").findAll(html)
                for (m in genreLinkMatches) {
                    val g = m.groupValues[1].trim()
                    if (g.isNotBlank()) {
                        genreSet.add(g)
                    }
                }

                // Translate every single genre/theme into Arabic
                val translatedGenres = mutableListOf<String>()
                for (genre in genreSet) {
                    val ar = translateGenreToArabic(genre)
                    if (ar.isNotBlank() && !translatedGenres.contains(ar)) {
                        translatedGenres.add(ar)
                    }
                }

                // 4. Studio
                val studioMatch = Regex(\"\"\"<span class=\"dark_text\">Studios:</span>\\s*<a[^>]*>([^<]+)</a>\"\"\").find(html)
                val studio = studioMatch?.groupValues?.getOrNull(1)?.trim() ?: \"\"

                // 5. Season & Year
                val premieredMatch = Regex(\"\"\"<span class=\"dark_text\">Premiered:</span>\\s*<a[^>]*>(\\w+)\\s*(\\d+)</a>\"\"\").find(html)
                val seasonEn = premieredMatch?.groupValues?.getOrNull(1) ?: \"\"
                val yearStr = premieredMatch?.groupValues?.getOrNull(2) ?: item.year

                val mappedSeason = when (seasonEn.lowercase()) {
                    \"spring\" -> \"ربيع\"
                    \"summer\" -> \"صيف\"
                    \"fall\", \"autumn\" -> \"خريف\"
                    \"winter\" -> \"شتاء\"
                    else -> item.season
                }

                // 6. Trailer URL
                val trailerMatch = Regex(\"\"\"(?:youtube\\.com|youtube-nocookie\\.com)/embed/([a-zA-Z0-9_-]+)\"\"\").find(html)
                val trailerUrl = if (trailerMatch != null) \"https://www.youtube.com/watch?v=${trailerMatch.groupValues[1]}\" else item.trailer

                // 7. Characters Extraction with High-Res Images & Clean Names
                val charsList = mutableListOf<Person>()
                val charRegex = Regex(
                    \"\"\"<a href=\"https://myanimelist.net/character/(\\d+)/[^\"]*\"[^>]*>\\s*<img[^>]*data-src=\"([^\"]+)\"[^>]*>.*?<a href=\"https://myanimelist.net/character/\\1/[^\"]*\">([^<]+)</a>\"\"\",
                    RegexOption.DOT_MATCHES_ALL
                )
                val charMatches = charRegex.findAll(html)
                for (cm in charMatches.take(12)) {
                    val imgRaw = cm.groupValues[2]
                    var cleanImg = imgRaw.replace(Regex(\"\"\"/r/\\d+x\\d+/\"\"\"), \"/\")
                    val qIdx = cleanImg.indexOf(\"?\")
                    if (qIdx != -1) cleanImg = cleanImg.substring(0, qIdx)
                    if (cleanImg.contains(\"questionmark\")) cleanImg = \"\"

                    val rawName = cm.groupValues[3].trim()
                    val formattedName = if (rawName.contains(\",\")) {
                        val parts = rawName.split(\",\").map { it.trim() }
                        if (parts.size > 1) \"${parts[1]} ${parts[0]}\" else parts[0]
                    } else rawName

                    if (formattedName.isNotBlank()) {
                        charsList.add(Person(name = formattedName, imageUrl = cleanImg))
                    }
                }

                // 8. Staff Extraction with High-Res Images & Clean Names
                val staffList = mutableListOf<Person>()
                val staffHeaderIdx = html.indexOf(\"<h2>Staff</h2>\")
                if (staffHeaderIdx != -1) {
                    val staffSection = html.substring(staffHeaderIdx)
                    val staffRegex = Regex(
                        \"\"\"<a href=\"https://myanimelist.net/people/(\\d+)/[^\"]*\"[^>]*>\\s*<img[^>]*data-src=\"([^\"]+)\"[^>]*>.*?<a href=\"https://myanimelist.net/people/\\1/[^\"]*\">([^<]+)</a>.*?<small>([^<]+)</small>\"\"\",
                        RegexOption.DOT_MATCHES_ALL
                    )
                    val staffMatches = staffRegex.findAll(staffSection)
                    for (sm in staffMatches.take(8)) {
                        val imgRaw = sm.groupValues[2]
                        var cleanImg = imgRaw.replace(Regex(\"\"\"/r/\\d+x\\d+/\"\"\"), \"/\")
                        val qIdx = cleanImg.indexOf(\"?\")
                        if (qIdx != -1) cleanImg = cleanImg.substring(0, qIdx)
                        if (cleanImg.contains(\"questionmark\")) cleanImg = \"\"

                        val rawName = sm.groupValues[3].trim()
                        val formattedName = if (rawName.contains(\",\")) {
                            val parts = rawName.split(\",\").map { it.trim() }
                            if (parts.size > 1) \"${parts[1]} ${parts[0]}\" else parts[0]
                        } else rawName

                        val role = sm.groupValues[4].trim()
                        val displayName = if (role.isNotBlank()) \"$formattedName ($role)\" else formattedName

                        if (formattedName.isNotBlank()) {
                            staffList.add(Person(name = displayName, imageUrl = cleanImg))
                        }
                    }
                }

                return@withContext item.copy(
                    synopsisEn = description.ifBlank { item.synopsisEn },
                    posterUrl = fullImg,
                    genres = if (translatedGenres.isNotEmpty()) translatedGenres.joinToString(\", \") else item.genres,
                    studio = studio.ifBlank { item.studio },
                    season = mappedSeason,
                    year = yearStr.ifBlank { item.year },
                    trailer = trailerUrl,
                    charactersList = if (charsList.isNotEmpty()) charsList else item.charactersList,
                    staffList = if (staffList.isNotEmpty()) staffList else item.staffList
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext item
        }
    }
}
"""

code = code[:old_fetch_start] + new_fetch_func

with open('app/src/main/java/com/example/util/AniListService.kt', 'w') as f:
    f.write(code)

print("AniListService updated successfully!")
