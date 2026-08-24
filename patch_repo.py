import sys

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "r") as f:
    content = f.read()

replacement = """
    fun getEpisodesForAnime(anime: com.example.data.model.Anime): List<com.example.data.model.Episode> {
        val totalEpisodes = anime.currentEpisodes
        val animeId = anime.id
        
        // Find maximum episode number (either from currentEpisodes or customEpisodes)
        val maxCustomEp = anime.customEpisodes?.keys?.mapNotNull { it.toIntOrNull() }?.maxOrNull() ?: 0
        val maxEp = maxOf(totalEpisodes, maxCustomEp)
        
        return (1..maxEp).map { num ->
            val formattedNumber = num.toString()
            val customUrl = anime.customEpisodes?.get(formattedNumber)
            
            val serversList = if (!customUrl.isNullOrBlank()) {
                listOf(
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_custom",
                        nameAr = "سيرفر رئيسي 1080p",
                        quality = "1080p",
                        streamUrl = customUrl
                    )
                )
            } else if (anime.serverId != null && anime.serverId.isNotBlank()) {
                listOf(
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_720",
                        nameAr = "سيرفر رئيسي 720p",
                        quality = "720p",
                        streamUrl = "https://server.sanime.net/Video/${anime.serverId}/${formattedNumber}.mp4"
                    ),
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_480",
                        nameAr = "سيرفر اقتصادي 480p",
                        quality = "480p",
                        streamUrl = "https://server.sanime.net/Video/${anime.serverId}/${formattedNumber}SD.mp4"
                    )
                )
            } else {
                emptyList()
            }
            
            com.example.data.model.Episode(
                id = "${animeId}_ep_${num}",
                animeId = animeId,
                episodeNumber = num,
                titleAr = "الحلقة $num",
                durationMinutes = 24,
                thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop",
                releaseDate = "2023-01-01",
                servers = serversList
            )
        }.filter { it.servers.isNotEmpty() }
    }
"""

import re
pattern = r"fun getEpisodesForAnime.*?return.*?\}\n\s*\}\n\s*\}"
content = re.sub(pattern, replacement.strip(), content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "w") as f:
    f.write(content)

print("Patched AnimeRepository.kt")
