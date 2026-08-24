package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedAnimeEntity::class, WatchHistoryEntity::class, LocalCommentEntity::class, CustomListEntity::class, CustomListItemEntity::class, WatchedEpisodeEntity::class, PublishedAnimeEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AniFluxDatabase : RoomDatabase() {
    abstract fun dao(): AniFluxDao

    companion object {
        @Volatile
        private var INSTANCE: AniFluxDatabase? = null

        fun getDatabase(context: Context): AniFluxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AniFluxDatabase::class.java,
                    "aniflux_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
