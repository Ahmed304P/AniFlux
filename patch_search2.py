import re

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# When we open general search, we MUST reset filters so it doesn't remember the genre we clicked before
open_search_old = """    fun openSearch() {
        val currentStack = _overlayStack.value.toMutableList()
        if (currentStack.isNotEmpty() && currentStack.last() is OverlayState.Search) {
            return
        }
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
        _selectedAnimeForDetails.value = null
    }"""

open_search_new = """    fun openSearch() {
        resetFilters() // ADDED: Reset filters when opening general search from main screen
        val currentStack = _overlayStack.value.toMutableList()
        if (currentStack.isNotEmpty() && currentStack.last() is OverlayState.Search) {
            return
        }
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
        _selectedAnimeForDetails.value = null
    }"""

content = content.replace(open_search_old, open_search_new)

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("AnimeViewModel.kt patched.")
