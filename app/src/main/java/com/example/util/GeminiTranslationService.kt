package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GeminiTranslationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun translateToArabic(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ar&dt=t&q=${encodedText}"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Translation failed: ${response.code}"
                
                val bodyStr = response.body?.string() ?: return@withContext "No response body"
                // Response is a JSON Array containing arrays of translated text fragments
                val jsonArray = JSONArray(bodyStr)
                val partsArray = jsonArray.optJSONArray(0)
                if (partsArray != null) {
                    val sb = java.lang.StringBuilder()
                    for (i in 0 until partsArray.length()) {
                        val part = partsArray.optJSONArray(i)
                        if (part != null && part.length() > 0) {
                            sb.append(part.optString(0, ""))
                        }
                    }
                    return@withContext sb.toString()
                }
                return@withContext "Failed to parse translation"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Translation error: ${e.message}"
        }
    }
}
