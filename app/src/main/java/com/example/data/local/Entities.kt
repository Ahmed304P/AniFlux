package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_anime")
data class SavedAnimeEntity(
    @PrimaryKey val animeId: String,
    val status: String, // "FAVORITE", "WATCHING", "PLAN_TO_WATCH", "COMPLETED"
    val addedTimestamp: Long = System.currentTimeMillis(),
    val userRating: Int = 0
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val historyId: String, // animeId + "_" + episodeNumber
    val animeId: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val progressSeconds: Int,
    val totalSeconds: Int,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_comments")
data class LocalCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animeId: String,
    val episodeNumber: Int,
    val authorName: String,
    val content: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_list_items")
data class CustomListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val animeId: String,
    val addedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "watched_episodes")
data class WatchedEpisodeEntity(
    @PrimaryKey val id: String, // animeId + "_" + episodeNumber
    val animeId: String,
    val episodeNumber: Int,
    val watchedAt: Long = System.currentTimeMillis()
)
