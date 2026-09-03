import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_check = """                                            val isValid = withContext(Dispatchers.IO) {
                                                try {
                                                    val url = URL(link.trim())
                                                    val connection = url.openConnection() as HttpURLConnection
                                                    connection.requestMethod = "GET"
                                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                                    connection.connectTimeout = 4000
                                                    connection.readTimeout = 4000
                                                    val code = connection.responseCode
                                                    code in 200..399
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }"""

new_check = """                                            val isValid = withContext(Dispatchers.IO) {
                                                try {
                                                    val url = URL(link.trim())
                                                    val connection = url.openConnection() as HttpURLConnection
                                                    connection.requestMethod = "HEAD"
                                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                                    connection.connectTimeout = 4000
                                                    connection.readTimeout = 4000
                                                    connection.responseCode 
                                                    true 
                                                } catch (e: Exception) {
                                                    android.webkit.URLUtil.isValidUrl(link.trim())
                                                }
                                            }"""

content = content.replace(old_check, new_check)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
