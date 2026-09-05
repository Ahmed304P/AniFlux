import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

search_overlay_old = """                SearchScreenOverlay(
                    visible = isSearchOverlayVisible,
                    isDetailScreenOpen = selectedAnimeDetails != null,
                    onClose = { 
                        viewModel.popOverlay()
                        viewModel.updateSearchQuery("")
                    },
                    searchQuery = filterState.query,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    searchResults = filteredAnimeList,
                    onOpenAnimeDetails = { viewModel.openAnimeDetails(it) },
                    allAnime = allAnime,
                    filterState = filterState,
                    onGenreFilterChange = { viewModel.updateGenreFilter(it) },
                    onStatusFilterChange = { viewModel.updateStatusFilter(it) },
                    onTypeFilterChange = { viewModel.updateTypeFilter(it) },
                    onYearFilterChange = { viewModel.updateYearFilter(it) },
                    onStudioFilterChange = { viewModel.updateStudioFilter(it) },
                    onAgeRatingFilterChange = { viewModel.updateAgeRatingFilter(it) },
                    onSeasonFilterChange = { viewModel.updateSeasonFilter(it) },
                    onResetFilters = { viewModel.resetFilters() },
                    isGridMode = isGridMode
                )"""

search_overlay_new = """                val currentSearchOverlay = overlayStack.lastOrNull { it is com.example.ui.viewmodel.AnimeViewModel.OverlayState.Search } as? com.example.ui.viewmodel.AnimeViewModel.OverlayState.Search
                val searchTitle = currentSearchOverlay?.title

                SearchScreenOverlay(
                    visible = isSearchOverlayVisible,
                    isDetailScreenOpen = selectedAnimeDetails != null,
                    onClose = { 
                        viewModel.popOverlay()
                        viewModel.updateSearchQuery("")
                    },
                    searchQuery = filterState.query,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    searchResults = filteredAnimeList,
                    onOpenAnimeDetails = { viewModel.openAnimeDetails(it) },
                    allAnime = allAnime,
                    filterState = filterState,
                    onGenreFilterChange = { viewModel.updateGenreFilter(it) },
                    onStatusFilterChange = { viewModel.updateStatusFilter(it) },
                    onTypeFilterChange = { viewModel.updateTypeFilter(it) },
                    onYearFilterChange = { viewModel.updateYearFilter(it) },
                    onStudioFilterChange = { viewModel.updateStudioFilter(it) },
                    onAgeRatingFilterChange = { viewModel.updateAgeRatingFilter(it) },
                    onSeasonFilterChange = { viewModel.updateSeasonFilter(it) },
                    onResetFilters = { viewModel.resetFilters() },
                    isGridMode = isGridMode,
                    title = searchTitle
                )"""

content = content.replace(search_overlay_old, search_overlay_new)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MainActivity.kt patched.")
