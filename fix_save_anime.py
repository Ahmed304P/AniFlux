import re

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

replacement = """    fun saveAnime(anime: Anime) {
        val currentList = _allAnime.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == anime.id }
        if (index >= 0) {
            currentList[index] = anime
            val repoIndex = repository.sampleAnimeList.indexOfFirst { it.id == anime.id }
            if (repoIndex >= 0) repository.sampleAnimeList[repoIndex] = anime
        } else {
            currentList.add(0, anime)
            repository.sampleAnimeList.add(0, anime)
        }
        _allAnime.value = currentList.toList()
    }"""

content = re.sub(r'    fun saveAnime\(anime: Anime\) \{.*?(?=\n\n    fun openAnimeDetails)', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)
