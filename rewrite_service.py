import sys

content = """package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.data.model.Person

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
    val studio: String = ""
)

object AniListService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // We use Jikan (MyAnimeList API) as fallback because AniList API is currently globally down
    private const val JIKAN_URL = "https://api.jikan.moe/v4/anime?q="

    private val genreTranslations = mapOf(
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
        "Romance" to "رومانسية",
        "Sci-Fi" to "خيال علمي",
        "Slice of Life" to "شريحة من الحياة",
        "Sports" to "رياضة",
        "Supernatural" to "خارق للطبيعة",
        "Thriller" to "إثارة"
    )

    suspend fun searchAnime(query: String): List<AniListResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val request = Request.Builder()
            .url(JIKAN_URL + query + "&limit=10")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()

                val results = mutableListOf<AniListResult>()

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)

                    val titleEn = item.optString("title_english").takeUnless { it.isNullOrBlank() || it == "null" }
                        ?: item.optString("title")
                    val titleAr = item.optString("title") // Default to romaji/english since MAL lacks Arabic
                    
                    val synopsis = item.optString("synopsis", "").replace("[Written by MAL Rewrite]", "").trim()
                    
                    val posterUrl = item.optJSONObject("images")?.optJSONObject("jpg")?.optString("large_image_url", "") ?: ""
                    
                    val trailerId = item.optJSONObject("trailer")?.optString("youtube_id", "")
                    val trailerUrl = if (!trailerId.isNullOrBlank() && trailerId != "null") "https://www.youtube.com/watch?v=$trailerId" else ""
                    
                    val score = item.optDouble("score", 0.0)
                    val formattedScore = if (score > 0) String.format("%.1f", score) else ""
                    
                    val statusStr = item.optString("status", "")
                    val mappedStatus = when (statusStr) {
                        "Finished Airing" -> "مكتمل"
                        "Currently Airing" -> "مستمر"
                        "Not yet aired" -> "قريباً"
                        else -> "مستمر"
                    }
                    
                    val typeStr = item.optString("type", "TV")
                    val mappedFormat = when (typeStr) {
                        "TV" -> "TV"
                        "Movie" -> "Movie"
                        "OVA" -> "OVA"
                        "ONA" -> "ONA"
                        else -> "TV"
                    }
                    
                    val seasonStr = item.optString("season", "")
                    val mappedSeason = when (seasonStr.lowercase()) {
                        "spring" -> "ربيع"
                        "summer" -> "صيف"
                        "fall" -> "خريف"
                        "winter" -> "شتاء"
                        else -> "ربيع"
                    }
                    
                    val year = item.optInt("year", 0).let { if (it > 0) it.toString() else "" }
                    
                    // Parse Studios
                    val studiosArray = item.optJSONArray("studios")
                    var studioName = ""
                    if (studiosArray != null && studiosArray.length() > 0) {
                        studioName = studiosArray.optJSONObject(0)?.optString("name", "") ?: ""
                    }
                    
                    // Parse Genres
                    val genresArray = item.optJSONArray("genres")
                    val genresList = mutableListOf<String>()
                    if (genresArray != null) {
                        for (j in 0 until genresArray.length()) {
                            val genreName = genresArray.optJSONObject(j)?.optString("name", "") ?: ""
                            if (genreName.isNotBlank()) {
                                genresList.add(genreTranslations[genreName] ?: genreName)
                            }
                        }
                    }
                    
                    results.add(
                        AniListResult(
                            titleEn = titleEn,
                            titleAr = titleAr,
                            synopsisEn = synopsis,
                            posterUrl = posterUrl,
                            bannerUrl = posterUrl, // MAL doesn't provide banner in search easily, fallback to poster
                            genres = genresList.joinToString(", "),
                            rating = formattedScore,
                            status = mappedStatus,
                            format = mappedFormat,
                            season = mappedSeason,
                            year = year,
                            charactersList = emptyList(), // MAL search doesn't return characters
                            staffList = emptyList(),      // MAL search doesn't return staff
                            trailer = trailerUrl,
                            studio = studioName
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
}
"""

with open("app/src/main/java/com/example/util/AniListService.kt", "w") as f:
    f.write(content)

print("Rewrote AniListService.kt to use Jikan API successfully.")
