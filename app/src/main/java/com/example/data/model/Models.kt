package com.example.data.model

data class Person(
    val name: String,
    val imageUrl: String
)

data class SubtitleTrack(
    val langCode: String, // "ar", "en", "ja"
    val label: String, // "العربية (SRT)", "English (VTT)"
    val url: String,
    val format: String = "VTT" // "SRT", "VTT", "ASS", "SSA"
)

data class Anime(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val posterUrl: String,
    val bannerUrl: String,
    val rating: Double,
    val totalEpisodes: Int,
    val currentEpisodes: Int,
    val status: String, // "مستمر", "مكتمل", "قريباً"
    val type: String, // "TV", "Movie", "OVA", "ONA"
    val year: Int,
    val studio: String,
    val synopsisAr: String,
    val genres: List<String>,
    val releaseDay: String, // "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    val trendingRank: Int = 0,
    val isFeatured: Boolean = false,
    val featuredTag: String = "جديد الموسم",
    val ageRating: String = "+13", // "+13", "+17", "للجميع"
    val season: String = "ربيع", // "ربيع", "صيف", "خريف", "شتاء"
    val seasonsCount: Int = 1,
    val isCommentsLocked: Boolean = false,
    val trailerUrl: String = "",
    val characters: List<Person> = emptyList(),
    val staff: List<Person> = emptyList(),
    val serverId: String? = null,
    val customEpisodes: Map<String, String>? = null,
    val originalTitle: String = "",
    val titleJp: String = "",
    val altTitles: List<String> = emptyList(),
    val sourceId: String = "",
    val sourceName: String = "",
    val sourceAnimeId: String = "",
    val lastUpdated: Long = 0L
) {
    /**
     * Requirement 4: Always display the Original English or Latin/Romaji title from the source
     * as the primary title across the application, without auto-translating it to Arabic.
     */
    val primaryTitle: String
        get() = originalTitle
            .takeIf { it.isNotBlank() && !it.startsWith("Fake Anime") }
            ?: titleEn.takeIf { it.isNotBlank() && !it.startsWith("Fake Anime") }
            ?: titleJp.takeIf { it.isNotBlank() }
            ?: titleAr.ifBlank { titleEn }
}

data class Episode(
    val id: String,
    val animeId: String,
    val episodeNumber: Int,
    val titleAr: String,
    val durationMinutes: Int = 0,
    val thumbnailUrl: String,
    val releaseDate: String,
    val servers: List<StreamServer>,
    val descriptionAr: String = "",
    val sourceEpisodeId: String = "",
    val sourceAnimeId: String = "",
    val seasonNumber: Int = 1,
    val originalTitle: String = "",
    val status: String = "متاح",
    val episodePageUrl: String = "",
    val sourceId: String = ""
)

data class StreamServer(
    val id: String,
    val nameAr: String,
    val quality: String, // "4K", "1080p", "720p", "480p", "Auto"
    val streamUrl: String,
    val streamType: String = "MP4", // "MP4", "HLS", "DASH", "WEBM", "EMBED"
    val subtitleLang: String = "مترجم عربي",
    val headers: Map<String, String> = emptyMap(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val serverOrder: Int = 0,
    val sourceId: String = "",
    val sourceName: String = ""
)

data class CommentItem(
    val id: String,
    val authorName: String,
    val avatarUrl: String,
    val content: String,
    val timeAgo: String,
    val likesCount: Int,
    val isLikedByMe: Boolean = false
)

enum class LibraryCategory(val titleAr: String) {
    WATCHING("أتابعه حالياً"),
    FAVORITES("المفضلة"),
    PLAN_TO_WATCH("أخطط لمشاهدته"),
    COMPLETED("مكتمل"),
    HISTORY("سجل المشاهدة")
}

data class FilterState(
    val query: String = "",
    val selectedGenre: String? = null,
    val selectedStatus: String? = null,
    val selectedType: String? = null,
    val selectedSort: String = "الأحدث",
    val selectedYear: Int? = null,
    val selectedStudio: String? = null,
    val selectedAgeRating: String? = null,
    val selectedSeason: String? = null
)
