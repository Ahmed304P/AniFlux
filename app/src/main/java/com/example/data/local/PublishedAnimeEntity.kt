package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "published_anime")
data class PublishedAnimeEntity(
    @PrimaryKey val id: String,
    val animeJson: String
)
