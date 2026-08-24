import re

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

# Make allAnime a MutableStateFlow so we can update it
content = content.replace(
    "val allAnime: StateFlow<List<Anime>> = flowOf(repository.sampleAnimeList)",
    "private val _allAnime = MutableStateFlow(repository.sampleAnimeList.toList())\n    val allAnime: StateFlow<List<Anime>> = _allAnime.asStateFlow()"
)
content = content.replace(
    "SharingStarted.WhileSubscribed(5000), emptyList()",
    "SharingStarted.WhileSubscribed(5000), emptyList()\n    )"
)

# Add saveAnime method
add_method = """
    fun saveAnime(anime: Anime) {
        val currentList = repository.sampleAnimeList
        val index = currentList.indexOfFirst { it.id == anime.id }
        if (index >= 0) {
            currentList[index] = anime
        } else {
            currentList.add(anime)
        }
        _allAnime.value = currentList.toList()
    }
"""
content = content.replace("fun openAnimeDetails(anime: Anime)", add_method + "\n    fun openAnimeDetails(anime: Anime)")

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)
