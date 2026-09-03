import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add imports if missing
imports = """import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL"""
if 'java.net.HttpURLConnection' not in content:
    content = content.replace('import androidx.compose.ui.text.font.FontWeight', 'import androidx.compose.ui.text.font.FontWeight\n' + imports)

# 2. Add isCheckingLinks state
if 'var isCheckingLinks by remember' not in content:
    content = content.replace(
        'var isEditingExisting by remember { mutableStateOf(false) }',
        'var isEditingExisting by remember { mutableStateOf(false) }\n    var isCheckingLinks by remember { mutableStateOf(false) }'
    )

# 3. Replace Button click
old_button = """                        Button(
                            onClick = {
                                val epNum = dialogEpisodeNum.trim()"""

new_button = """                        Button(
                            enabled = !isCheckingLinks,
                            onClick = {
                                scope.launch {
                                    isCheckingLinks = true
                                    val epNum = dialogEpisodeNum.trim()
                                    val epNumInt = epNum.toIntOrNull()
                                    if (epNumInt != null && epNumInt > 0) {
                                        val validLinks = mutableListOf<String>()
                                        for (link in dialogServerLinks.filter { it.isNotBlank() }) {
                                            val isValid = withContext(Dispatchers.IO) {
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
                                            }
                                            if (isValid) {
                                                validLinks.add(link.trim())
                                            } else {
                                                // Optional: we can alert the user, or just silently discard
                                                // We will just discard it per request: "استبعاد الروابط المعطوبة"
                                            }
                                        }

                                        val newMap = customEpisodesState.toMutableMap()
                                        if (validLinks.isNotEmpty()) {
                                            val formattedString = validLinks.mapIndexed { i, url -> "سيرفر ${i + 1}|$url" }.joinToString(", ")
                                            newMap[epNum] = formattedString
                                        } else {
                                            newMap.remove(epNum)
                                        }
                                        customEpisodesState = newMap

                                        val currentMax = currentEpisodesInput.toIntOrNull() ?: 0
                                        val newMax = maxOf(currentMax, epNumInt)
                                        currentEpisodesInput = newMax.toString()

                                        showEpisodeDialog = false
                                    }
                                    isCheckingLinks = false
                                }
                            }"""
content = content.replace(old_button, new_button)

# 4. Also replace the rest of the original save logic so we don't duplicate it.
# We need to remove the original saving block
old_save_block = """                                val epNum = dialogEpisodeNum.trim()
                                val epNumInt = epNum.toIntOrNull()
                                if (epNumInt != null && epNumInt > 0) {
                                    val newMap = customEpisodesState.toMutableMap()
                                    val validLinks = dialogServerLinks.filter { it.isNotBlank() }
                                    if (validLinks.isNotEmpty()) {
                                        val formattedString = validLinks.mapIndexed { i, url -> "سيرفر ${i + 1}|$url" }.joinToString(", ")
                                        newMap[epNum] = formattedString
                                    } else {
                                        newMap.remove(epNum)
                                    }
                                    customEpisodesState = newMap

                                    // Update max episode count
                                    val currentMax = currentEpisodesInput.toIntOrNull() ?: 0
                                    val newMax = maxOf(currentMax, epNumInt)
                                    currentEpisodesInput = newMax.toString()

                                    showEpisodeDialog = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)"""

new_save_block = """                            },
                            modifier = Modifier
                                .weight(1f)"""

# Be careful with replacing to avoid breaking braces
# Let's use regex
content = re.sub(
    r'val epNum = dialogEpisodeNum\.trim\(\).*?showEpisodeDialog = false\s+\}\s+\},',
    '',
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

