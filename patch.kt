    sealed class OverlayState {
        val id: String = java.util.UUID.randomUUID().toString()
        data class AnimeDetails(val anime: Anime) : OverlayState()
        data class Search(val filterState: FilterState) : OverlayState()
    }

    private val _overlayStack = MutableStateFlow<List<OverlayState>>(emptyList())
    val overlayStack: StateFlow<List<OverlayState>> = _overlayStack.asStateFlow()

    fun openSearch() {
        val currentStack = _overlayStack.value.toMutableList()
        if (currentStack.isNotEmpty() && currentStack.last() is OverlayState.Search) {
            return
        }
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
    }

    fun openSearchWithGenre(genre: String) {
        resetFilters()
        updateGenreFilter(genre)
        val currentStack = _overlayStack.value.toMutableList()
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
    }

    fun openSearchWithStudio(studio: String) {
        resetFilters()
        updateStudioFilter(studio)
        val currentStack = _overlayStack.value.toMutableList()
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
    }

    fun popOverlay(): Boolean {
        val currentStack = _overlayStack.value
        if (currentStack.isNotEmpty()) {
            val newStack = currentStack.dropLast(1)
            _overlayStack.value = newStack
            val newTop = newStack.lastOrNull()
            if (newTop is OverlayState.Search) {
                _filterState.value = newTop.filterState
            }
            if (newTop is OverlayState.AnimeDetails) {
                _selectedAnimeForDetails.value = newTop.anime
            } else if (newTop == null) {
                _selectedAnimeForDetails.value = null
            }
            return true
        }
        return false
    }

    fun clearOverlays() {
        _overlayStack.value = emptyList()
        _selectedAnimeForDetails.value = null
    }

    // Active Anime Details Screen
    private val _selectedAnimeForDetails = MutableStateFlow<Anime?>(null)
    val selectedAnimeForDetails: StateFlow<Anime?> = _selectedAnimeForDetails.asStateFlow()
