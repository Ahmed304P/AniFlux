import re

with open('app/src/main/java/com/example/data/repository/AnimeRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_block = """            val serversList = if (!customUrl.isNullOrBlank()) {
                listOf(
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_custom",
                        nameAr = "سيرفر رئيسي 1080p",
                        quality = "1080p",
                        streamUrl = customUrl
                    )
                )
            } else if"""

new_block = """            val serversList = if (!customUrl.isNullOrBlank()) {
                val parsedServers = mutableListOf<com.example.data.model.StreamServer>()
                val parts = customUrl.split(",")
                parts.forEachIndexed { index, part ->
                    val trimmedPart = part.trim()
                    if (trimmedPart.isNotBlank()) {
                        val nameAndUrl = trimmedPart.split("|", limit = 2)
                        val name = if (nameAndUrl.size == 2) nameAndUrl[0].trim() else "سيرفر ${index + 1}"
                        val url = if (nameAndUrl.size == 2) nameAndUrl[1].trim() else nameAndUrl[0].trim()
                        parsedServers.add(
                            com.example.data.model.StreamServer(
                                id = "${animeId}_${num}_custom_$index",
                                nameAr = name,
                                quality = "متعدد",
                                streamUrl = url
                            )
                        )
                    }
                }
                if (parsedServers.isEmpty()) {
                    listOf(
                        com.example.data.model.StreamServer(
                            id = "${animeId}_${num}_custom",
                            nameAr = "سيرفر رئيسي",
                            quality = "متعدد",
                            streamUrl = customUrl
                        )
                    )
                } else parsedServers
            } else if"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/data/repository/AnimeRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
