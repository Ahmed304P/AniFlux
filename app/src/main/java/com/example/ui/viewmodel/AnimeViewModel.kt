package com.example.ui.viewmodel
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.delay
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AniFluxDatabase
import com.example.data.local.AnimeExtensionEntity
import com.example.data.local.ExtensionAnimeLinkEntity
import com.example.data.local.ExtensionRepoEntity
import com.example.data.local.ExtensionSyncStateEntity
import com.example.data.local.UnifiedEpisodeSourceEntity
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
    
    private val _userHandle = MutableStateFlow("")
    val userHandle: StateFlow<String> = _userHandle.asStateFlow()
    
    private val _lastHandleChangeDate = MutableStateFlow(0L)
    val lastHandleChangeDate: StateFlow<Long> = _lastHandleChangeDate.asStateFlow()
    
    private val _userLocation = MutableStateFlow("")
    val userLocation: StateFlow<String> = _userLocation.asStateFlow()
    
    private val _userBirthDate = MutableStateFlow("")
    val userBirthDate: StateFlow<String> = _userBirthDate.asStateFlow()
    
    private val _userJoinDate = MutableStateFlow("2026-09-04")
    val userJoinDate: StateFlow<String> = _userJoinDate.asStateFlow()

    fun completeLogin(name: String, handle: String, avatar: String, banner: String) {
        _userName.value = name
        _userHandle.value = handle
        _userAvatar.value = avatar
        _userBanner.value = banner
        if (_userJoinDate.value.isEmpty()) {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
            _userJoinDate.value = format.format(java.util.Date())
        }
        _isLoggedIn.value = true
    }
    
    fun updateProfile(name: String, avatar: String, banner: String, location: String, birthDate: String) {
        _userName.value = name
        _userAvatar.value = avatar
        _userBanner.value = banner
        _userLocation.value = location
        _userBirthDate.value = birthDate
    }
    
    fun updateHandle(handle: String) {
        _userHandle.value = handle
        _lastHandleChangeDate.value = System.currentTimeMillis()
    }
    
    fun logout() {
        _isLoggedIn.value = false
        _userName.value = ""
        _userHandle.value = ""
        _userAvatar.value = ""
        _userBanner.value = ""
        _userLocation.value = ""
        _userBirthDate.value = ""
        _userJoinDate.value = ""
        _lastHandleChangeDate.value = 0L
    }

    // UI Navigation State

    private val tabHistory = mutableListOf<Int>()
    private val _tabHistorySize = MutableStateFlow(0)
    val tabHistorySize: StateFlow<Int> = _tabHistorySize.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(tabIndex: Int, addToHistory: Boolean = true) {
        if (addToHistory && _selectedTab.value != tabIndex) {
            tabHistory.remove(tabIndex) // Prevent duplicate loops in history
            if (tabIndex == 0) {
                tabHistory.clear() // Root tab clears the back stack
            } else {
                tabHistory.add(_selectedTab.value)
            }
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
        // Background auto-sync for all currently enabled extensions on launch
        viewModelScope.launch {
            repository.syncAllEnabledExtensions()
        }

        // Collect the Unified Anime Catalog (Admin Published + Deduplicated Extension Anime + Base Catalog)
        viewModelScope.launch {
            delay(500)
            repository.unifiedCatalogFlow.collect { unifiedCatalog ->
                _allAnime.value = unifiedCatalog
                _isLoading.value = false

                // Keep open detail screen reactive if its anime gained episodes/characters/servers from an extension
                val currentDetail = _selectedAnimeForDetails.value
                if (currentDetail != null) {
                    val refreshed = unifiedCatalog.find { it.id == currentDetail.id }
                    if (refreshed != null && refreshed != currentDetail) {
                        _selectedAnimeForDetails.value = refreshed
                    }
                }
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
                    anime.primaryTitle.contains(filters.query, ignoreCase = true) ||
                    anime.originalTitle.contains(filters.query, ignoreCase = true) ||
                    anime.titleEn.contains(filters.query, ignoreCase = true) ||
                    anime.titleJp.contains(filters.query, ignoreCase = true) ||
                    anime.titleAr.contains(filters.query, ignoreCase = true) ||
                    anime.altTitles.any { it.contains(filters.query, ignoreCase = true) } ||
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
        data class Search(val filterState: FilterState, val title: String? = null) : OverlayState()
    }

    private val _overlayStack = MutableStateFlow<List<OverlayState>>(emptyList())
    val overlayStack: StateFlow<List<OverlayState>> = _overlayStack.asStateFlow()

    fun openSearch() {
        resetFilters() // ADDED: Reset filters when opening general search from main screen
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
        currentStack.add(OverlayState.Search(_filterState.value, title = "تصنيف: $genre"))
        _overlayStack.value = currentStack
        _selectedAnimeForDetails.value = null
    }

    fun openSearchWithStudio(studio: String) {
        resetFilters()
        updateStudioFilter(studio)
        val currentStack = _overlayStack.value.toMutableList()
        currentStack.add(OverlayState.Search(_filterState.value, title = "استوديو: $studio"))
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
        
        // Auto-add to watch history and refresh extension episodes on-demand when entering details screen
        viewModelScope.launch {
            repository.recordWatchProgress(anime.id, 0, "Details", 0, 0)
            repository.refreshAnimeEpisodesOnDemand(anime.id)
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
        viewModelScope.launch {
            repository.refreshEpisodeServersOnDemand(anime.id, episode.episodeNumber)
            val updatedEpisodes = repository.getEpisodesForAnime(
                anime,
                enabledExtensions.value,
                activeEpisodeSources.value
            )
            val refreshedEp = updatedEpisodes.find { it.episodeNumber == episode.episodeNumber }
            if (refreshedEp != null && _activePlayerEpisode.value?.second?.episodeNumber == episode.episodeNumber) {
                _activePlayerEpisode.value = Pair(anime, refreshedEp)
            }
        }
    }

    fun closePlayer() {
        _activePlayerEpisode.value = null
        _activePlayerServer.value = null
    }

    // Episodes list for current anime (merges active extension episodes & servers from Room DB reactively)
    fun getEpisodesForAnime(
        anime: Anime,
        activeExtensions: List<AnimeExtensionEntity> = enabledExtensions.value,
        activeDbSources: List<UnifiedEpisodeSourceEntity> = activeEpisodeSources.value
    ): List<Episode> {
        return repository.getEpisodesForAnime(anime, activeExtensions, activeDbSources)
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

    // --- Centralized Extension Repositories, Content Sync & Smart Deduplication ---
    val extensionRepos: StateFlow<List<ExtensionRepoEntity>> = repository.extensionReposFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val allExtensions: StateFlow<List<AnimeExtensionEntity>> = repository.allExtensionsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val enabledExtensions: StateFlow<List<AnimeExtensionEntity>> = repository.enabledExtensionsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val activeEpisodeSources: StateFlow<List<UnifiedEpisodeSourceEntity>> = repository.allActiveEpisodeSourcesFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val extensionAnimeLinks: StateFlow<List<ExtensionAnimeLinkEntity>> = repository.extensionAnimeLinksFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val extensionSyncStates: StateFlow<List<ExtensionSyncStateEntity>> = repository.extensionSyncStatesFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _isImportingRepo = MutableStateFlow(false)
    val isImportingRepo: StateFlow<Boolean> = _isImportingRepo.asStateFlow()

    private val _verifyingExtensionId = MutableStateFlow<String?>(null)
    val verifyingExtensionId: StateFlow<String?> = _verifyingExtensionId.asStateFlow()

    private val _syncingExtensionId = MutableStateFlow<String?>(null)
    val syncingExtensionId: StateFlow<String?> = _syncingExtensionId.asStateFlow()

    fun importExtensionRepository(repoUrl: String, onResult: (Boolean, String) -> Unit) {
        if (_isImportingRepo.value) return
        viewModelScope.launch {
            _isImportingRepo.value = true
            val res = repository.importOrRefreshRepository(repoUrl)
            _isImportingRepo.value = false
            res.fold(
                onSuccess = { bundle ->
                    val total = bundle.extensions.size
                    val compat = bundle.extensions.count { it.isCompatible && it.securityVerified }
                    val msg = if (compat > 0) {
                        val linkedNote = if (bundle.fetchedLinkedUrls.isNotEmpty()) {
                            " • تم جلب ${bundle.fetchedLinkedUrls.size} ملفات قوائم مرتبطة"
                        } else ""
                        "تم استيراد \"${bundle.repo.name}\" بنجاح ($total إضافة، منها $compat متوافقة)$linkedNote • جميع الإضافات الجديدة معطلة افتراضياً"
                    } else if (bundle.statusSummaryAr.isNotBlank()) {
                        bundle.statusSummaryAr
                    } else {
                        "المستودع صالح لكن تشغيل الإضافات غير مدعوم ($total إضافة مستخرجة، 0 متوافقة)"
                    }
                    onResult(true, msg)
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "فشل استيراد المستودع")
                }
            )
        }
    }

    fun refreshSingleRepository(repoUrl: String, onResult: (Boolean, String) -> Unit) {
        if (_isImportingRepo.value) return
        viewModelScope.launch {
            _isImportingRepo.value = true
            val res = repository.importOrRefreshRepository(repoUrl)
            _isImportingRepo.value = false
            res.fold(
                onSuccess = { bundle ->
                    onResult(
                        true,
                        "تم تحديث \"${bundle.repo.name}\" ومزامنة محتوى الإضافات المفعّلة مع الحفاظ على حالات التفعيل"
                    )
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "تعذر تحديث المستودع")
                }
            )
        }
    }

    fun refreshAllRepositories(onResult: (Boolean, String) -> Unit) {
        if (_isImportingRepo.value) return
        viewModelScope.launch {
            _isImportingRepo.value = true
            val res = repository.refreshAllRepositories()
            _isImportingRepo.value = false
            res.fold(
                onSuccess = { count ->
                    if (count > 0) {
                        onResult(true, "تم تحديث $count مستودعات ومزامنة محتواها مع الحفاظ التام على حالات التفعيل")
                    } else {
                        onResult(false, "لا توجد مستودعات مضافة لتحديثها")
                    }
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "حدث خطأ أثناء تحديث المستودعات")
                }
            )
        }
    }

    fun toggleExtensionEnabled(
        extensionId: String,
        targetEnabled: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _syncingExtensionId.value = if (targetEnabled) extensionId else null
            val res = repository.setExtensionEnabled(extensionId, targetEnabled)
            _syncingExtensionId.value = null
            res.fold(
                onSuccess = { syncSummary ->
                    val msg = if (targetEnabled) {
                        if (syncSummary != null) {
                            "تم تفعيل \"${syncSummary.extensionName}\" وجلب ${syncSummary.fetchedAnimeCount} أنمي عبر ${syncSummary.syncedPagesCount} صفحات " +
                                "(منها ${syncSummary.deduplicatedMergedAnimeCount} مدمج ذكياً بدون تكرار) و ${syncSummary.syncedEpisodesCount} حلقة و ${syncSummary.syncedServersCount} سيرفر"
                        } else {
                            "تم تفعيل الإضافة ومزامنة محتواها بنجاح في مكتبة التطبيق"
                        }
                    } else {
                        "تم تعطيل الإضافة وإيقاف مزامنتها مع الحفاظ على الأنميات المشتركة مع مصادر أخرى"
                    }
                    onResult(true, msg)
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "لا يمكن تفعيل هذه الإضافة")
                }
            )
        }
    }

    fun syncSingleExtensionContent(
        extensionId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (_syncingExtensionId.value != null) return
        viewModelScope.launch {
            _syncingExtensionId.value = extensionId
            val res = repository.syncExtensionContent(extensionId)
            _syncingExtensionId.value = null
            res.fold(
                onSuccess = { s ->
                    onResult(
                        true,
                        "تمت مزامنة \"${s.extensionName}\": ${s.fetchedAnimeCount} أنمي عبر ${s.syncedPagesCount} صفحات (${s.deduplicatedMergedAnimeCount} مدمج) • ${s.syncedEpisodesCount} حلقة • ${s.syncedServersCount} سيرفر"
                    )
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "تعذر مزامنة محتوى الإضافة")
                }
            )
        }
    }

    fun verifyExtensionCompatibility(
        extensionId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (_verifyingExtensionId.value != null) return
        viewModelScope.launch {
            _verifyingExtensionId.value = extensionId
            val res = repository.verifyExtensionLiveAndPersist(extensionId)
            _verifyingExtensionId.value = null
            res.fold(
                onSuccess = { report ->
                    onResult(report.passed, report.reasonAr)
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "فشل فحص الإضافة")
                }
            )
        }
    }

    fun deleteExtensionRepository(repoId: String, repoName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteRepository(repoId)
            onResult("تم حذف مستودع \"$repoName\" وجميع إضافاته من النظام")
        }
    }
}



enum class AppTheme {
    DEFAULT,
    DARK_BLACK,
    DARK_BLUE
}
