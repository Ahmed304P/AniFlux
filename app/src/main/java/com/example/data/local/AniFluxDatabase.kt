package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedAnimeEntity::class,
        WatchHistoryEntity::class,
        LocalCommentEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        WatchedEpisodeEntity::class,
        PublishedAnimeEntity::class,
        ExtensionRepoEntity::class,
        AnimeExtensionEntity::class,
        UnifiedAnimeEntity::class,
        ExtensionAnimeLinkEntity::class,
        UnifiedEpisodeSourceEntity::class,
        ExtensionSyncStateEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AniFluxDatabase : RoomDatabase() {
    abstract fun dao(): AniFluxDao

    companion object {
        @Volatile
        private var INSTANCE: AniFluxDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extension_repositories` (
                        `repoId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `extensionCount` INTEGER NOT NULL,
                        `compatibleCount` INTEGER NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`repoId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anime_extensions` (
                        `extensionId` TEXT NOT NULL,
                        `repoId` TEXT NOT NULL,
                        `repoName` TEXT NOT NULL,
                        `repoUrl` TEXT NOT NULL,
                        `pkgName` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `versionCode` INTEGER NOT NULL,
                        `lang` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `iconUrl` TEXT NOT NULL,
                        `streamTemplate` TEXT NOT NULL,
                        `qualities` TEXT NOT NULL,
                        `engineVersion` INTEGER NOT NULL,
                        `extensionFormat` TEXT NOT NULL,
                        `isCompatible` INTEGER NOT NULL,
                        `compatibilityStatus` TEXT NOT NULL,
                        `compatibilityReasonAr` TEXT NOT NULL,
                        `securityVerified` INTEGER NOT NULL,
                        `securityDetailsAr` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`extensionId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unified_anime_catalog` (
                        `unifiedAnimeId` TEXT NOT NULL,
                        `malId` TEXT NOT NULL,
                        `anilistId` TEXT NOT NULL,
                        `titleAr` TEXT NOT NULL,
                        `titleEn` TEXT NOT NULL,
                        `titleJp` TEXT NOT NULL,
                        `altTitlesPipe` TEXT NOT NULL,
                        `posterUrl` TEXT NOT NULL,
                        `bannerUrl` TEXT NOT NULL,
                        `synopsisAr` TEXT NOT NULL,
                        `genresJson` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `season` TEXT NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `seasonsCount` INTEGER NOT NULL,
                        `totalEpisodes` INTEGER NOT NULL,
                        `currentEpisodes` INTEGER NOT NULL,
                        `studio` TEXT NOT NULL,
                        `releaseDay` TEXT NOT NULL,
                        `ageRating` TEXT NOT NULL,
                        `isFeatured` INTEGER NOT NULL,
                        `featuredTag` TEXT NOT NULL,
                        `trailerUrl` TEXT NOT NULL,
                        `charactersJson` TEXT NOT NULL,
                        `staffJson` TEXT NOT NULL,
                        `activeSourcesCount` INTEGER NOT NULL,
                        `hasBaseCatalogRecord` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`unifiedAnimeId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_anime_catalog_malId` ON `unified_anime_catalog` (`malId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_anime_catalog_anilistId` ON `unified_anime_catalog` (`anilistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_anime_catalog_activeSourcesCount` ON `unified_anime_catalog` (`activeSourcesCount`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extension_anime_links` (
                        `linkId` TEXT NOT NULL,
                        `extensionId` TEXT NOT NULL,
                        `unifiedAnimeId` TEXT NOT NULL,
                        `sourceAnimeId` TEXT NOT NULL,
                        `sourceTitleAr` TEXT NOT NULL,
                        `sourceTitleEn` TEXT NOT NULL,
                        `sourceTitleJp` TEXT NOT NULL,
                        `sourcePosterUrl` TEXT NOT NULL,
                        `sourceBannerUrl` TEXT NOT NULL,
                        `sourceSynopsisAr` TEXT NOT NULL,
                        `sourceEpisodeCount` INTEGER NOT NULL,
                        `isExtensionActive` INTEGER NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`linkId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extension_anime_links_extensionId` ON `extension_anime_links` (`extensionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extension_anime_links_unifiedAnimeId` ON `extension_anime_links` (`unifiedAnimeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_extension_anime_links_isExtensionActive` ON `extension_anime_links` (`isExtensionActive`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unified_episode_sources` (
                        `recordId` TEXT NOT NULL,
                        `unifiedEpisodeId` TEXT NOT NULL,
                        `unifiedAnimeId` TEXT NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `extensionId` TEXT NOT NULL,
                        `extensionName` TEXT NOT NULL,
                        `episodeTitleAr` TEXT NOT NULL,
                        `episodeDescriptionAr` TEXT NOT NULL,
                        `thumbnailUrl` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `releaseDate` TEXT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `serverNameAr` TEXT NOT NULL,
                        `quality` TEXT NOT NULL,
                        `subtitleLang` TEXT NOT NULL,
                        `streamUrl` TEXT NOT NULL,
                        `isExtensionActive` INTEGER NOT NULL,
                        `syncedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`recordId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_episode_sources_unifiedAnimeId_episodeNumber` ON `unified_episode_sources` (`unifiedAnimeId`, `episodeNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_episode_sources_unifiedEpisodeId` ON `unified_episode_sources` (`unifiedEpisodeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_episode_sources_extensionId` ON `unified_episode_sources` (`extensionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_episode_sources_isExtensionActive` ON `unified_episode_sources` (`isExtensionActive`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extension_sync_states` (
                        `extensionId` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        `syncedAnimeCount` INTEGER NOT NULL,
                        `mergedAnimeCount` INTEGER NOT NULL,
                        `syncedEpisodesCount` INTEGER NOT NULL,
                        `syncedServersCount` INTEGER NOT NULL,
                        `lastErrorAr` TEXT NOT NULL,
                        PRIMARY KEY(`extensionId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `anime_extensions` ADD COLUMN `adapterId` TEXT NOT NULL DEFAULT 'UNIVERSAL_DECLARATIVE_V2'")
                db.execSQL("ALTER TABLE `anime_extensions` ADD COLUMN `runtimeEnvironment` TEXT NOT NULL DEFAULT 'AniFlux Universal Runtime v3'")
                db.execSQL("ALTER TABLE `anime_extensions` ADD COLUMN `paginationType` TEXT NOT NULL DEFAULT 'PAGE'")
                db.execSQL("ALTER TABLE `anime_extensions` ADD COLUMN `verificationStagesJson` TEXT NOT NULL DEFAULT '[]'")

                db.execSQL("ALTER TABLE `unified_anime_catalog` ADD COLUMN `originalTitle` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `unified_anime_catalog` ADD COLUMN `sourceId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `unified_anime_catalog` ADD COLUMN `sourceName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `unified_anime_catalog` ADD COLUMN `sourceAnimeId` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `extension_anime_links` ADD COLUMN `sourceOriginalTitle` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `unified_episode_sources` ADD COLUMN `streamType` TEXT NOT NULL DEFAULT 'MP4'")
                db.execSQL("ALTER TABLE `unified_episode_sources` ADD COLUMN `headersJson` TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE `unified_episode_sources` ADD COLUMN `subtitlesJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `unified_episode_sources` ADD COLUMN `serverOrder` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `extension_sync_states` ADD COLUMN `totalDiscoveredAnime` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `extension_sync_states` ADD COLUMN `syncedPagesCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `extension_sync_states` ADD COLUMN `totalPagesCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `extension_sync_states` ADD COLUMN `nextPageOrCursor` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `extension_sync_states` ADD COLUMN `paginationMode` TEXT NOT NULL DEFAULT 'PAGE'")
            }
        }

        fun getDatabase(context: Context): AniFluxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AniFluxDatabase::class.java,
                    "aniflux_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
