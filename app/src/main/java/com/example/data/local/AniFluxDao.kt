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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePublishedAnime(entity: PublishedAnimeEntity)

    @Query("DELETE FROM published_anime WHERE id = :animeId")
    suspend fun removePublishedAnime(animeId: String)
}
