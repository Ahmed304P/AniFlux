package com.example.ui.viewmodel
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.delay
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AniFluxDatabase
import com.example.data.model.*
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val suggestedAnimeList: List<Anime> = emptyList()
)

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    var adminEditingAnimeId: String? = null

    fun getWatchedEpisodes(animeId: String): Flow<Set<Int>> {
        return db.dao().getWatchedEpisodesForAnime(animeId).map { list ->
            list.map { it.episodeNumber }.toSet()
        }
    }

    
    fun rateAnime(animeId: String, rating: Int) {
        viewModelScope.launch {
            val existing = db.dao().getSavedAnimeById(animeId).firstOrNull()
            if (existing != null) {
                db.dao().saveAnime(existing.copy(userRating = rating))
            } else {
                db.dao().saveAnime(com.example.data.local.SavedAnimeEntity(animeId, "NONE", userRating = rating))
            }
        }
    }

    fun toggleEpisodeWatched(animeId: String, episodeNumber: Int, isWatched: Boolean) {
        viewModelScope.launch {
            val id = "${animeId}_${episodeNumber}"
            if (isWatched) {
                db.dao().insertWatchedEpisode(com.example.data.local.WatchedEpisodeEntity(id, animeId, episodeNumber))
            } else {
                db.dao().removeWatchedEpisode(id)
            }
        }
    }


    private val db = AniFluxDatabase.getDatabase(application)
    private val repository = AnimeRepository(db.dao())


    // App Settings
    private val _currentTheme = MutableStateFlow(AppTheme.DEFAULT)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _isGridMode = MutableStateFlow(true)
    val isGridMode: StateFlow<Boolean> = _isGridMode.asStateFlow()

    fun toggleGridMode() {
        _isGridMode.value = !_isGridMode.value
    }


    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }

    private val _skipTime = MutableStateFlow(10)
    val skipTime: StateFlow<Int> = _skipTime.asStateFlow()

    fun setSkipTime(seconds: Int) {
        _skipTime.value = seconds
    }


    // User Profile State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("مستخدم جديد")
    val userName: StateFlow<String> = _userName.asStateFlow()
    
    private val _userAvatar = MutableStateFlow("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&auto=format&fit=crop")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()
    
    private val _userBanner = MutableStateFlow("https://images.unsplash.com/photo-1542451313056-b7c8e626645f?q=80&w=1000&auto=format&fit=crop")
    val userBanner: StateFlow<String> = _userBanner.asStateFlow()

    fun completeLogin(name: String, avatar: String, banner: String) {
        _userName.value = name
        _userAvatar.value = avatar
        _userBanner.value = banner
        _isLoggedIn.value = true
    }
    
    fun logout() {
        _isLoggedIn.value = false
        _userName.value = ""
        _userAvatar.value = ""
        _userBanner.value = ""
    }

    // UI Navigation State

    private val tabHistory = mutableListOf<Int>()
    private val _tabHistorySize = MutableStateFlow(0)
    val tabHistorySize: StateFlow<Int> = _tabHistorySize.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(tabIndex: Int, addToHistory: Boolean = true) {
        if (addToHistory && _selectedTab.value != tabIndex) {
            tabHistory.add(_selectedTab.value)
            _tabHistorySize.value = tabHistory.size
        }
        _selectedTab.value = tabIndex
        when (tabIndex) {
            8 -> setLibraryCategory(LibraryCategory.PLAN_TO_WATCH)
            9 -> setLibraryCategory(LibraryCategory.FAVORITES)
            11 -> setLibraryCategory(LibraryCategory.HISTORY)
            // Default to WATCHING if tab 3 is clicked directly
            3 -> setLibraryCategory(LibraryCategory.WATCHING)
            
            // For Ratings, could optionally reset filters and change sort order
            1, 5, 6, 7 -> resetFilters()
        }
    }
    
    fun popTabStack(): Boolean {
        if (tabHistory.isNotEmpty()) {
            val prevTab = tabHistory.removeAt(tabHistory.size - 1)
            _tabHistorySize.value = tabHistory.size
            setSelectedTab(prevTab, addToHistory = false)
            return true
        }
        return false
    }


    // Anime Data Catalog
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _allAnime = MutableStateFlow<List<Anime>>(emptyList())
    val allAnime: StateFlow<List<Anime>> = _allAnime

    init {
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
    }

    val featuredAnime: StateFlow<Anime?> = allAnime.map { list ->
        list.find { it.isFeatured } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, repository.sampleAnimeList.firstOrNull())

    // Search and Filters
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun updateGenreFilter(genre: String?) {
        _filterState.update { it.copy(selectedGenre = if (it.selectedGenre == genre) null else genre) }
    }

    fun updateStatusFilter(status: String?) {
        _filterState.update { it.copy(selectedStatus = if (it.selectedStatus == status) null else status) }
    }


    fun updateTypeFilter(type: String?) {
        _filterState.update { it.copy(selectedType = if (it.selectedType == type) null else type) }
    }

    fun updateYearFilter(year: Int?) {
        _filterState.update { it.copy(selectedYear = if (it.selectedYear == year) null else year) }
    }

    fun updateStudioFilter(studio: String?) {
        _filterState.update { it.copy(selectedStudio = if (it.selectedStudio == studio) null else studio) }
    }

    fun updateAgeRatingFilter(ageRating: String?) {
        _filterState.update { it.copy(selectedAgeRating = if (it.selectedAgeRating == ageRating) null else ageRating) }
    }

    fun updateSeasonFilter(season: String?) {
        _filterState.update { it.copy(selectedSeason = if (it.selectedSeason == season) null else season) }
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    // Filtered Anime List for Explore / Search
    val filteredAnimeList: StateFlow<List<Anime>> = combine(allAnime, _filterState) { list, filters ->
        list.filter { anime ->
            val matchesQuery = filters.query.isBlank() ||
                    anime.titleAr.contains(filters.query, ignoreCase = true) ||
                    anime.titleEn.contains(filters.query, ignoreCase = true) ||
                    anime.genres.any { it.contains(filters.query, ignoreCase = true) }

            val matchesGenre = filters.selectedGenre == null || anime.genres.contains(filters.selectedGenre)
            val matchesStatus = filters.selectedStatus == null || anime.status == filters.selectedStatus
            val matchesType = filters.selectedType == null || anime.type == filters.selectedType
            val matchesYear = filters.selectedYear == null || anime.year == filters.selectedYear
            val matchesStudio = filters.selectedStudio == null || anime.studio == filters.selectedStudio
            val matchesAgeRating = filters.selectedAgeRating == null || anime.ageRating == filters.selectedAgeRating
            val matchesSeason = filters.selectedSeason == null || anime.season == filters.selectedSeason

            matchesQuery && matchesGenre && matchesStatus && matchesType && matchesYear && matchesStudio && matchesAgeRating && matchesSeason
        }.sortedByDescending { it.rating }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        _selectedAnimeForDetails.value = null
    }

    fun openSearchWithGenre(genre: String) {
        resetFilters()
        updateGenreFilter(genre)
        val currentStack = _overlayStack.value.toMutableList()
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
        _selectedAnimeForDetails.value = null
    }

    fun openSearchWithStudio(studio: String) {
        resetFilters()
        updateStudioFilter(studio)
        val currentStack = _overlayStack.value.toMutableList()
        currentStack.add(OverlayState.Search(_filterState.value))
        _overlayStack.value = currentStack
        _selectedAnimeForDetails.value = null
    }

    fun popOverlay(): Boolean {
        val currentStack = _overlayStack.value
        if (currentStack.isNotEmpty()) {
            val newStack = currentStack.dropLast(1)
            _overlayStack.value = newStack
            val newTop = newStack.lastOrNull()
            if (newTop is OverlayState.Search) {
                _filterState.value = newTop.filterState
                _selectedAnimeForDetails.value = null
            } else if (newTop is OverlayState.AnimeDetails) {
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

    
    fun saveAnime(anime: Anime) {
        viewModelScope.launch {
            repository.savePublishedAnime(anime)
        }
    }

    fun openAnimeDetails(anime: Anime) {
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
    }

    fun closeAnimeDetails() {
        popOverlay()
    }

    // Active Video Player Modal
    private val _activePlayerEpisode = MutableStateFlow<Pair<Anime, Episode>?>(null)
    val activePlayerEpisode = _activePlayerEpisode.asStateFlow()
    private val _activePlayerServer = MutableStateFlow<com.example.data.model.StreamServer?>(null)
    val activePlayerServer = _activePlayerServer.asStateFlow()

    fun openPlayer(anime: Anime, episode: Episode, server: com.example.data.model.StreamServer? = null) {
        _activePlayerServer.value = server
        _activePlayerEpisode.value = Pair(anime, episode)
    }

    fun closePlayer() {
        _activePlayerEpisode.value = null
        _activePlayerServer.value = null
    }

    // Episodes list for current anime
    fun getEpisodesForAnime(anime: Anime): List<Episode> {
        return repository.getEpisodesForAnime(anime)
    }

    // Saved Anime / Favorites Flow from Room DB
    val savedAnimeEntities = repository.getAllSavedAnime().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun isAnimeSaved(animeId: String): Flow<Boolean> {
        return repository.getSavedAnimeById(animeId).map { it != null }
    }

    fun getAnimeSavedStatus(animeId: String): Flow<String?> {
        return repository.getSavedAnimeById(animeId).map { it?.status }
    }

    fun getUserRating(animeId: String): Flow<Int?> {
        return repository.getSavedAnimeById(animeId).map { it?.userRating }
    }

    fun toggleSaveAnime(animeId: String, status: String = "FAVORITE") {
        viewModelScope.launch {
            repository.setAnimeSavedStatus(animeId, status)
        }
    }

    fun removeSavedAnime(animeId: String) {
        viewModelScope.launch {
            repository.removeSavedAnime(animeId)
        }
    }

    // Watch History
    val watchHistory = repository.getAllWatchHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    fun removeWatchHistory(animeId: String) {
        viewModelScope.launch {
            repository.removeWatchHistoryByAnime(animeId)
        }
    }

    fun recordWatchProgress(animeId: String, episodeNum: Int, title: String, progressSec: Int, totalSec: Int) {
        viewModelScope.launch {
            repository.recordWatchProgress(animeId, episodeNum, title, progressSec, totalSec)
        }
    }

    // Episode Comments State
    private val _activeComments = MutableStateFlow<List<CommentItem>>(emptyList())
    val activeComments: StateFlow<List<CommentItem>> = _activeComments.asStateFlow()

    fun loadCommentsForEpisode(animeId: String, epNum: Int) {
        viewModelScope.launch {
            // Combine initial sample comments with local room comments
            repository.getLocalComments(animeId, epNum).collect { localList ->
                val initial = repository.getInitialComments(animeId, epNum)
                _activeComments.value = localList + initial
            }
        }
    }

    fun addComment(animeId: String, epNum: Int, author: String, content: String) {
        viewModelScope.launch {
            repository.addComment(animeId, epNum, author, content)
            loadCommentsForEpisode(animeId, epNum)
        }
    }

    fun likeComment(commentId: String) {
        val idLong = commentId.toLongOrNull()
        if (idLong != null) {
            viewModelScope.launch {
                repository.likeComment(idLong)
            }
        } else {
            _activeComments.update { list ->
                list.map { c ->
                    if (c.id == commentId) {
                        c.copy(
                            likesCount = if (c.isLikedByMe) c.likesCount - 1 else c.likesCount + 1,
                            isLikedByMe = !c.isLikedByMe
                        )
                    } else c
                }
            }
        }
    }

    // Schedule Day Filter
    private val _selectedScheduleDay = MutableStateFlow("الأحد")
    val selectedScheduleDay: StateFlow<String> = _selectedScheduleDay.asStateFlow()

    fun setScheduleDay(day: String) {
        _selectedScheduleDay.value = day
    }

    val scheduledAnimeList: StateFlow<List<Anime>> = combine(allAnime, _selectedScheduleDay) { list, day ->
        list.filter { it.releaseDay == day }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Library Tab Filter
    private val _selectedLibraryCategory = MutableStateFlow(LibraryCategory.FAVORITES)
    val selectedLibraryCategory: StateFlow<LibraryCategory> = _selectedLibraryCategory.asStateFlow()

    fun setLibraryCategory(category: LibraryCategory) {
        _selectedLibraryCategory.value = category
    }

    // AI Recommendation Assistant
    private val _aiMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "أهلاً بك في مساعد AniFlux الذكي! 🤖✨\nاطلب مني أي اقتراحات لأنمي، مثل:\n- \"أبي أنمي ذكاء وغموض\"\n- \"اقترح لي أنمي خيال وأكشن قوافل\"\n- \"أنمي رومنسي وشريحة من الحياة\""
            )
        )
    )
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    fun sendAiPrompt(promptText: String) {
        if (promptText.isBlank()) return

        val userMsg = ChatMessage(sender = "USER", text = promptText)
        _aiMessages.update { it + userMsg }

        viewModelScope.launch {
            val queryLower = promptText.lowercase()
            val matchedAnime = repository.sampleAnimeList.filter { anime ->
                queryLower.split(" ").any { keyword ->
                    keyword.length > 2 && (
                        anime.titleAr.contains(keyword, true) ||
                        anime.synopsisAr.contains(keyword, true) ||
                        anime.genres.any { genre -> genre.contains(keyword, true) }
                    )
                }
            }.take(3)

            val finalSuggestions = if (matchedAnime.isNotEmpty()) matchedAnime else repository.sampleAnimeList.take(3)

            val aiResponseText = when {
                matchedAnime.isNotEmpty() -> "بناءً على طلبك \"$promptText\"، إليك أفضل أنميات AniFlux المقترحة لك:"
                else -> "إليك تشكيلة من أفضل وأعلى الأنميات تقييماً على شبكة AniFlux لهذا الموسم:"
            }

            _aiMessages.update {
                it + ChatMessage(
                    sender = "AI",
                    text = aiResponseText,
                    suggestedAnimeList = finalSuggestions
                )
            }
        }
    }

    // --- Custom Lists ---
    val customLists = repository.getAllCustomLists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun createCustomList(name: String, description: String) {
        viewModelScope.launch {
            repository.createCustomList(name, description)
        }
    }

    fun deleteCustomList(listId: Long) {
        viewModelScope.launch {
            repository.deleteCustomList(listId)
        }
    }

    private val _currentCustomListItems = MutableStateFlow<List<com.example.data.local.CustomListItemEntity>>(emptyList())
    val currentCustomListItems = _currentCustomListItems.asStateFlow()

    private var currentListJob: kotlinx.coroutines.Job? = null

    fun loadCustomListItems(listId: Long) {
        currentListJob?.cancel()
        currentListJob = viewModelScope.launch {
            repository.getCustomListItems(listId).collect { items ->
                _currentCustomListItems.value = items
            }
        }
    }

    fun addAnimeToCustomList(listId: Long, animeId: String) {
        viewModelScope.launch {
            repository.addAnimeToCustomList(listId, animeId)
        }
    }

    fun removeAnimeFromCustomList(listId: Long, animeId: String) {
        viewModelScope.launch {
            repository.removeAnimeFromCustomList(listId, animeId)
        }
    }
}



enum class AppTheme {
    DEFAULT,
    DARK_BLACK,
    DARK_BLUE
}
