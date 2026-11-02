package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AniFluxDao {
    // Saved Anime / Favorites
    @Query("SELECT * FROM saved_anime ORDER BY addedTimestamp DESC")
    fun getAllSavedAnime(): Flow<List<SavedAnimeEntity>>

    @Query("SELECT * FROM saved_anime WHERE animeId = :animeId LIMIT 1")
    fun getSavedAnimeById(animeId: String): Flow<SavedAnimeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnime(entity: SavedAnimeEntity)

    @Query("DELETE FROM saved_anime WHERE animeId = :animeId")
    suspend fun removeSavedAnime(animeId: String)

    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE animeId = :animeId")
    suspend fun removeWatchHistoryByAnime(animeId: String)

    @Query("SELECT * FROM watch_history WHERE animeId = :animeId")
    fun getHistoryForAnime(animeId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE historyId = :historyId LIMIT 1")
    suspend fun getEpisodeHistory(historyId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchHistory(entity: WatchHistoryEntity)

    // Local Comments
    @Query("SELECT * FROM local_comments WHERE animeId = :animeId AND episodeNumber = :episodeNumber ORDER BY id DESC")
    fun getCommentsForEpisode(animeId: String, episodeNumber: Int): Flow<List<LocalCommentEntity>>

    // --- Custom Lists ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomList(list: CustomListEntity): Long

    @Query("SELECT * FROM custom_lists ORDER BY createdAt DESC")
    fun getAllCustomLists(): Flow<List<CustomListEntity>>

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteCustomList(listId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomListItem(item: CustomListItemEntity)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId AND animeId = :animeId")
    suspend fun deleteCustomListItem(listId: Long, animeId: String)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId")
    suspend fun deleteCustomListItems(listId: Long)

    @Query("SELECT * FROM custom_list_items WHERE listId = :listId ORDER BY addedAt DESC")
    fun getCustomListItems(listId: Long): Flow<List<CustomListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: LocalCommentEntity)

    @Query("UPDATE local_comments SET likesCount = likesCount + 1, isLiked = 1 WHERE id = :commentId")
    suspend fun likeComment(commentId: Long)

    @Query("SELECT * FROM watched_episodes WHERE animeId = :animeId")
    fun getWatchedEpisodesForAnime(animeId: String): Flow<List<WatchedEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedEpisode(entity: WatchedEpisodeEntity)

    @Query("DELETE FROM watched_episodes WHERE id = :id")
    suspend fun removeWatchedEpisode(id: String)

    @Query("SELECT * FROM published_anime ORDER BY rowid DESC")
    fun getAllPublishedAnime(): Flow<List<PublishedAnimeEntity>>

    @Query("SELECT * FROM published_anime ORDER BY rowid DESC")
    suspend fun getAllPublishedAnimeSync(): List<PublishedAnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePublishedAnime(entity: PublishedAnimeEntity)

    @Query("DELETE FROM published_anime WHERE id = :animeId")
    suspend fun removePublishedAnime(animeId: String)

    // --- Extension Repositories & Managed Extensions ---
    @Query("SELECT * FROM extension_repositories ORDER BY addedAt DESC")
    fun getAllExtensionRepos(): Flow<List<ExtensionRepoEntity>>

    @Query("SELECT * FROM extension_repositories ORDER BY addedAt DESC")
    suspend fun getAllExtensionReposSync(): List<ExtensionRepoEntity>

    @Query("SELECT * FROM extension_repositories WHERE repoId = :repoId LIMIT 1")
    suspend fun getExtensionRepoById(repoId: String): ExtensionRepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtensionRepo(repo: ExtensionRepoEntity)

    @Query("DELETE FROM extension_repositories WHERE repoId = :repoId")
    suspend fun deleteExtensionRepo(repoId: String)

    @Query("SELECT * FROM anime_extensions ORDER BY repoName ASC, name ASC")
    fun getAllExtensions(): Flow<List<AnimeExtensionEntity>>

    @Query("SELECT * FROM anime_extensions WHERE isEnabled = 1 AND isCompatible = 1 AND securityVerified = 1 ORDER BY name ASC")
    fun getEnabledCompatibleExtensions(): Flow<List<AnimeExtensionEntity>>

    @Query("SELECT * FROM anime_extensions WHERE isEnabled = 1 AND isCompatible = 1 AND securityVerified = 1 ORDER BY name ASC")
    suspend fun getEnabledCompatibleExtensionsSync(): List<AnimeExtensionEntity>

    @Query("SELECT * FROM anime_extensions WHERE repoId = :repoId")
    suspend fun getExtensionsByRepoIdSync(repoId: String): List<AnimeExtensionEntity>

    @Query("SELECT * FROM anime_extensions WHERE extensionId = :extensionId LIMIT 1")
    suspend fun getExtensionByIdSync(extensionId: String): AnimeExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtensions(extensions: List<AnimeExtensionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtension(extension: AnimeExtensionEntity)

    @Query("UPDATE anime_extensions SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE extensionId = :extensionId")
    suspend fun setExtensionEnabled(extensionId: String, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM anime_extensions WHERE repoId = :repoId")
    suspend fun deleteExtensionsByRepoId(repoId: String)

    // --- Unified Anime Catalog (Smart Deduplication & Multi-Source Merging) ---
    @Query("SELECT * FROM unified_anime_catalog WHERE activeSourcesCount > 0 OR hasBaseCatalogRecord = 1 ORDER BY activeSourcesCount DESC, updatedAt DESC, rating DESC")
    fun getActiveUnifiedAnimeFlow(): Flow<List<UnifiedAnimeEntity>>

    @Query("SELECT * FROM unified_anime_catalog")
    suspend fun getAllUnifiedAnimeSync(): List<UnifiedAnimeEntity>

    @Query("SELECT * FROM unified_anime_catalog WHERE unifiedAnimeId = :unifiedAnimeId LIMIT 1")
    suspend fun getUnifiedAnimeByIdSync(unifiedAnimeId: String): UnifiedAnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnifiedAnime(entity: UnifiedAnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnifiedAnimeList(entities: List<UnifiedAnimeEntity>)

    @Query("DELETE FROM unified_anime_catalog WHERE unifiedAnimeId = :unifiedAnimeId")
    suspend fun deleteUnifiedAnimeById(unifiedAnimeId: String)

    // --- Extension Anime Links (Source Provenance & Multi-Source Linking) ---
    @Query("SELECT * FROM extension_anime_links")
    fun getAllExtensionAnimeLinksFlow(): Flow<List<ExtensionAnimeLinkEntity>>

    @Query("SELECT * FROM extension_anime_links WHERE unifiedAnimeId = :unifiedAnimeId")
    suspend fun getExtensionAnimeLinksByAnimeSync(unifiedAnimeId: String): List<ExtensionAnimeLinkEntity>

    @Query("SELECT * FROM extension_anime_links WHERE extensionId = :extensionId")
    suspend fun getExtensionAnimeLinksByExtensionSync(extensionId: String): List<ExtensionAnimeLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtensionAnimeLink(link: ExtensionAnimeLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtensionAnimeLinks(links: List<ExtensionAnimeLinkEntity>)

    @Query("UPDATE extension_anime_links SET isExtensionActive = :isActive WHERE extensionId = :extensionId")
    suspend fun setExtensionAnimeLinksActive(extensionId: String, isActive: Boolean)

    @Query("DELETE FROM extension_anime_links WHERE extensionId = :extensionId")
    suspend fun deleteExtensionAnimeLinksByExtension(extensionId: String)

    @Query(
        """
        UPDATE unified_anime_catalog 
        SET activeSourcesCount = (
            SELECT COUNT(*) FROM extension_anime_links 
            WHERE extension_anime_links.unifiedAnimeId = unified_anime_catalog.unifiedAnimeId 
            AND extension_anime_links.isExtensionActive = 1
        )
        """
    )
    suspend fun recalculateAllActiveSourcesCounts()

    // --- Unified Episode Sources (Merged Episodes & Multi-Extension Watch Servers) ---
    @Query("SELECT * FROM unified_episode_sources WHERE isExtensionActive = 1 ORDER BY episodeNumber ASC, serverOrder ASC, syncedAt ASC")
    fun getAllActiveEpisodeSourcesFlow(): Flow<List<UnifiedEpisodeSourceEntity>>

    @Query("SELECT * FROM unified_episode_sources WHERE unifiedAnimeId = :unifiedAnimeId AND isExtensionActive = 1 ORDER BY episodeNumber ASC, serverOrder ASC, syncedAt ASC")
    fun getActiveEpisodeSourcesForAnimeFlow(unifiedAnimeId: String): Flow<List<UnifiedEpisodeSourceEntity>>

    @Query("SELECT * FROM unified_episode_sources WHERE unifiedAnimeId = :unifiedAnimeId AND isExtensionActive = 1 ORDER BY episodeNumber ASC, serverOrder ASC, syncedAt ASC")
    suspend fun getActiveEpisodeSourcesForAnimeSync(unifiedAnimeId: String): List<UnifiedEpisodeSourceEntity>

    @Query("SELECT * FROM unified_episode_sources WHERE unifiedAnimeId = :unifiedAnimeId ORDER BY episodeNumber ASC, serverOrder ASC")
    suspend fun getAllEpisodeSourcesForAnimeSync(unifiedAnimeId: String): List<UnifiedEpisodeSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodeSources(sources: List<UnifiedEpisodeSourceEntity>)

    @Query("UPDATE unified_episode_sources SET isExtensionActive = :isActive WHERE extensionId = :extensionId")
    suspend fun setEpisodeSourcesActive(extensionId: String, isActive: Boolean)

    @Query("DELETE FROM unified_episode_sources WHERE extensionId = :extensionId")
    suspend fun deleteEpisodeSourcesByExtension(extensionId: String)

    // --- Extension Sync State Tracking ---
    @Query("SELECT * FROM extension_sync_states")
    fun getAllExtensionSyncStatesFlow(): Flow<List<ExtensionSyncStateEntity>>

    @Query("SELECT * FROM extension_sync_states WHERE extensionId = :extensionId LIMIT 1")
    suspend fun getExtensionSyncStateSync(extensionId: String): ExtensionSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtensionSyncState(state: ExtensionSyncStateEntity)

    @Query("DELETE FROM extension_sync_states WHERE extensionId = :extensionId")
    suspend fun deleteExtensionSyncState(extensionId: String)
}
