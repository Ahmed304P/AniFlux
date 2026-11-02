package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "extension_repositories")
data class ExtensionRepoEntity(
    @PrimaryKey val repoId: String,
    val name: String,
    val url: String,
    val description: String = "",
    val author: String = "",
    val extensionCount: Int = 0,
    val compatibleCount: Int = 0,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "anime_extensions")
data class AnimeExtensionEntity(
    @PrimaryKey val extensionId: String, // Deterministic: "${repoId}::${pkgName}"
    val repoId: String,
    val repoName: String,
    val repoUrl: String,
    val pkgName: String,
    val name: String,
    val description: String,
    val version: String,
    val versionCode: Int = 1,
    val lang: String = "ar",
    val sourceUrl: String,
    val iconUrl: String = "",
    val streamTemplate: String = "",
    val qualities: String = "1080p,720p,480p",
    val engineVersion: Int = 2,
    val extensionFormat: String = "ANIFLUX_DECLARATIVE_V2",
    val adapterId: String = "UNIVERSAL_DECLARATIVE_V2",
    val runtimeEnvironment: String = "AniFlux Universal Runtime v3",
    val paginationType: String = "PAGE", // PAGE, CURSOR, OFFSET, MULTI_SECTION
    val isCompatible: Boolean = false,
    val compatibilityStatus: String = "UNVERIFIED", // COMPATIBLE, INCOMPATIBLE_ENGINE, BLOCKED_UNSAFE_CODE, INVALID_MANIFEST, UNREACHABLE_SOURCE
    val compatibilityReasonAr: String = "",
    val securityVerified: Boolean = false,
    val securityDetailsAr: String = "",
    val verificationStagesJson: String = "[]", // 9-Stage Real Verification Report JSON
    val isEnabled: Boolean = false, // Always disabled by default on initial import
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Canonical Unified Anime Record in the central Room Database.
 * Represents a single deduplicated anime work across all enabled extensions and base catalog.
 */
@Entity(
    tableName = "unified_anime_catalog",
    indices = [
        Index(value = ["malId"]),
        Index(value = ["anilistId"]),
        Index(value = ["activeSourcesCount"])
    ]
)
data class UnifiedAnimeEntity(
    @PrimaryKey val unifiedAnimeId: String,
    val malId: String = "",
    val anilistId: String = "",
    val originalTitle: String = "", // Primary Original English / Latin Romaji title from source
    val titleAr: String,
    val titleEn: String,
    val titleJp: String = "",
    val altTitlesPipe: String = "", // Pipe-separated normalized aliases for Smart Deduplication
    val posterUrl: String,
    val bannerUrl: String,
    val synopsisAr: String,
    val genresJson: String, // JSON array of Arabic genres
    val rating: Double = 8.5,
    val status: String = "مستمر", // "مستمر", "مكتمل", "قريباً"
    val type: String = "TV", // "TV", "Movie", "OVA", "ONA"
    val year: Int = 2024,
    val season: String = "ربيع", // "ربيع", "صيف", "خريف", "شتاء"
    val seasonNumber: Int = 1,
    val seasonsCount: Int = 1,
    val totalEpisodes: Int = 12,
    val currentEpisodes: Int = 12,
    val studio: String = "",
    val releaseDay: String = "الأحد",
    val ageRating: String = "+13",
    val isFeatured: Boolean = false,
    val featuredTag: String = "جديد الإضافات",
    val trailerUrl: String = "",
    val charactersJson: String = "[]", // JSON array of Person(name, imageUrl)
    val staffJson: String = "[]", // JSON array of Person(name, imageUrl)
    val sourceId: String = "",
    val sourceName: String = "",
    val sourceAnimeId: String = "",
    val activeSourcesCount: Int = 1,
    val hasBaseCatalogRecord: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Links an individual extension source to a canonical UnifiedAnimeEntity.
 * Retains provenance so disabling or updating one extension never corrupts another extension's data.
 */
@Entity(
    tableName = "extension_anime_links",
    indices = [
        Index(value = ["extensionId"]),
        Index(value = ["unifiedAnimeId"]),
        Index(value = ["isExtensionActive"])
    ]
)
data class ExtensionAnimeLinkEntity(
    @PrimaryKey val linkId: String, // "${extensionId}::${unifiedAnimeId}"
    val extensionId: String,
    val unifiedAnimeId: String,
    val sourceAnimeId: String,
    val sourceOriginalTitle: String = "",
    val sourceTitleAr: String = "",
    val sourceTitleEn: String = "",
    val sourceTitleJp: String = "",
    val sourcePosterUrl: String = "",
    val sourceBannerUrl: String = "",
    val sourceSynopsisAr: String = "",
    val sourceEpisodeCount: Int = 0,
    val isExtensionActive: Boolean = true,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Stores per-episode, per-extension watch servers and episode metadata.
 * Multiple extensions providing the same episode (or missing episodes) merge seamlessly by (unifiedAnimeId, episodeNumber).
 */
@Entity(
    tableName = "unified_episode_sources",
    indices = [
        Index(value = ["unifiedAnimeId", "episodeNumber"]),
        Index(value = ["unifiedEpisodeId"]),
        Index(value = ["extensionId"]),
        Index(value = ["isExtensionActive"])
    ]
)
data class UnifiedEpisodeSourceEntity(
    @PrimaryKey val recordId: String, // "${unifiedAnimeId}_ep_${episodeNumber}::${extensionId}::${serverKey}"
    val unifiedEpisodeId: String, // Canonical episode ID: "${unifiedAnimeId}_ep_${episodeNumber}"
    val unifiedAnimeId: String,
    val episodeNumber: Int,
    val extensionId: String,
    val extensionName: String,
    val episodeTitleAr: String,
    val episodeDescriptionAr: String = "",
    val thumbnailUrl: String = "",
    val durationMinutes: Int = 24,
    val releaseDate: String = "",
    val serverId: String,
    val serverNameAr: String,
    val quality: String = "1080p",
    val subtitleLang: String = "مترجم عربي",
    val streamUrl: String,
    val streamType: String = "MP4", // MP4, HLS, DASH, WEBM, EMBED
    val headersJson: String = "{}", // Custom HTTP headers (Referer, User-Agent, Origin)
    val subtitlesJson: String = "[]", // SubtitleTrack list JSON (SRT, VTT, ASS)
    val serverOrder: Int = 0,
    val isExtensionActive: Boolean = true,
    val syncedAt: Long = System.currentTimeMillis()
)

/**
 * Tracks synchronization status, pagination progress, and deduplication metrics for each extension.
 */
@Entity(tableName = "extension_sync_states")
data class ExtensionSyncStateEntity(
    @PrimaryKey val extensionId: String,
    val syncStatus: String = "IDLE", // IDLE, SYNCING, SUCCESS, ERROR, DISABLED
    val lastSyncedAt: Long = 0L,
    val totalDiscoveredAnime: Int = 0,
    val syncedAnimeCount: Int = 0,
    val mergedAnimeCount: Int = 0,
    val syncedEpisodesCount: Int = 0,
    val syncedServersCount: Int = 0,
    val syncedPagesCount: Int = 0,
    val totalPagesCount: Int = 0,
    val nextPageOrCursor: String = "",
    val paginationMode: String = "PAGE",
    val lastErrorAr: String = ""
)
