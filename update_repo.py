import re

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

replacement = """        if (index >= 0) {
            currentList[index] = anime
            val repoIndex = repository.sampleAnimeList.indexOfFirst { it.id == anime.id }
            if (repoIndex >= 0) repository.sampleAnimeList[repoIndex] = anime
        } else {
            currentList.add(0, anime)
            repository.sampleAnimeList.add(0, anime)
        }"""
content = content.replace("""        if (index >= 0) {
            currentList[index] = anime
        } else {
            currentList.add(0, anime)
        }""", replacement)

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)
