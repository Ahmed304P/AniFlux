import re

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r') as f:
    content = f.read()

old_open = """    fun openAnimeDetails(anime: Anime) {
        _selectedAnimeForDetails.value = anime
        val currentStack = _overlayStack.value.toMutableList()
        if (currentStack.isNotEmpty() && currentStack.last() is OverlayState.Search) {
            currentStack[currentStack.size - 1] = OverlayState.Search(_filterState.value)
        }
        currentStack.add(OverlayState.AnimeDetails(anime))
        _overlayStack.value = currentStack
        // Auto-add to watch history when entering details screen
        viewModelScope.launch {
            repository.recordWatchProgress(anime.id, 0, "Details", 0, 0)
        }
    }"""

new_open = """    fun openAnimeDetails(anime: Anime) {
        val currentStack = _overlayStack.value.toMutableList()
        val lastOverlay = currentStack.lastOrNull()
        if (lastOverlay is OverlayState.AnimeDetails && lastOverlay.anime.id == anime.id) {
            return
        }
        
        _selectedAnimeForDetails.value = anime
        if (currentStack.isNotEmpty() && currentStack.last() is OverlayState.Search) {
            currentStack[currentStack.size - 1] = OverlayState.Search(_filterState.value)
        }
        currentStack.add(OverlayState.AnimeDetails(anime))
        _overlayStack.value = currentStack
        
        // Auto-add to watch history when entering details screen
        viewModelScope.launch {
            repository.recordWatchProgress(anime.id, 0, "Details", 0, 0)
        }
    }"""

content = content.replace(old_open, new_open)

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w') as f:
    f.write(content)
