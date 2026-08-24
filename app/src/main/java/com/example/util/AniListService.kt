package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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

    private const val ANILIST_URL = "https://graphql.anilist.co"

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

        val graphqlQuery = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 10) {
                media(search: ${'$'}search, type: ANIME) {
                  id
                  title {
                    romaji
                    english
                  }
                  description
                  coverImage {
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
            put("variables", JSONObject().apply { put("search", query) })
        }

        val request = Request.Builder()
            .url(ANILIST_URL)
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
                    val item = mediaArray.getJSONObject(i)
                    
                    val titleObj = item.optJSONObject("title")
                    val enTitle = titleObj?.optString("english").takeUnless { it.isNullOrBlank() || it == "null" }
                        ?: titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } 
                        ?: "Unknown Title"
                    
                    val romajiTitle = titleObj?.optString("romaji").takeUnless { it.isNullOrBlank() || it == "null" } ?: enTitle
                    
                    val description = item.optString("description", "")
                        .replace("<br>", "\n").replace(Regex("<[^>]*>"), "")
                    
                    val cover = item.optJSONObject("coverImage")?.optString("large", "") ?: ""
                    val banner = item.optString("bannerImage", "")
                    
                    val genresArray = item.optJSONArray("genres")
                    val genresList = mutableListOf<String>()
                    if (genresArray != null) {
                        for (j in 0 until genresArray.length()) {
                            val englishGenre = genresArray.getString(j)
                            genresList.add(genreTranslations[englishGenre] ?: englishGenre)
                        }
                    }
                    
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
                                charsList.add(Person(name, image ?: ""))
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
                                staffList.add(Person(name, image ?: ""))
                            }
                        }
                    }
                    
                    // Trailer
                    val trailerObj = item.optJSONObject("trailer")
                    val site = trailerObj?.optString("site")
                    val trailerId = trailerObj?.optString("id")
                    val trailerUrl = if (site == "youtube" && !trailerId.isNullOrBlank()) {
                        "https://www.youtube.com/watch?v=${'$'}{trailerId}"
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
