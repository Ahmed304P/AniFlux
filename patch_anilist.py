import re

with open('app/src/main/java/com/example/util/AniListService.kt', 'r') as f:
    content = f.read()

new_classes = """
data class RelatedAnime(
    val titleEn: String,
    val titleAr: String,
    val posterUrl: String,
    val relationType: String,
    val relationTypeAr: String,
    val rating: String,
    val status: String
)
"""

if "data class RelatedAnime" not in content:
    content = content.replace("data class AniListResult(", new_classes + "\ndata class AniListResult(")

new_function = """
    suspend fun getAnimeRelations(query: String): List<RelatedAnime> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val graphqlQuery = \"\"\"
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
                    }
                  }
                }
              }
            }
        \"\"\".trimIndent()

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
                    val mappedStatus = when (status) {
                        "FINISHED" -> "مكتمل"
                        "RELEASING" -> "مستمر"
                        "NOT_YET_RELEASED" -> "قريباً"
                        else -> "مستمر"
                    }
                    
                    val mappedRelation = when (relationType) {
                        "SEQUEL" -> "التالي"
                        "PREQUEL" -> "السابق"
                        "ALTERNATIVE" -> "بديل"
                        "SPIN_OFF" -> "فرعي"
                        "SIDE_STORY" -> "قصة جانبية"
                        "PARENT" -> "رئيسي"
                        "SUMMARY" -> "ملخص"
                        else -> "مرتبط"
                    }
                    
                    results.add(RelatedAnime(
                        titleEn = enTitle,
                        titleAr = romajiTitle,
                        posterUrl = cover,
                        relationType = relationType,
                        relationTypeAr = mappedRelation,
                        rating = formattedScore,
                        status = mappedStatus
                    ))
                }
                
                // Sorting logic: SEQUEL (التالي) and PREQUEL (السابق) first.
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
"""

if "fun getAnimeRelations" not in content:
    content = content.replace("suspend fun searchAnime", new_function + "\n    suspend fun searchAnime")

with open('app/src/main/java/com/example/util/AniListService.kt', 'w') as f:
    f.write(content)
