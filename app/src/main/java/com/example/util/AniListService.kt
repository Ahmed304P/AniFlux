package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import com.example.data.model.Person

data class RelatedAnime(
    val titleEn: String,
    val titleAr: String,
    val posterUrl: String,
    val relationType: String,
    val relationTypeAr: String,
    val rating: String,
    val status: String,
    val format: String = ""
)

data class AniListResult(
    val titleEn: String,
    val titleAr: String,
    val synopsisEn: String = "",
    val posterUrl: String,
    val bannerUrl: String,
    val genres: String,
    val rating: String,
    val status: String,
    val format: String,
    val season: String,
    val year: String,
    val charactersList: List<Person>,
    val staffList: List<Person>,
    val trailer: String,
    val studio: String = "",
    val source: String = "AniList", // "AniList" or "MyAnimeList"
    val sourceId: String = ""
)

object AniListService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val ANILIST_URL = "https://graphql.anilist.co"
    private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 AniFlux/3.3"

    // Global Standard Anime Genres (used in MyAnimeList, AniList, Crunchyroll, etc.)
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
        "Psychological" to "نفسي",
        "Magic" to "سحر",
        "Mahou Shoujo" to "سحر",
        "Strategy Game" to "ألعاب استراتيجية",
        "Team Sports" to "رياضات جماعية"
    )

    // Full translations map for direct lookups
    val genreTranslations = (standardGenres + standardDemographics + approvedThemes)

    val arabicSynonyms = mapOf(
        "رومانسي" to "رومانسية",
        "مدرسة" to "مدرسي",
        "سنين" to "سينين",
        "سينين (للبالغين)" to "سينين",
        "شوجو (للفتيات)" to "شوجو",
        "شونين (للشباب)" to "شونين",
        "خوارق" to "خارق للطبيعة",
        "خارق" to "خارق للطبيعة",
        "قوة خارقة" to "قوى خارقة",
        "قوى خارقه" to "قوى خارقة",
        "مصاص دماء" to "مصاصي دماء",
        "مصاصي الدماء" to "مصاصي دماء",
        "سفر في الزمن" to "سفر عبر الزمن",
        "سفر في الوقت" to "سفر عبر الزمن",
        "فنون حرب" to "فنون قتالية",
        "قتال" to "فنون قتالية",
        "شريحه من الحياه" to "شريحة من الحياة",
        "شريحة الحياة" to "شريحة من الحياة",
        "خيال علمي" to "خيال علمي",
        "تشويق واثارة" to "تشويق وإثارة",
        "تشويق" to "تشويق وإثارة",
        "اثارة" to "إثارة",
        "كوميدي" to "كوميديا",
        "ايتشي" to "إيتشي",
        "العاب استراتيجية" to "ألعاب استراتيجية",
        "العاب استراتيجيه" to "ألعاب استراتيجية",
        "العاب" to "ألعاب استراتيجية",
        "رياضات جماعية" to "رياضات جماعية",
        "رياضي" to "رياضة"
    )

    val junkTags = setOf(
        "otaku culture", "ثقافة أوتاكو", "ثقافة الاوتاكو", "اوتاكو", "أوتاكو",
        "workplace", "بيئة عمل",
        "adult cast", "شخصيات بالغة",
        "anthropomorphic", "حيوانات مجسمة",
        "parody", "محاكاة ساخرة",
        "idols (female)", "idols (male)", "آيدولز", "ايدولز",
        "visual arts", "فنون بصرية",
        "showbiz", "عالم الترفيه",
        "love polygon", "مثلث حب",
        "gag humor", "كوميديا هزلية",
        "iyashikei", "شفاء نفسي",
        "childcare", "رعاية أطفال",
        "cgp", "cgp"
    )

    /**
     * Normalizes a single genre tag (English or Arabic) to standard Arabic name,
     * discarding junk tags and matching synonyms.
     */
    fun normalizeGenre(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase()

        // 1. Discard junk tags
        if (junkTags.contains(lower) || junkTags.any { lower == it || lower.contains(it) }) {
            return null
        }

        // 2. Direct Arabic value match in any of standard maps
        val directArabic = standardGenres.values.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            ?: approvedThemes.values.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            ?: standardDemographics.values.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (directArabic != null) return directArabic

        // 3. Arabic synonym match
        val syn = arabicSynonyms[trimmed] ?: arabicSynonyms[lower]
        if (syn != null) return syn

        // 4. English key exact match
        val trans = genreTranslations.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        if (trans != null) return trans

        // 5. English partial match (e.g. "School Life" -> "School")
        val partial = genreTranslations.entries.firstOrNull {
            lower.contains(it.key.lowercase()) || it.key.lowercase().contains(lower)
        }?.value
        if (partial != null) return partial

        // 6. If it contains Arabic characters and is not junk, preserve it!
        if (trimmed.any { it in '\u0600'..'\u06FF' }) {
            return trimmed
        }

        return null
    }

    /**
     * Smart Genre Normalizer & Filter Engine.
     * Takes raw genres from AniList/MAL or Arabic input, cleans out weird, niche, or junk tags (like 'Otaku Culture'),
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
            val norm = normalizeGenre(raw)
            if (norm != null && !result.contains(norm)) {
                result.add(norm)
            }
        }

        // 2. Add approved themes (skip obscure or junk themes)
        for (raw in themes) {
            val norm = normalizeGenre(raw)
            if (norm != null && !result.contains(norm)) {
                result.add(norm)
            }
        }

        // 3. Add demographic (e.g. Shounen, Seinen)
        for (raw in demographics) {
            val norm = normalizeGenre(raw)
            if (norm != null && !result.contains(norm)) {
                result.add(norm)
            }
        }

        // 4. Fallback: If result is completely empty, try matching against any known genre
        if (result.isEmpty()) {
            for (raw in primaryGenres + themes + demographics) {
                val trimmed = raw.trim()
                if (trimmed.isNotBlank() && !junkTags.contains(trimmed.lowercase())) {
                    val norm = normalizeGenre(trimmed) ?: if (trimmed.any { it in '\u0600'..'\u06FF' }) trimmed else null
                    if (norm != null && !result.contains(norm)) {
                        result.add(norm)
                    }
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
    }

    suspend fun getAnimeRelations(query: String): List<RelatedAnime> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val graphqlQuery = """
            query (${'$'}search: String) {
              Media(search: ${'$'}search, type: ANIME) {
                relations {
                  edges {
                    relationType
                    node {
                      title {
                        english
                        romaji
                      }
                      coverImage {
                        large
                      }
                      averageScore
                      status
                      format
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", graphqlQuery)
            put("variables", JSONObject().apply { put("search", trimmed) })
        }
        val request = Request.Builder()
            .url(ANILIST_URL)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val relationsEdges = json.optJSONObject("data")?.optJSONObject("Media")?.optJSONObject("relations")?.optJSONArray("edges")
                    ?: return@withContext emptyList()
                
                val results = mutableListOf<RelatedAnime>()
                for (i in 0 until relationsEdges.length()) {
                    val edge = relationsEdges.optJSONObject(i) ?: continue
                    val relationType = edge.optString("relationType", "")
                    val node = edge.optJSONObject("node") ?: continue
                    
                    val titleObj = node.optJSONObject("title")
                    val enTitle = titleObj?.optString("english").takeUnless { it.isNullOrBlank() || it == "null" }
                        ?: titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } 
                        ?: "Unknown Title"
                    val romajiTitle = titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } ?: enTitle
                    
                    val cover = node.optJSONObject("coverImage")?.optString("large", "") ?: ""
                    
                    val score = node.optInt("averageScore", 0)
                    val formattedScore = if (score > 0) (score / 10.0).toString() else ""
                    
                    val status = node.optString("status", "")
                    val format = node.optString("format", "")
                    
                    if (format == "MANGA" || format == "NOVEL" || format == "ONE_SHOT") continue
                    
                    val mappedStatus = when (status) {
                        "FINISHED" -> "مكتمل"
                        "RELEASING" -> "مستمر"
                        "NOT_YET_RELEASED" -> "قريباً"
                        else -> "مستمر"
                    }
                    
                    val mappedRelation = when (relationType) {
                        "SEQUEL" -> "التالي"
                        "PREQUEL" -> "السابق"
                        "SIDE_STORY" -> "قصة جانبية"
                        "SPIN_OFF" -> "فرعي"
                        "SUMMARY" -> "ملخص"
                        "ALTERNATIVE" -> when (format) {
                            "MOVIE" -> "فيلم"
                            "SPECIAL" -> "حلقة خاصة"
                            "OVA" -> "أوفا"
                            "ONA" -> "أونا"
                            else -> "بديل"
                        }
                        else -> when (format) {
                            "MOVIE" -> "فيلم"
                            "SPECIAL" -> "حلقة خاصة"
                            "OVA" -> "أوفا"
                            "ONA" -> "أونا"
                            else -> "مرتبط"
                        }
                    }
                    
                    results.add(RelatedAnime(
                        titleEn = enTitle,
                        titleAr = romajiTitle,
                        posterUrl = cover,
                        relationType = relationType,
                        relationTypeAr = mappedRelation,
                        rating = formattedScore,
                        status = mappedStatus,
                        format = format
                    ))
                }
                
                results.sortWith(compareBy<RelatedAnime> { 
                    when (it.relationType) {
                        "SEQUEL" -> 0
                        "PREQUEL" -> 1
                        "SIDE_STORY" -> 2
                        "SPIN_OFF" -> 3
                        "ALTERNATIVE" -> 4
                        else -> 5
                    }
                })
                
                return@withContext results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Search AniList with full details, safe browser headers, and robust parsing.
     */
    suspend fun searchAniList(query: String): List<AniListResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val graphqlQuery = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 8) {
                media(search: ${'$'}search, type: ANIME) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  description
                  coverImage {
                    extraLarge
                    large
                  }
                  bannerImage
                  genres
                  averageScore
                  status
                  format
                  season
                  seasonYear
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                  characters(sort: ROLE, perPage: 5) {
                    edges {
                      node {
                        name {
                          full
                        }
                        image {
                          large
                        }
                      }
                      role
                    }
                  }
                  staff(perPage: 3) {
                    edges {
                      node {
                        name {
                          full
                        }
                        image {
                          large
                        }
                      }
                      role
                    }
                  }
                  trailer {
                    id
                    site
                  }
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", graphqlQuery)
            put("variables", JSONObject().apply { put("search", trimmed) })
        }

        val request = Request.Builder()
            .url(ANILIST_URL)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val mediaArray = json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
                    ?: return@withContext emptyList()

                val results = mutableListOf<AniListResult>()

                for (i in 0 until mediaArray.length()) {
                    val item = mediaArray.optJSONObject(i) ?: continue
                    val id = item.optInt("id", 0).toString()
                    
                    val titleObj = item.optJSONObject("title")
                    val enTitle = titleObj?.optString("english").takeUnless { it.isNullOrBlank() || it == "null" }
                        ?: titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } 
                        ?: "Unknown Title"
                    
                    val romajiTitle = titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } ?: enTitle
                    
                    val description = item.optString("description", "")
                        .replace("<br>", "\n").replace(Regex("<[^>]*>"), "").trim()
                    
                    val cover = item.optJSONObject("coverImage")?.optString("extraLarge", "")
                        ?.takeIf { it.isNotBlank() }
                        ?: item.optJSONObject("coverImage")?.optString("large", "") ?: ""
                    val banner = item.optString("bannerImage", "")
                    
                    val genresArray = item.optJSONArray("genres")
                    val rawAniGenres = mutableListOf<String>()
                    if (genresArray != null) {
                        for (j in 0 until genresArray.length()) {
                            val g = genresArray.optString(j, "").trim()
                            if (g.isNotBlank()) rawAniGenres.add(g)
                        }
                    }
                    val genresList = cleanAndFilterGenres(rawAniGenres)
                    
                    val score = item.optInt("averageScore", 0)
                    val formattedScore = if (score > 0) (score / 10.0).toString() else ""
                    
                    val status = item.optString("status", "")
                    val mappedStatus = when (status) {
                        "FINISHED" -> "مكتمل"
                        "RELEASING" -> "مستمر"
                        "NOT_YET_RELEASED" -> "قريباً"
                        else -> "مستمر"
                    }
                    
                    val format = item.optString("format", "TV")
                    val mappedFormat = when (format) {
                        "TV" -> "TV"
                        "MOVIE" -> "Movie"
                        "OVA" -> "OVA"
                        "ONA" -> "ONA"
                        "SPECIAL" -> "Special"
                        else -> "TV"
                    }
                    
                    val season = item.optString("season", "")
                    val mappedSeason = when (season) {
                        "SPRING" -> "ربيع"
                        "SUMMER" -> "صيف"
                        "FALL" -> "خريف"
                        "WINTER" -> "شتاء"
                        else -> "ربيع"
                    }
                    
                    val year = item.optInt("seasonYear", 0).let { if (it > 0) it.toString() else "" }

                    // Parse Studio
                    val studiosNodes = item.optJSONObject("studios")?.optJSONArray("nodes")
                    var studioName = ""
                    if (studiosNodes != null && studiosNodes.length() > 0) {
                        studioName = studiosNodes.optJSONObject(0)?.optString("name", "") ?: ""
                    }
                    
                    // Parse Characters
                    val charsEdges = item.optJSONObject("characters")?.optJSONArray("edges")
                    val charsList = mutableListOf<Person>()
                    if (charsEdges != null) {
                        for (j in 0 until charsEdges.length()) {
                            val edge = charsEdges.optJSONObject(j)
                            val name = edge?.optJSONObject("node")?.optJSONObject("name")?.optString("full", "")
                            val image = edge?.optJSONObject("node")?.optJSONObject("image")?.optString("large", "")
                            if (!name.isNullOrBlank()) {
                                charsList.add(Person(name = name, imageUrl = image ?: ""))
                            }
                        }
                    }
                    
                    // Parse Staff
                    val staffEdges = item.optJSONObject("staff")?.optJSONArray("edges")
                    val staffList = mutableListOf<Person>()
                    if (staffEdges != null) {
                        for (j in 0 until staffEdges.length()) {
                            val edge = staffEdges.optJSONObject(j)
                            val name = edge?.optJSONObject("node")?.optJSONObject("name")?.optString("full", "")
                            val image = edge?.optJSONObject("node")?.optJSONObject("image")?.optString("large", "")
                            if (!name.isNullOrBlank()) {
                                staffList.add(Person(name = name, imageUrl = image ?: ""))
                            }
                        }
                    }
                    
                    // Trailer
                    val trailerObj = item.optJSONObject("trailer")
                    val site = trailerObj?.optString("site")
                    val trailerId = trailerObj?.optString("id")
                    val trailerUrl = if (site == "youtube" && !trailerId.isNullOrBlank()) {
                        "https://www.youtube.com/watch?v=${trailerId}"
                    } else ""
                    
                    results.add(
                        AniListResult(
                            titleEn = enTitle,
                            titleAr = romajiTitle,
                            synopsisEn = description,
                            posterUrl = cover,
                            bannerUrl = banner,
                            genres = genresList.joinToString(", "),
                            rating = formattedScore,
                            status = mappedStatus,
                            format = mappedFormat,
                            season = mappedSeason,
                            year = year,
                            charactersList = charsList,
                            staffList = staffList,
                            trailer = trailerUrl,
                            studio = studioName,
                            source = "AniList",
                            sourceId = id
                        )
                    )
                }

                return@withContext results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Search MyAnimeList directly via official prefix search API.
     */
    suspend fun searchMyAnimeList(query: String): List<AniListResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://myanimelist.net/search/prefix.json?type=anime&keyword=$encoded&v=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val categories = json.optJSONArray("categories") ?: return@withContext emptyList()
                val results = mutableListOf<AniListResult>()

                for (c in 0 until categories.length()) {
                    val cat = categories.optJSONObject(c) ?: continue
                    if (cat.optString("type") != "anime") continue
                    val items = cat.optJSONArray("items") ?: continue

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optInt("id", 0).toString()
                        val name = item.optString("name", "Unknown Anime")
                        
                        var imageUrl = item.optString("image_url", "")
                        // Convert thumbnail to high resolution image
                        if (imageUrl.contains("/r/")) {
                            imageUrl = imageUrl.replace(Regex("""/r/\d+x\d+/"""), "/")
                            val qIdx = imageUrl.indexOf("?")
                            if (qIdx != -1) imageUrl = imageUrl.substring(0, qIdx)
                        }

                        val payload = item.optJSONObject("payload")
                        val mediaType = payload?.optString("media_type", "TV") ?: "TV"
                        val startYear = payload?.optInt("start_year", 0)?.let { if (it > 0) it.toString() else "" } ?: ""
                        val score = payload?.optString("score", "")?.takeUnless { it == "N/A" || it == "null" } ?: ""
                        val status = payload?.optString("status", "") ?: ""

                        val mappedStatus = when {
                            status.contains("Finished", ignoreCase = true) -> "مكتمل"
                            status.contains("Currently", ignoreCase = true) -> "مستمر"
                            status.contains("Not yet", ignoreCase = true) -> "قريباً"
                            else -> "مستمر"
                        }

                        val mappedFormat = when (mediaType.uppercase()) {
                            "MOVIE" -> "Movie"
                            "OVA" -> "OVA"
                            "ONA" -> "ONA"
                            "SPECIAL", "TV SPECIAL" -> "Special"
                            else -> "TV"
                        }

                        results.add(
                            AniListResult(
                                titleEn = name,
                                titleAr = name,
                                synopsisEn = "",
                                posterUrl = imageUrl,
                                bannerUrl = "",
                                genres = "",
                                rating = score,
                                status = mappedStatus,
                                format = mappedFormat,
                                season = "ربيع",
                                year = startYear,
                                charactersList = emptyList(),
                                staffList = emptyList(),
                                trailer = "",
                                studio = "",
                                source = "MyAnimeList",
                                sourceId = id
                            )
                        )
                    }
                }
                return@withContext results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Unified search supporting AniList, MyAnimeList, or combined parallel queries.
     */
    suspend fun searchAnime(query: String, sourceFilter: String = "ALL"): List<AniListResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        when (sourceFilter.uppercase()) {
            "ANILIST" -> searchAniList(trimmed)
            "MYANIMELIST" -> searchMyAnimeList(trimmed)
            else -> coroutineScope {
                val aniDeferred = async { searchAniList(trimmed) }
                val malDeferred = async { searchMyAnimeList(trimmed) }

                val aniResults = aniDeferred.await()
                var malResults = malDeferred.await()

                // Smart Arabic bridge: If search was Arabic and MAL gave 0, use AniList's mapped title for MAL!
                val hasArabic = trimmed.any { it in '\u0600'..'\u06FF' }
                if (malResults.isEmpty() && aniResults.isNotEmpty() && hasArabic) {
                    val topTitle = aniResults.first().titleEn
                    if (topTitle.isNotBlank()) {
                        malResults = searchMyAnimeList(topTitle)
                    }
                }

                // Interleave results from both sources with AniList and MyAnimeList badges
                val combined = mutableListOf<AniListResult>()
                val maxLen = maxOf(aniResults.size, malResults.size)
                for (i in 0 until maxLen) {
                    if (i < aniResults.size) combined.add(aniResults[i])
                    if (i < malResults.size) combined.add(malResults[i])
                }
                combined
            }
        }
    }

    /**
     * Fetches detailed synopsis, genres, studio, season, and trailer for MyAnimeList entries.
     */
    suspend fun fetchFullMyAnimeListDetails(item: AniListResult): AniListResult = withContext(Dispatchers.IO) {
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
                val descMatch = Regex("""<meta property="og:description" content="([^"]+)"""").find(html)
                var description = descMatch?.groupValues?.getOrNull(1) ?: ""
                description = android.text.Html.fromHtml(description, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()

                // 2. High-res Image
                val imgMatch = Regex("""<meta property="og:image" content="([^"]+)"""").find(html)
                var fullImg = imgMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: item.posterUrl
                if (fullImg.contains("/r/")) {
                    fullImg = fullImg.replace(Regex("""/r/\d+x\d+/"""), "/")
                    val qIdx = fullImg.indexOf("?")
                    if (qIdx != -1) fullImg = fullImg.substring(0, qIdx)
                }

                // 3. Genres, Themes & Demographics with Smart Filtering & Standardization
                val rawGenres = mutableListOf<String>()
                val rawThemes = mutableListOf<String>()
                val rawDemographics = mutableListOf<String>()

                // Extract exact categorized rows from MAL sidebar
                val categoryRegex = Regex("""<span class="dark_text">(Genres?|Explicit Genres?|Themes?|Demographics?):</span>(.*?)(?:</div>|<span class="dark_text")""", RegexOption.DOT_MATCHES_ALL)
                val catMatches = categoryRegex.findAll(html)
                for (cm in catMatches) {
                    val categoryName = cm.groupValues[1].lowercase()
                    val content = cm.groupValues[2]
                    val linkMatches = Regex("""<a[^>]*>([^<]+)</a>""").findAll(content)
                    val items = linkMatches.map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()

                    if (categoryName.startsWith("genre")) {
                        rawGenres.addAll(items)
                    } else if (categoryName.startsWith("theme")) {
                        rawThemes.addAll(items)
                    } else if (categoryName.startsWith("demographic")) {
                        rawDemographics.addAll(items)
                    }
                }

                // If sidebar category parsing was empty, fallback 1: itemprop="genre"
                if (rawGenres.isEmpty() && rawThemes.isEmpty() && rawDemographics.isEmpty()) {
                    val itemPropMatches = Regex("""<span itemprop="genre"[^>]*>([^<]+)</span>""").findAll(html)
                    for (m in itemPropMatches) {
                        val g = m.groupValues[1].trim()
                        if (g.isNotBlank()) rawGenres.add(g)
                    }
                }

                // Fallback 2: direct anime genre links
                if (rawGenres.isEmpty() && rawThemes.isEmpty() && rawDemographics.isEmpty()) {
                    val linkMatches = Regex("""<a href="[^"]*/anime/genre/\d+/[^"]*"[^>]*>([^<]+)</a>""").findAll(html)
                    for (m in linkMatches) {
                        val g = m.groupValues[1].trim()
                        if (g.isNotBlank()) rawGenres.add(g)
                    }
                }

                // Apply the Smart Genre Engine
                var cleanedGenres = cleanAndFilterGenres(
                    primaryGenres = rawGenres,
                    themes = rawThemes,
                    demographics = rawDemographics
                )

                // Fallback 3: If still empty, search AniList directly by title to grab official genres!
                if (cleanedGenres.isEmpty() && item.titleEn.isNotBlank()) {
                    try {
                        val aniFallback = searchAniList(item.titleEn).firstOrNull()
                        if (aniFallback != null && aniFallback.genres.isNotBlank()) {
                            cleanedGenres = aniFallback.genres.split(Regex("[,،]")).map { it.trim() }.filter { it.isNotBlank() }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // 4. Studio
                val studioMatch = Regex("""<span class="dark_text">Studios:</span>\s*<a[^>]*>([^<]+)</a>""").find(html)
                val studio = studioMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

                // 5. Season & Year
                val premieredMatch = Regex("""<span class="dark_text">Premiered:</span>\s*<a[^>]*>(\w+)\s*(\d+)</a>""").find(html)
                val seasonEn = premieredMatch?.groupValues?.getOrNull(1) ?: ""
                val yearStr = premieredMatch?.groupValues?.getOrNull(2) ?: item.year

                val mappedSeason = when (seasonEn.lowercase()) {
                    "spring" -> "ربيع"
                    "summer" -> "صيف"
                    "fall", "autumn" -> "خريف"
                    "winter" -> "شتاء"
                    else -> item.season
                }

                // 6. Trailer URL
                val trailerMatch = Regex("""(?:youtube\.com|youtube-nocookie\.com)/embed/([a-zA-Z0-9_-]+)""").find(html)
                val trailerUrl = if (trailerMatch != null) "https://www.youtube.com/watch?v=${trailerMatch.groupValues[1]}" else item.trailer

                // 7. Characters Extraction with High-Res Images & Clean Names
                val charsList = mutableListOf<Person>()
                val charRegex = Regex(
                    """<a href="https://myanimelist.net/character/(\d+)/[^"]*"[^>]*>\s*<img[^>]*data-src="([^"]+)"[^>]*>.*?<a href="https://myanimelist.net/character/\1/[^"]*">([^<]+)</a>""",
                    RegexOption.DOT_MATCHES_ALL
                )
                val charMatches = charRegex.findAll(html)
                for (cm in charMatches.take(12)) {
                    val imgRaw = cm.groupValues[2]
                    var cleanImg = imgRaw.replace(Regex("""/r/\d+x\d+/"""), "/")
                    val qIdx = cleanImg.indexOf("?")
                    if (qIdx != -1) cleanImg = cleanImg.substring(0, qIdx)
                    if (cleanImg.contains("questionmark")) cleanImg = ""

                    val rawName = cm.groupValues[3].trim()
                    val formattedName = if (rawName.contains(",")) {
                        val parts = rawName.split(",").map { it.trim() }
                        if (parts.size > 1) "${parts[1]} ${parts[0]}" else parts[0]
                    } else rawName

                    if (formattedName.isNotBlank()) {
                        charsList.add(Person(name = formattedName, imageUrl = cleanImg))
                    }
                }

                // 8. Staff Extraction with High-Res Images & Clean Names
                val staffList = mutableListOf<Person>()
                val staffHeaderIdx = html.indexOf("<h2>Staff</h2>")
                if (staffHeaderIdx != -1) {
                    val staffSection = html.substring(staffHeaderIdx)
                    val staffRegex = Regex(
                        """<a href="https://myanimelist.net/people/(\d+)/[^"]*"[^>]*>\s*<img[^>]*data-src="([^"]+)"[^>]*>.*?<a href="https://myanimelist.net/people/\1/[^"]*">([^<]+)</a>.*?<small>([^<]+)</small>""",
                        RegexOption.DOT_MATCHES_ALL
                    )
                    val staffMatches = staffRegex.findAll(staffSection)
                    for (sm in staffMatches.take(8)) {
                        val imgRaw = sm.groupValues[2]
                        var cleanImg = imgRaw.replace(Regex("""/r/\d+x\d+/"""), "/")
                        val qIdx = cleanImg.indexOf("?")
                        if (qIdx != -1) cleanImg = cleanImg.substring(0, qIdx)
                        if (cleanImg.contains("questionmark")) cleanImg = ""

                        val rawName = sm.groupValues[3].trim()
                        val formattedName = if (rawName.contains(",")) {
                            val parts = rawName.split(",").map { it.trim() }
                            if (parts.size > 1) "${parts[1]} ${parts[0]}" else parts[0]
                        } else rawName

                        val role = sm.groupValues[4].trim()
                        val displayName = if (role.isNotBlank()) "$formattedName ($role)" else formattedName

                        if (formattedName.isNotBlank()) {
                            staffList.add(Person(name = displayName, imageUrl = cleanImg))
                        }
                    }
                }

                return@withContext item.copy(
                    synopsisEn = description.ifBlank { item.synopsisEn },
                    posterUrl = fullImg,
                    genres = if (cleanedGenres.isNotEmpty()) cleanedGenres.joinToString(", ") else item.genres,
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
