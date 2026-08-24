package com.example.data.model

data class Person(
    val name: String,
    val imageUrl: String
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
    val type: String, // "TV", "Movie", "OVA"
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
    val isCommentsLocked: Boolean = false,
    val trailerUrl: String = "",
    val characters: List<Person> = emptyList(),
    val staff: List<Person> = emptyList(),
    val serverId: String? = null,
    val customEpisodes: Map<String, String>? = null
)

data class Episode(
    val id: String,
    val animeId: String,
    val episodeNumber: Int,
    val titleAr: String,
    val durationMinutes: Int = 24,
    val thumbnailUrl: String,
    val releaseDate: String,
    val servers: List<StreamServer>
)

data class StreamServer(
    val id: String,
    val nameAr: String, // "سيرفر AniFlux FHD", "سيرفر cloud 720p", "سيرفر VIP 1080p", "سيرفر سريع SD"
    val quality: String, // "1080p", "720p", "480p"
    val streamUrl: String
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
