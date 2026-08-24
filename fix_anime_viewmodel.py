import re

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

# Replace the init block to collect published anime and merge them with sampleAnimeList
init_replacement = """    init {
        viewModelScope.launch {
            delay(1500) // Simulate network load
            repository.publishedAnimeFlow.collect { publishedAnime ->
                val mergedList = mutableListOf<Anime>()
                mergedList.addAll(publishedAnime)
                
                // Add sample animes that are not in the published list (so we don't duplicate or overwrite edits)
                val publishedIds = publishedAnime.map { it.id }.toSet()
                val samplesToAdd = repository.sampleAnimeList.filter { it.id !in publishedIds }
                mergedList.addAll(samplesToAdd)
                
                _allAnime.value = mergedList
                _isLoading.value = false
            }
        }
    }"""

content = re.sub(r'    init \{.*?\n    \}', init_replacement, content, flags=re.DOTALL)


save_anime_replacement = """    fun saveAnime(anime: Anime) {
        viewModelScope.launch {
            repository.savePublishedAnime(anime)
        }
    }"""

content = re.sub(r'    fun saveAnime\(anime: Anime\) \{.*?\n    \}', save_anime_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)

