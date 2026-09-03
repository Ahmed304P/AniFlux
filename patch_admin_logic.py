import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = """                val episodesCount = currentEpisodesInput.toIntOrNull() ?: 0
                val maxCustom = customEpisodesState.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                val finalEpisodesCount = maxOf(episodesCount, maxCustom)"""

new_logic = """                val episodesCount = currentEpisodesInput.toIntOrNull() ?: 0
                val maxCustom = customEpisodesState.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                var finalEpisodesCount = maxOf(episodesCount, maxCustom)
                
                // Ensure at least 1 episode is generated if server ID is provided
                if (serverIdInput.isNotBlank() && finalEpisodesCount == 0) {
                    finalEpisodesCount = 1
                }"""

content = content.replace(old_logic, new_logic)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
