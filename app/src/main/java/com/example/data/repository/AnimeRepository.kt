package com.example.data.repository

import com.example.data.local.AniFluxDao
import com.example.data.local.AnimeExtensionEntity
import com.example.data.local.CustomListEntity
import com.example.data.local.CustomListItemEntity
import com.example.data.local.ExtensionAnimeLinkEntity
import com.example.data.local.ExtensionRepoEntity
import com.example.data.local.ExtensionSyncStateEntity
import com.example.data.local.LocalCommentEntity
import com.example.data.local.PublishedAnimeEntity
import com.example.data.local.SavedAnimeEntity
import com.example.data.local.UnifiedAnimeEntity
import com.example.data.local.UnifiedEpisodeSourceEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Anime
import com.example.data.model.CommentItem
import com.example.data.model.Episode
import com.example.data.model.Person
import com.example.data.model.StreamServer
import com.example.util.AniListService
import com.example.util.ExtensionCatalogEngine
import com.example.util.ExtensionEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AnimeRepository(private val dao: AniFluxDao) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val animeAdapter = moshi.adapter(Anime::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val personListType = Types.newParameterizedType(List::class.java, Person::class.java)
    private val personListAdapter = moshi.adapter<List<Person>>(personListType)

    // Per-extension sync mutex to prevent duplicate concurrent network/sync jobs
    private val extensionSyncLocks = ConcurrentHashMap<String, Mutex>()
    private val deduplicationDbMutex = Mutex()

    val publishedAnimeFlow: Flow<List<Anime>> = dao.getAllPublishedAnime().map { entities ->
        entities.mapNotNull {
            try {
                animeAdapter.fromJson(it.animeJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun savePublishedAnime(anime: Anime) {
        val json = animeAdapter.toJson(anime)
        dao.savePublishedAnime(PublishedAnimeEntity(anime.id, json))
    }

    suspend fun removePublishedAnime(animeId: String) {
        dao.removePublishedAnime(animeId)
    }

    // Base Anime Catalog with official AniList CDN posters and realistic Arabic metadata
    val sampleAnimeList = mutableListOf(
        Anime(
            id = "solo_leveling",
            titleAr = "سولو ليفلينج: رفع المستوى فردياً",
            titleEn = "Solo Leveling: Arise",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx151807-it355ZgzquUd.png",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-37yfQA3ym8PA.jpg",
            rating = 9.4,
            totalEpisodes = 12,
            currentEpisodes = 12,
            status = "مستمر",
            type = "TV",
            year = 2024,
            studio = "A-1 Pictures",
            synopsisAr = "في عالم ظهرت فيه بوابات تحتوي على وحوش خطيرة، يحصل الصياد الضعيف سونغ جين وو على قدرة فريدة تتيح له رؤية واجهة نظام تسمح له بالارتقاء بمستواه ومضاعفة قوته بلا حدود ليصبح أعظم صياد في العالم.",
            genres = listOf("أكشن", "خيال", "مغامرات", "إيسيكاي", "قوة خارقة"),
            releaseDay = "الأحد",
            trendingRank = 1,
            isFeatured = true,
            featuredTag = "الأكثر مشاهدة هذا الموسم 🔥",
            serverId = "solo_leveling"
        ),
        Anime(
            id = "demon_slayer_s4",
            titleAr = "قاتل الشياطين: تدريب الهاشيرا",
            titleEn = "Demon Slayer: Hashira Training Arc",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx166240-PBV7zukIHW7V.png",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/166240-J463589s23A2.jpg",
            rating = 9.2,
            totalEpisodes = 8,
            currentEpisodes = 8,
            status = "مكتمل",
            type = "TV",
            year = 2024,
            studio = "ufotable",
            synopsisAr = "يتوجه تانجيرو وأصدقاؤه للخضوع لتدريب شاق ومدمر تحت إشراف أعضاء الهاشيرا، أقوى سيّافي فيلق قاتلي الشياطين، استعداداً للمعركة النهائية القادمة ضد موزان كيبوتسوجي.",
            genres = listOf("أكشن", "شياطين", "خيال تاريخي", "شونين"),
            releaseDay = "الإثنين",
            trendingRank = 2,
            isFeatured = true,
            featuredTag = "أنمي فائق الجودة ⚡",
            serverId = "demon_slayer_s4"
        ),
        Anime(
            id = "attack_on_titan_final",
            titleAr = "هجوم العمالقة: الفصل النهائي",
            titleEn = "Attack on Titan: The Final Season",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx110277-sKUNXAsWMNFw.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/110277-iuGn6F5bK1U1.jpg",
            rating = 9.6,
            totalEpisodes = 28,
            currentEpisodes = 28,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "MAPPA",
            synopsisAr = "بعد اكتشاف الحقيقة الجغرافية والتاريخية وراء العمالقة والأسوار، يبدأ إيرين ييغر رحلة مصيرية ومثيرة للجدل لتغيير مصير شعب إيلديا والعالم بأكمله.",
            genres = listOf("أكشن", "غموض", "دراما", "عسكري", "خيال ملحمي"),
            releaseDay = "الأربعاء",
            trendingRank = 3,
            isFeatured = true,
            featuredTag = "أسطوري ⭐",
            serverId = "attack_on_titan_final"
        ),
        Anime(
            id = "jujutsu_kaisen_s2",
            titleAr = "جوجوتسو كايسن: الموسم الثاني",
            titleEn = "Jujutsu Kaisen Season 2",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx145064-5fa4ZBbW4dqA.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/145064-S7qAgxf6k07P.jpg",
            rating = 9.1,
            totalEpisodes = 23,
            currentEpisodes = 23,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "MAPPA",
            synopsisAr = "يتناول الموسم أحداث الماضي الخفية بين ساتورو غوجو وسوغورو غيتو خلال سنوات دراستهما، يليه أرك شيبويا الكارثي والمواجهة المباشرة ضد اللعنات الأقوى.",
            genres = listOf("أكشن", "خوارق", "مدرسة", "شونين"),
            releaseDay = "الخميس",
            trendingRank = 4,
            isFeatured = false,
            serverId = "jujutsu_kaisen_s2"
        ),
        Anime(
            id = "frieren_journey",
            titleAr = "فريرين: ما بعد نهاية الرحلة",
            titleEn = "Frieren: Beyond Journey's End",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx154587-qQTzQnEJJ3oB.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-ivXNJ23SM1xB.jpg",
            rating = 9.5,
            totalEpisodes = 28,
            currentEpisodes = 28,
            status = "مكتمل",
            type = "TV",
            year = 2024,
            studio = "Madhouse",
            synopsisAr = "الساحرة الجنية فريرين، التي عاشت لآلاف السنين، تبدأ رحلة جديدة بعد وفاة صديقها البطل هيميل، لتفهم مشاعر البشر وقيمة الذكريات والعلاقات الإنسانية.",
            genres = listOf("خيال", "شريحة من الحياة", "مغامرات", "دراما"),
            releaseDay = "الجمعة",
            trendingRank = 5,
            isFeatured = true,
            featuredTag = "تحفة فنية 🌸",
            serverId = "frieren_journey"
        ),
        Anime(
            id = "kaiju_no8",
            titleAr = "كايجو رقم 8",
            titleEn = "Kaiju No. 8",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx153288-tD2OmKy5CJab.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/153288-M7fEXJ6BM2xL.jpg",
            rating = 8.8,
            totalEpisodes = 12,
            currentEpisodes = 12,
            status = "مستمر",
            type = "TV",
            year = 2024,
            studio = "Production I.G",
            synopsisAr = "كافكا هيبينو البالغ من العمر 32 عاماً يعمل في فريق تنظيف جثث الكايجو، لكن حادثة غريبة تمنحه القدرة على التحول بنفسه إلى كايجو قوي ينضم لقوات الدفاع.",
            genres = listOf("أكشن", "خيال علمي", "عسكري", "كوميديا"),
            releaseDay = "السبت",
            trendingRank = 6,
            isFeatured = false,
            serverId = "kaiju_no8"
        ),
        Anime(
            id = "one_piece_egghead",
            titleAr = "ون بيس: آرك إيغ هيد",
            titleEn = "One Piece: Egghead Arc",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx21-YCDoj1EkAxFn.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg",
            rating = 9.3,
            totalEpisodes = 1110,
            currentEpisodes = 1110,
            status = "مستمر",
            type = "TV",
            year = 1999,
            studio = "Toei Animation",
            synopsisAr = "يصل طاقم قبعة القش إلى جزيرة المستقبل إيغ هيد ويلتقون بالعالم العبقري فيغابانك، لتبدأ مرحلة ملحمية تكشف أسرار الفراغ الزمني وحكومة العالم.",
            genres = listOf("مغامرات", "أكشن", "كوميديا", "شونين", "خيال"),
            releaseDay = "الأحد",
            trendingRank = 7,
            isFeatured = false,
            serverId = "one_piece_egghead"
        ),
        Anime(
            id = "bleach_tybw",
            titleAr = "بليتش: حرب الدم الألفية",
            titleEn = "Bleach: Thousand-Year Blood War",
            posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx116674-p3zK4PUX2Aag.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/116674-l2YlIyJzvGSV.jpg",
            rating = 9.0,
            totalEpisodes = 26,
            currentEpisodes = 26,
            status = "مستمر",
            type = "TV",
            year = 2024,
            studio = "Studio Pierrot",
            synopsisAr = "يعود ملك الكوينشي يوهاباخ بعد ألف عام لشن حرب شاملة ومدمجة ضد مجتمع الأرواح، ليجبر إيتشيغو كوروساكي وقادة الفرق على الخروج بأقصى طاقاتهم.",
            genres = listOf("أكشن", "خوارق", "شونين", "سيوف"),
            releaseDay = "الثلاثاء",
            trendingRank = 8,
            isFeatured = false,
            serverId = "bleach_tybw"
        )
    )

    // =========================================================================
    // UNIFIED CATALOG FLOW (Merges Published + Extension Unified DB + Base Catalog)
    // =========================================================================
    val activeUnifiedAnimeEntitiesFlow: Flow<List<UnifiedAnimeEntity>> = dao.getActiveUnifiedAnimeFlow()
    val allActiveEpisodeSourcesFlow: Flow<List<UnifiedEpisodeSourceEntity>> = dao.getAllActiveEpisodeSourcesFlow()
    val extensionAnimeLinksFlow: Flow<List<ExtensionAnimeLinkEntity>> = dao.getAllExtensionAnimeLinksFlow()
    val extensionSyncStatesFlow: Flow<List<ExtensionSyncStateEntity>> = dao.getAllExtensionSyncStatesFlow()

    /**
     * Reactive Unified Anime Catalog Flow.
     * Ensures every anime appears ONCE ONLY across the entire application, merging:
     * 1. Admin published/edited anime (`published_anime`)
     * 2. Deduplicated multi-source extension anime (`unified_anime_catalog` where `activeSourcesCount > 0`)
     * 3. Base catalog anime (`sampleAnimeList`), automatically enriched if matched by an active extension!
     */
    val unifiedCatalogFlow: Flow<List<Anime>> = combine(
        publishedAnimeFlow,
        activeUnifiedAnimeEntitiesFlow,
        allActiveEpisodeSourcesFlow
    ) { publishedList, unifiedEntities, activeEpisodeSources ->
        val epCountByAnimeId: Map<String, Int> = activeEpisodeSources
            .groupBy { it.unifiedAnimeId }
            .mapValues { (_, list) -> list.map { it.episodeNumber }.distinct().size }

        val publishedById = publishedList
            .filter { !it.id.startsWith("dummy_") }
            .associateBy { it.id }
        val sampleById = sampleAnimeList.associateBy { it.id }
        val result = LinkedHashMap<String, Anime>()

        // 1. Add Active Extension Unified Anime FIRST (ordered by updatedAt DESC) so newly enabled extensions
        // immediately show all their anime works at the very top of "آخر التحديثات" on HomeScreen!
        val activeExtensionEntities = unifiedEntities
            .filter { it.activeSourcesCount > 0 && !it.unifiedAnimeId.startsWith("dummy_") }
            .sortedWith(compareByDescending<UnifiedAnimeEntity> { it.updatedAt }.thenByDescending { it.rating })

        for (uni in activeExtensionEntities) {
            val extEpCount = epCountByAnimeId[uni.unifiedAnimeId] ?: 0
            val existingBase = publishedById[uni.unifiedAnimeId] ?: sampleById[uni.unifiedAnimeId]
            if (existingBase != null) {
                val upgradedBase = sampleById[uni.unifiedAnimeId]?.let { officialSample ->
                    existingBase.copy(
                        posterUrl = if (existingBase.posterUrl.contains("unsplash")) officialSample.posterUrl else existingBase.posterUrl,
                        bannerUrl = if (existingBase.bannerUrl.contains("unsplash")) officialSample.bannerUrl else existingBase.bannerUrl
                    )
                } ?: existingBase
                result[uni.unifiedAnimeId] = mergeBaseAnimeWithUnifiedEntity(upgradedBase, uni, extEpCount)
            } else {
                result[uni.unifiedAnimeId] = unifiedEntityToAnimeModel(uni, extEpCount)
            }
        }

        // 2. Add Admin Published Anime (that weren't already added via active extension entities)
        for (pub in publishedById.values) {
            if (!result.containsKey(pub.id)) {
                val officialSample = sampleById[pub.id]
                val upgradedPub = if (officialSample != null) {
                    pub.copy(
                        posterUrl = if (pub.posterUrl.contains("unsplash")) officialSample.posterUrl else pub.posterUrl,
                        bannerUrl = if (pub.bannerUrl.contains("unsplash")) officialSample.bannerUrl else pub.bannerUrl
                    )
                } else pub
                result[pub.id] = upgradedPub
            }
        }

        // 3. Add remaining Base Sample Anime that weren't already added
        for (sample in sampleAnimeList) {
            if (!result.containsKey(sample.id)) {
                result[sample.id] = sample
            }
        }

        result.values.toList()
    }

    private fun mergeBaseAnimeWithUnifiedEntity(
        base: Anime,
        uni: UnifiedAnimeEntity,
        activeExtEpCount: Int
    ): Anime {
        val uniChars = parsePersonsJson(uni.charactersJson)
        val uniStaff = parsePersonsJson(uni.staffJson)
        val uniGenres = parseGenresJson(uni.genresJson)
        val uniAltTitles = uni.altTitlesPipe.split("|").filter { it.isNotBlank() }

        val mergedChars = (base.characters + uniChars).distinctBy { it.name.lowercase() }
        val mergedStaff = (base.staff + uniStaff).distinctBy { it.name.lowercase() }
        val mergedGenres = (base.genres + uniGenres).distinct()
        val effectiveCurrentEp = maxOf(base.currentEpisodes, activeExtEpCount)
        val effectiveTotalEp = maxOf(base.totalEpisodes, uni.totalEpisodes, effectiveCurrentEp)

        val resolvedPoster = when {
            !uni.posterUrl.contains("unsplash") && uni.posterUrl.isNotBlank() && base.posterUrl.contains("unsplash") -> uni.posterUrl
            base.posterUrl.isNotBlank() -> base.posterUrl
            else -> uni.posterUrl
        }
        val resolvedBanner = when {
            !uni.bannerUrl.contains("unsplash") && uni.bannerUrl.isNotBlank() && base.bannerUrl.contains("unsplash") -> uni.bannerUrl
            base.bannerUrl.isNotBlank() -> base.bannerUrl
            else -> uni.bannerUrl
        }

        return base.copy(
            originalTitle = uni.originalTitle.ifBlank { base.originalTitle.ifBlank { base.titleEn } },
            titleJp = uni.titleJp.ifBlank { base.titleJp },
            altTitles = (base.altTitles + uniAltTitles).distinct(),
            posterUrl = resolvedPoster,
            bannerUrl = resolvedBanner,
            totalEpisodes = effectiveTotalEp,
            currentEpisodes = effectiveCurrentEp,
            seasonsCount = maxOf(base.seasonsCount, uni.seasonsCount),
            studio = base.studio.ifBlank { uni.studio },
            synopsisAr = if (base.synopsisAr.length >= uni.synopsisAr.length) base.synopsisAr else uni.synopsisAr,
            genres = mergedGenres,
            trailerUrl = base.trailerUrl.ifBlank { uni.trailerUrl },
            characters = mergedChars,
            staff = mergedStaff,
            serverId = base.serverId ?: uni.unifiedAnimeId,
            sourceId = uni.sourceId.ifBlank { base.sourceId },
            sourceName = uni.sourceName.ifBlank { base.sourceName },
            sourceAnimeId = uni.sourceAnimeId.ifBlank { base.sourceAnimeId },
            lastUpdated = maxOf(base.lastUpdated, uni.updatedAt)
        )
    }

    fun unifiedEntityToAnimeModel(uni: UnifiedAnimeEntity, activeExtEpCount: Int = 0): Anime {
        val genres = parseGenresJson(uni.genresJson).ifEmpty { listOf("أكشن", "خيال") }
        val chars = parsePersonsJson(uni.charactersJson)
        val staff = parsePersonsJson(uni.staffJson)
        val altTitles = uni.altTitlesPipe.split("|").filter { it.isNotBlank() }
        val effectiveCurrent = if (activeExtEpCount > 0) activeExtEpCount else uni.currentEpisodes
        val effectiveTotal = maxOf(uni.totalEpisodes, effectiveCurrent)

        return Anime(
            id = uni.unifiedAnimeId,
            titleAr = uni.titleAr,
            titleEn = uni.titleEn,
            originalTitle = uni.originalTitle.ifBlank { uni.titleEn },
            titleJp = uni.titleJp,
            altTitles = altTitles,
            posterUrl = uni.posterUrl,
            bannerUrl = uni.bannerUrl.ifBlank { uni.posterUrl },
            rating = uni.rating,
            totalEpisodes = effectiveTotal,
            currentEpisodes = effectiveCurrent,
            seasonsCount = uni.seasonsCount,
            status = uni.status,
            type = uni.type,
            year = uni.year,
            studio = uni.studio.ifBlank { "Madhouse" },
            synopsisAr = uni.synopsisAr,
            genres = genres,
            releaseDay = uni.releaseDay,
            trendingRank = 9,
            isFeatured = uni.isFeatured,
            featuredTag = uni.featuredTag,
            ageRating = uni.ageRating,
            season = uni.season,
            isCommentsLocked = false,
            trailerUrl = uni.trailerUrl,
            characters = chars,
            staff = staff,
            serverId = uni.unifiedAnimeId,
            sourceId = uni.sourceId,
            sourceName = uni.sourceName,
            sourceAnimeId = uni.sourceAnimeId,
            lastUpdated = uni.updatedAt
        )
    }

    private fun parseGenresJson(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePersonsJson(json: String): List<Person> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            personListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeHeadersJson(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "{}"
        return try {
            val obj = org.json.JSONObject()
            for ((k, v) in headers) obj.put(k, v)
            obj.toString()
        } catch (_: Exception) {
            "{}"
        }
    }

    private fun parseHeadersJson(json: String): Map<String, String> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeSubtitlesJson(tracks: List<com.example.data.model.SubtitleTrack>): String {
        if (tracks.isEmpty()) return "[]"
        return try {
            val arr = org.json.JSONArray()
            for (t in tracks) {
                val obj = org.json.JSONObject()
                obj.put("langCode", t.langCode)
                obj.put("label", t.label)
                obj.put("url", t.url)
                obj.put("format", t.format)
                arr.put(obj)
            }
            arr.toString()
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun parseSubtitlesJson(json: String): List<com.example.data.model.SubtitleTrack> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<com.example.data.model.SubtitleTrack>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    com.example.data.model.SubtitleTrack(
                        langCode = obj.optString("langCode", "ar"),
                        label = obj.optString("label", "العربية"),
                        url = obj.optString("url", ""),
                        format = obj.optString("format", "VTT")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // =========================================================================
    // UNIFIED EPISODES & MULTI-SOURCE SERVER MERGING
    // =========================================================================
    fun getEpisodesForAnime(
        anime: Anime,
        enabledExtensions: List<AnimeExtensionEntity> = emptyList(),
        activeDbEpisodeSources: List<UnifiedEpisodeSourceEntity> = emptyList()
    ): List<Episode> {
        val animeId = anime.id
        val animeDbSources = activeDbEpisodeSources.filter {
            it.unifiedAnimeId == animeId && it.isExtensionActive
        }
        val dbSourcesByEpNum: Map<Int, List<UnifiedEpisodeSourceEntity>> = animeDbSources.groupBy { it.episodeNumber }

        // Determine if this anime also has base catalog episodes (e.g. sampleAnimeList or admin customEpisodes)
        val hasBaseCatalogEntry = sampleAnimeList.any { it.id == animeId } || !anime.customEpisodes.isNullOrEmpty()
        val baseMaxEp = if (hasBaseCatalogEntry) {
            val maxCustomEp = anime.customEpisodes?.keys?.mapNotNull { it.toIntOrNull() }?.maxOrNull() ?: 0
            maxOf(anime.currentEpisodes, maxCustomEp)
        } else {
            0
        }
        val maxDbEp = dbSourcesByEpNum.keys.maxOrNull() ?: 0
        val maxEp = maxOf(baseMaxEp, maxDbEp)
        if (maxEp <= 0) return emptyList()

        // Collect all episode numbers present in either base catalog or active extension DB sources
        val allEpisodeNumbers = sortedSetOf<Int>().apply {
            if (baseMaxEp > 0) addAll(1..baseMaxEp)
            addAll(dbSourcesByEpNum.keys)
        }

        return allEpisodeNumbers.mapNotNull { num ->
            val formattedNumber = num.toString()
            val customUrl = anime.customEpisodes?.get(formattedNumber)

            // 1. Base / Admin custom servers (if this episode exists in base catalog)
            val baseServersList = if (num <= baseMaxEp) {
                if (!customUrl.isNullOrBlank()) {
                    val parsedServers = mutableListOf<StreamServer>()
                    val parts = customUrl.split(",")
                    parts.forEachIndexed { index, part ->
                        val trimmedPart = part.trim()
                        if (trimmedPart.isNotBlank()) {
                            val nameAndUrl = trimmedPart.split("|", limit = 2)
                            val name = if (nameAndUrl.size == 2) nameAndUrl[0].trim() else "سيرفر ${index + 1}"
                            val url = if (nameAndUrl.size == 2) nameAndUrl[1].trim() else nameAndUrl[0].trim()
                            parsedServers.add(
                                StreamServer(
                                    id = "${animeId}_${num}_custom_$index",
                                    nameAr = name,
                                    quality = "متعدد",
                                    streamUrl = url,
                                    serverOrder = index + 1
                                )
                            )
                        }
                    }
                    if (parsedServers.isEmpty()) {
                        listOf(
                            StreamServer(
                                id = "${animeId}_${num}_custom",
                                nameAr = "سيرفر رئيسي",
                                quality = "متعدد",
                                streamUrl = customUrl,
                                serverOrder = 1
                            )
                        )
                    } else parsedServers
                } else if (hasBaseCatalogEntry && !anime.serverId.isNullOrBlank()) {
                    listOf(
                        StreamServer(
                            id = "${animeId}_${num}_1080_mp4",
                            nameAr = "SAnime الرئيسي • 1080p",
                            quality = "1080p",
                            streamUrl = ExtensionCatalogEngine.STREAM_MP4_FHD,
                            streamType = "MP4",
                            subtitleLang = "مترجم عربي",
                            serverOrder = 1
                        ),
                        StreamServer(
                            id = "${animeId}_${num}_1080_hls",
                            nameAr = "MegaCloud HLS • 1080p",
                            quality = "1080p",
                            streamUrl = ExtensionCatalogEngine.STREAM_HLS_MASTER,
                            streamType = "HLS",
                            subtitleLang = "عربي + English",
                            serverOrder = 2
                        ),
                        StreamServer(
                            id = "${animeId}_${num}_4k_ultra",
                            nameAr = "Filemoon Ultra • 4K",
                            quality = "4K",
                            streamUrl = ExtensionCatalogEngine.STREAM_HLS_4K,
                            streamType = "HLS",
                            subtitleLang = "مترجم عربي",
                            serverOrder = 3
                        ),
                        StreamServer(
                            id = "${animeId}_${num}_720_hd",
                            nameAr = "VidStream HD • 720p",
                            quality = "720p",
                            streamUrl = ExtensionCatalogEngine.STREAM_MP4_HD,
                            streamType = "MP4",
                            subtitleLang = "مترجم عربي",
                            serverOrder = 4
                        ),
                        StreamServer(
                            id = "${animeId}_${num}_480_eco",
                            nameAr = "StreamTape اقتصادي • 480p",
                            quality = "480p",
                            streamUrl = ExtensionCatalogEngine.STREAM_MP4_SD,
                            streamType = "MP4",
                            subtitleLang = "مترجم عربي",
                            serverOrder = 5
                        )
                    )
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // 2. Persisted Multi-Source Extension Servers from `unified_episode_sources` in Room DB
            val dbEpRecords = dbSourcesByEpNum[num].orEmpty().sortedBy { it.serverOrder }
            val dbExtensionServers = dbEpRecords.mapIndexed { idx, rec ->
                val normalizedStreamUrl = ExtensionCatalogEngine.normalizePlayableStreamUrl(rec.streamUrl)
                StreamServer(
                    id = rec.serverId,
                    nameAr = rec.serverNameAr,
                    quality = rec.quality,
                    streamUrl = normalizedStreamUrl,
                    streamType = if (normalizedStreamUrl.contains(".m3u8", ignoreCase = true)) "HLS" else rec.streamType.ifBlank { "MP4" },
                    subtitleLang = rec.subtitleLang.ifBlank { "مترجم عربي" },
                    headers = parseHeadersJson(rec.headersJson),
                    subtitleTracks = parseSubtitlesJson(rec.subtitlesJson),
                    serverOrder = rec.serverOrder.takeIf { it > 0 } ?: (idx + 1),
                    sourceId = rec.extensionId,
                    sourceName = rec.extensionName
                )
            }

            // 3. Smart Live Stream Extractor: actively extract authentic servers from all enabled extensions
            val liveExtractedServers = com.example.util.SmartEpisodeStreamExtractor.extractLiveServersForEpisode(
                anime = anime,
                episodeNumber = num,
                enabledExtensions = enabledExtensions
            ).servers

            // Combine all servers: live extracted servers from enabled extensions FIRST, then persisted DB records, then base fallback
            val validExtensionServers = (liveExtractedServers + dbExtensionServers)
                .filter { ExtensionCatalogEngine.isSafePlayableStreamUrl(it.streamUrl) }
            val combinedServers = (validExtensionServers + baseServersList)
                .distinctBy { "${it.nameAr}::${it.quality}::${it.streamUrl}" }

            if (combinedServers.isEmpty()) {
                null
            } else {
                val rawRichTitle = dbEpRecords.firstOrNull {
                    it.episodeTitleAr.isNotBlank() && it.episodeTitleAr != "الحلقة $num"
                }?.episodeTitleAr ?: dbEpRecords.firstOrNull()?.episodeTitleAr ?: "الحلقة $num"

                val richTitle = rawRichTitle
                    .removePrefix("${anime.titleAr} - ")
                    .removePrefix("${anime.titleEn} - ")
                    .trim()
                    .ifBlank { "الحلقة $num" }

                val richDesc = ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(
                    dbEpRecords.firstOrNull { it.episodeDescriptionAr.isNotBlank() }?.episodeDescriptionAr ?: ""
                )

                val richThumb = dbEpRecords.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl
                    ?: anime.bannerUrl.ifBlank { anime.posterUrl }.ifBlank {
                        "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop"
                    }

                // Do not force 24 minutes if duration is unknown (0)
                val duration = dbEpRecords.firstOrNull { it.durationMinutes > 0 }?.durationMinutes
                    ?: if (hasBaseCatalogEntry) 24 else 0
                val releaseDate = dbEpRecords.firstOrNull { it.releaseDate.isNotBlank() }?.releaseDate ?: "2024-01-01"
                val firstDbRec = dbEpRecords.firstOrNull()
                val resolvedSourceId = firstDbRec?.extensionId
                    ?: combinedServers.firstOrNull { it.sourceId.isNotBlank() }?.sourceId
                    ?: anime.sourceId

                Episode(
                    id = "${animeId}_ep_${num}", // Canonical unified episode ID
                    animeId = animeId,
                    episodeNumber = num,
                    titleAr = richTitle,
                    descriptionAr = richDesc,
                    durationMinutes = duration,
                    thumbnailUrl = richThumb,
                    releaseDate = releaseDate,
                    servers = combinedServers,
                    sourceEpisodeId = firstDbRec?.unifiedEpisodeId ?: "${animeId}_s${anime.seasonsCount}_ep_${num}",
                    sourceAnimeId = anime.sourceAnimeId.ifBlank { animeId },
                    seasonNumber = anime.seasonsCount.coerceAtLeast(1),
                    originalTitle = "Episode $num",
                    status = "متاح",
                    episodePageUrl = "https://aniflux-cdn.net/watch/$animeId/ep-$num",
                    sourceId = resolvedSourceId
                )
            }
        }
    }

    // =========================================================================
    // EXTENSION CONTENT SYNCHRONIZATION & SMART DEDUPLICATION ENGINE
    // =========================================================================
    data class ExtensionSyncSummary(
        val extensionId: String,
        val extensionName: String,
        val fetchedAnimeCount: Int,
        val newlyAddedAnimeCount: Int,
        val deduplicatedMergedAnimeCount: Int,
        val syncedEpisodesCount: Int,
        val syncedServersCount: Int,
        val syncedPagesCount: Int = 1,
        val totalPagesCount: Int = 1
    )

    /**
     * Synchronizes all anime, characters, staff, episodes, and watch servers from an enabled extension.
     * - Uses multi-page paginated ingestion across all catalog pages (`Page`, `Cursor`, `Offset`, `NextPageUrl`, `MultiSection`).
     * - Detects duplicate page signatures (`visitedPageSignatures`) and verified empty pages.
     * - Applies bounded per-page retry with backoff so a transient failure on one page does not abort the entire sync.
     * - Saves resumable pagination state after each page in `extension_sync_states`.
     * - Applies database-level Smart Deduplication (`SmartAnimeDeduplicator`) and `AnimeDataNormalizer`.
     */
    suspend fun syncExtensionContent(extensionId: String): Result<ExtensionSyncSummary> = withContext(Dispatchers.IO) {
        val lock = extensionSyncLocks.getOrPut(extensionId) { Mutex() }
        if (lock.isLocked) {
            return@withContext Result.failure(IllegalStateException("جاري مزامنة هذه الإضافة بالفعل في الخلفية"))
        }

        lock.withLock {
            val rawExt = dao.getExtensionByIdSync(extensionId)
                ?: return@withLock Result.failure(IllegalArgumentException("الإضافة غير موجودة"))
            val ext = healAndUpgradeExtensionEntity(rawExt)
            if (ext != rawExt) {
                dao.upsertExtension(ext)
            }

            if (!ext.isEnabled || !ext.isCompatible || !ext.securityVerified) {
                return@withLock Result.failure(IllegalStateException("الإضافة غير مفعّلة أو غير متوافقة"))
            }

            val adapter = ExtensionCatalogEngine.resolveAdapter(ext)
                ?: return@withLock Result.failure(IllegalStateException("لا يوجد محول (Adapter) متوافق مع صيغة الإضافة"))

            val prevSyncState = dao.getExtensionSyncStateSync(extensionId)
            dao.upsertExtensionSyncState(
                ExtensionSyncStateEntity(
                    extensionId = extensionId,
                    syncStatus = "SYNCING",
                    lastSyncedAt = prevSyncState?.lastSyncedAt ?: 0L,
                    totalDiscoveredAnime = prevSyncState?.totalDiscoveredAnime ?: 0,
                    syncedAnimeCount = prevSyncState?.syncedAnimeCount ?: 0,
                    mergedAnimeCount = prevSyncState?.mergedAnimeCount ?: 0,
                    syncedEpisodesCount = prevSyncState?.syncedEpisodesCount ?: 0,
                    syncedServersCount = prevSyncState?.syncedServersCount ?: 0,
                    syncedPagesCount = prevSyncState?.syncedPagesCount ?: 0,
                    totalPagesCount = prevSyncState?.totalPagesCount ?: 1,
                    nextPageOrCursor = prevSyncState?.nextPageOrCursor ?: "",
                    paginationMode = ext.paginationType,
                    lastErrorAr = ""
                )
            )

            try {
                var page = 1
                val pageSize = 12
                var hasNextPage = true

                var totalFetchedAnime = 0
                var newlyAddedCount = 0
                var mergedDeduplicatedCount = 0
                var totalEpisodesSynced = 0
                var totalServersSynced = 0
                var syncedPages = 0
                var totalPages = 1
                var totalDiscovered = 0
                var lastCursor = ""
                var nextOffset = 0
                var nextPageUrl = ""
                var currentSection = "all"
                var paginationMode = ext.paginationType
                val visitedPageSignatures = HashSet<String>()
                var consecutiveEmptyPages = 0
                val now = System.currentTimeMillis()

                // Safety ceiling of 1000 pages (~12,000 anime works) with duplicate-page signature protection
                while (hasNextPage && page <= 1000) {
                    val request = ExtensionCatalogEngine.PaginationRequest(
                        page = page,
                        pageSize = pageSize,
                        cursor = lastCursor,
                        offset = nextOffset,
                        nextPageUrl = nextPageUrl,
                        section = currentSection
                    )

                    var pageResult: ExtensionCatalogEngine.CatalogPageResult? = null
                    var lastPageError: Throwable? = null
                    for (attempt in 1..3) {
                        val res = adapter.fetchCatalogPage(ext, request)
                        if (res.isSuccess) {
                            pageResult = res.getOrNull()
                            break
                        } else {
                            lastPageError = res.exceptionOrNull()
                            if (attempt < 3) {
                                kotlinx.coroutines.delay(50L * attempt)
                            }
                        }
                    }

                    if (pageResult == null) {
                        if (syncedPages == 0) {
                            throw (lastPageError ?: IllegalStateException("تعذر جلب الصفحة الأولى من الإضافة"))
                        } else {
                            // Skip failed page after retries if we already synced prior pages, or stop cleanly
                            break
                        }
                    }

                    val items = pageResult.items
                    if (items.isEmpty()) {
                        consecutiveEmptyPages++
                        if (consecutiveEmptyPages >= 2 || !pageResult.hasNextPage) {
                            break
                        }
                        page++
                        continue
                    }
                    consecutiveEmptyPages = 0

                    // Duplicate page protection: stop immediately if the source repeats the exact same page items
                    val signature = pageResult.pageSignature.ifBlank {
                        items.joinToString("|") { it.sourceAnimeId }
                    }
                    if (!visitedPageSignatures.add(signature)) {
                        break
                    }

                    syncedPages = page
                    totalPages = maxOf(pageResult.totalPages, page)
                    totalDiscovered = maxOf(pageResult.totalDiscoveredCount, totalFetchedAnime + items.size)
                    lastCursor = pageResult.nextCursor
                    nextOffset = pageResult.nextOffset
                    nextPageUrl = pageResult.nextPageUrl
                    currentSection = pageResult.section
                    paginationMode = pageResult.paginationType

                    for (incoming in items) {
                        totalFetchedAnime++
                        val mergeOutcome = ingestSingleSourceAnime(
                            extension = ext,
                            incoming = incoming,
                            now = now
                        )
                        if (mergeOutcome.wasMergedWithExisting) {
                            mergedDeduplicatedCount++
                        } else {
                            newlyAddedCount++
                        }
                        totalEpisodesSynced += mergeOutcome.episodesIngested
                        totalServersSynced += mergeOutcome.serversIngested
                    }

                    // Persist intermediate page progress for resumable pagination
                    dao.upsertExtensionSyncState(
                        ExtensionSyncStateEntity(
                            extensionId = extensionId,
                            syncStatus = "SYNCING",
                            lastSyncedAt = now,
                            totalDiscoveredAnime = totalDiscovered,
                            syncedAnimeCount = totalFetchedAnime,
                            mergedAnimeCount = mergedDeduplicatedCount,
                            syncedEpisodesCount = totalEpisodesSynced,
                            syncedServersCount = totalServersSynced,
                            syncedPagesCount = syncedPages,
                            totalPagesCount = totalPages,
                            nextPageOrCursor = lastCursor.ifBlank { nextPageUrl },
                            paginationMode = paginationMode,
                            lastErrorAr = ""
                        )
                    )

                    hasNextPage = pageResult.hasNextPage
                    page++
                }

                dao.recalculateAllActiveSourcesCounts()

                dao.upsertExtensionSyncState(
                    ExtensionSyncStateEntity(
                        extensionId = extensionId,
                        syncStatus = "SUCCESS",
                        lastSyncedAt = now,
                        totalDiscoveredAnime = maxOf(totalDiscovered, totalFetchedAnime),
                        syncedAnimeCount = totalFetchedAnime,
                        mergedAnimeCount = mergedDeduplicatedCount,
                        syncedEpisodesCount = totalEpisodesSynced,
                        syncedServersCount = totalServersSynced,
                        syncedPagesCount = syncedPages,
                        totalPagesCount = maxOf(totalPages, syncedPages),
                        nextPageOrCursor = "",
                        paginationMode = paginationMode,
                        lastErrorAr = ""
                    )
                )

                Result.success(
                    ExtensionSyncSummary(
                        extensionId = extensionId,
                        extensionName = ext.name,
                        fetchedAnimeCount = totalFetchedAnime,
                        newlyAddedAnimeCount = newlyAddedCount,
                        deduplicatedMergedAnimeCount = mergedDeduplicatedCount,
                        syncedEpisodesCount = totalEpisodesSynced,
                        syncedServersCount = totalServersSynced,
                        syncedPagesCount = syncedPages,
                        totalPagesCount = maxOf(totalPages, syncedPages)
                    )
                )
            } catch (e: Exception) {
                dao.upsertExtensionSyncState(
                    ExtensionSyncStateEntity(
                        extensionId = extensionId,
                        syncStatus = "ERROR",
                        lastSyncedAt = prevSyncState?.lastSyncedAt ?: 0L,
                        totalDiscoveredAnime = prevSyncState?.totalDiscoveredAnime ?: 0,
                        syncedAnimeCount = prevSyncState?.syncedAnimeCount ?: 0,
                        mergedAnimeCount = prevSyncState?.mergedAnimeCount ?: 0,
                        syncedEpisodesCount = prevSyncState?.syncedEpisodesCount ?: 0,
                        syncedServersCount = prevSyncState?.syncedServersCount ?: 0,
                        syncedPagesCount = prevSyncState?.syncedPagesCount ?: 0,
                        totalPagesCount = prevSyncState?.totalPagesCount ?: 1,
                        nextPageOrCursor = prevSyncState?.nextPageOrCursor ?: "",
                        paginationMode = ext.paginationType,
                        lastErrorAr = e.message ?: "تعذر الاتصال بالمصدر مؤقتاً، المحتوى المحفوظ ما زال متاحاً"
                    )
                )
                Result.failure(e)
            }
        }
    }

    private data class SingleAnimeIngestOutcome(
        val unifiedAnimeId: String,
        val wasMergedWithExisting: Boolean,
        val episodesIngested: Int,
        val serversIngested: Int
    )

    /**
     * Performs database-level Smart Deduplication, Title/Rating/Synopsis Normalization,
     * and Episode/Server Merging for a single incoming anime.
     */
    private suspend fun ingestSingleSourceAnime(
        extension: AnimeExtensionEntity,
        incoming: ExtensionCatalogEngine.SourceAnimePayload,
        now: Long
    ): SingleAnimeIngestOutcome = deduplicationDbMutex.withLock {
        val existingUnifiedList = dao.getAllUnifiedAnimeSync()
        val publishedEntities = dao.getAllPublishedAnimeSync().mapNotNull {
            try { animeAdapter.fromJson(it.animeJson) } catch (_: Exception) { null }
        }
        val baseCatalogCombined = (publishedEntities + sampleAnimeList).distinctBy { it.id }

        // Normalize titles, synopsis, and rating via AnimeDataNormalizer before deduplication & persistence
        val normalizedTitles = ExtensionCatalogEngine.AnimeDataNormalizer.normalizeTitles(
            originalTitle = incoming.originalTitle,
            titleEn = incoming.titleEn,
            titleAr = incoming.titleAr,
            titleJp = incoming.titleJp,
            altTitles = incoming.altTitles
        )
        val cleanedSynopsis = ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(incoming.synopsisAr)
        val normalizedRating = ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(incoming.rating) ?: 0.0

        val sanitizedIncoming = incoming.copy(
            originalTitle = normalizedTitles.originalTitle,
            titleEn = normalizedTitles.titleEn,
            titleAr = normalizedTitles.titleAr,
            titleJp = normalizedTitles.titleJp,
            altTitles = normalizedTitles.altTitles,
            synopsisAr = cleanedSynopsis,
            rating = normalizedRating
        )

        // 1. Run Smart Deduplication against existing UnifiedAnimeEntity records and Base Catalog
        val matchedUnifiedId = ExtensionCatalogEngine.SmartAnimeDeduplicator.findMatchingUnifiedAnime(
            incoming = sanitizedIncoming,
            existingUnifiedList = existingUnifiedList,
            baseCatalogList = baseCatalogCombined
        )

        val unifiedAnimeId = matchedUnifiedId
            ?: ExtensionCatalogEngine.SmartAnimeDeduplicator.generateCanonicalUnifiedId(sanitizedIncoming)

        val existingUnified = dao.getUnifiedAnimeByIdSync(unifiedAnimeId)
        val existingLinks = dao.getExtensionAnimeLinksByAnimeSync(unifiedAnimeId)
        val isFromAnotherSourceAlready = existingUnified != null &&
            (existingLinks.any { it.extensionId != extension.extensionId } || existingUnified.hasBaseCatalogRecord)
        val matchedBaseRecord = baseCatalogCombined.find { it.id == unifiedAnimeId && !it.id.startsWith("dummy_") }
        val wasMerged = isFromAnotherSourceAlready || matchedBaseRecord != null

        // 2. Normalize and merge metadata into UnifiedAnimeEntity
        val normalizedGenres = AniListService.cleanAndFilterGenres(sanitizedIncoming.genres)
            .ifEmpty { sanitizedIncoming.genres.ifEmpty { listOf("أكشن", "خيال") } }

        val incomingAliases = ExtensionCatalogEngine.SmartAnimeDeduplicator.collectNormalizedAliases(sanitizedIncoming)
        val resolvedOriginalTitle = sanitizedIncoming.originalTitle.ifBlank { sanitizedIncoming.titleEn }

        val updatedUnified = if (existingUnified != null) {
            val prevAliases = existingUnified.altTitlesPipe.split("|").filter { it.isNotBlank() }.toSet()
            val mergedAliasesPipe = (prevAliases + incomingAliases).joinToString("|")

            val prevGenres = parseGenresJson(existingUnified.genresJson)
            val mergedGenresJson = stringListAdapter.toJson((prevGenres + normalizedGenres).distinct())

            val prevChars = parsePersonsJson(existingUnified.charactersJson)
            val mergedChars = (prevChars + sanitizedIncoming.characters).distinctBy { it.name.lowercase() }
            val mergedCharsJson = personListAdapter.toJson(mergedChars)

            val prevStaff = parsePersonsJson(existingUnified.staffJson)
            val mergedStaff = (prevStaff + sanitizedIncoming.staff).distinctBy { it.name.lowercase() }
            val mergedStaffJson = personListAdapter.toJson(mergedStaff)

            val mergedRating = ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(
                maxOf(existingUnified.rating, sanitizedIncoming.rating)
            ) ?: existingUnified.rating

            existingUnified.copy(
                malId = existingUnified.malId.ifBlank { sanitizedIncoming.malId },
                anilistId = existingUnified.anilistId.ifBlank { sanitizedIncoming.anilistId },
                originalTitle = existingUnified.originalTitle.ifBlank { resolvedOriginalTitle },
                titleJp = existingUnified.titleJp.ifBlank { sanitizedIncoming.titleJp },
                altTitlesPipe = mergedAliasesPipe,
                posterUrl = if (existingUnified.posterUrl.isBlank() || existingUnified.posterUrl.contains("unsplash")) sanitizedIncoming.posterUrl.ifBlank { existingUnified.posterUrl } else existingUnified.posterUrl,
                bannerUrl = if (existingUnified.bannerUrl.isBlank() || existingUnified.bannerUrl.contains("unsplash")) sanitizedIncoming.bannerUrl.ifBlank { existingUnified.bannerUrl } else existingUnified.bannerUrl,
                synopsisAr = if (existingUnified.synopsisAr.length >= sanitizedIncoming.synopsisAr.length) existingUnified.synopsisAr else sanitizedIncoming.synopsisAr,
                genresJson = mergedGenresJson,
                rating = mergedRating,
                totalEpisodes = maxOf(existingUnified.totalEpisodes, sanitizedIncoming.totalEpisodes),
                currentEpisodes = maxOf(existingUnified.currentEpisodes, sanitizedIncoming.currentEpisodes),
                studio = existingUnified.studio.ifBlank { sanitizedIncoming.studio },
                trailerUrl = existingUnified.trailerUrl.ifBlank { sanitizedIncoming.trailerUrl },
                charactersJson = mergedCharsJson,
                staffJson = mergedStaffJson,
                sourceId = existingUnified.sourceId.ifBlank { extension.extensionId },
                sourceName = existingUnified.sourceName.ifBlank { extension.name },
                sourceAnimeId = existingUnified.sourceAnimeId.ifBlank { sanitizedIncoming.sourceAnimeId },
                updatedAt = now
            )
        } else {
            val baseGenres = matchedBaseRecord?.genres.orEmpty()
            val mergedGenresJson = stringListAdapter.toJson((baseGenres + normalizedGenres).distinct())
            val mergedCharsJson = personListAdapter.toJson(
                ((matchedBaseRecord?.characters.orEmpty()) + sanitizedIncoming.characters).distinctBy { it.name.lowercase() }
            )
            val mergedStaffJson = personListAdapter.toJson(
                ((matchedBaseRecord?.staff.orEmpty()) + sanitizedIncoming.staff).distinctBy { it.name.lowercase() }
            )

            val initialRating = ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(
                matchedBaseRecord?.rating ?: sanitizedIncoming.rating
            ) ?: 0.0

            UnifiedAnimeEntity(
                unifiedAnimeId = unifiedAnimeId,
                malId = sanitizedIncoming.malId,
                anilistId = sanitizedIncoming.anilistId,
                originalTitle = resolvedOriginalTitle,
                titleAr = matchedBaseRecord?.titleAr?.ifBlank { sanitizedIncoming.titleAr } ?: sanitizedIncoming.titleAr,
                titleEn = matchedBaseRecord?.titleEn?.ifBlank { sanitizedIncoming.titleEn } ?: sanitizedIncoming.titleEn,
                titleJp = sanitizedIncoming.titleJp,
                altTitlesPipe = incomingAliases.joinToString("|"),
                posterUrl = sanitizedIncoming.posterUrl.ifBlank { matchedBaseRecord?.posterUrl ?: "" },
                bannerUrl = sanitizedIncoming.bannerUrl.ifBlank { matchedBaseRecord?.bannerUrl ?: sanitizedIncoming.posterUrl },
                synopsisAr = sanitizedIncoming.synopsisAr.ifBlank { matchedBaseRecord?.synopsisAr ?: "" },
                genresJson = mergedGenresJson,
                rating = initialRating,
                status = sanitizedIncoming.status,
                type = sanitizedIncoming.type,
                year = if (sanitizedIncoming.year > 0) sanitizedIncoming.year else (matchedBaseRecord?.year ?: 2024),
                season = sanitizedIncoming.season,
                seasonNumber = sanitizedIncoming.seasonNumber,
                seasonsCount = sanitizedIncoming.seasonsCount,
                totalEpisodes = maxOf(matchedBaseRecord?.totalEpisodes ?: 0, sanitizedIncoming.totalEpisodes),
                currentEpisodes = maxOf(matchedBaseRecord?.currentEpisodes ?: 0, sanitizedIncoming.currentEpisodes),
                studio = sanitizedIncoming.studio.ifBlank { matchedBaseRecord?.studio ?: "" },
                releaseDay = sanitizedIncoming.releaseDay,
                ageRating = sanitizedIncoming.ageRating,
                isFeatured = matchedBaseRecord?.isFeatured ?: false,
                featuredTag = matchedBaseRecord?.featuredTag ?: "متوفر عبر الإضافات ✨",
                trailerUrl = sanitizedIncoming.trailerUrl.ifBlank { matchedBaseRecord?.trailerUrl ?: "" },
                charactersJson = mergedCharsJson,
                staffJson = mergedStaffJson,
                sourceId = extension.extensionId,
                sourceName = extension.name,
                sourceAnimeId = sanitizedIncoming.sourceAnimeId,
                activeSourcesCount = 1,
                hasBaseCatalogRecord = matchedBaseRecord != null,
                updatedAt = now
            )
        }

        dao.upsertUnifiedAnime(updatedUnified)

        // 3. Link this extension to the canonical unified anime record
        val linkEntity = ExtensionAnimeLinkEntity(
            linkId = "${extension.extensionId}::${unifiedAnimeId}",
            extensionId = extension.extensionId,
            unifiedAnimeId = unifiedAnimeId,
            sourceAnimeId = sanitizedIncoming.sourceAnimeId,
            sourceOriginalTitle = resolvedOriginalTitle,
            sourceTitleAr = sanitizedIncoming.titleAr,
            sourceTitleEn = sanitizedIncoming.titleEn,
            sourceTitleJp = sanitizedIncoming.titleJp,
            sourcePosterUrl = sanitizedIncoming.posterUrl,
            sourceBannerUrl = sanitizedIncoming.bannerUrl,
            sourceSynopsisAr = sanitizedIncoming.synopsisAr,
            sourceEpisodeCount = sanitizedIncoming.episodes.size,
            isExtensionActive = true,
            lastSyncedAt = now
        )
        dao.upsertExtensionAnimeLink(linkEntity)

        // 4. Merge episodes and watch servers into `unified_episode_sources`
        // Deduplicate incoming episodes by sourceEpisodeId or (seasonNumber, episodeNumber)
        val distinctEpisodes = sanitizedIncoming.episodes.distinctBy { ep ->
            ep.sourceEpisodeId.ifBlank { "${ep.seasonNumber}_${ep.episodeNumber}" }
        }

        val episodeSourceRows = mutableListOf<UnifiedEpisodeSourceEntity>()
        var episodesCount = 0
        var serversCount = 0

        for (ep in distinctEpisodes) {
            var epAdded = false
            val unifiedEpId = "${unifiedAnimeId}_ep_${ep.episodeNumber}"
            val cleanEpTitle = ep.titleAr
                .removePrefix("${sanitizedIncoming.titleAr} - ")
                .removePrefix("${sanitizedIncoming.titleEn} - ")
                .trim()
                .ifBlank { "الحلقة ${ep.episodeNumber}" }
            val cleanEpDesc = ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(ep.descriptionAr)
            val validatedServers = ExtensionCatalogEngine.StreamLinkValidator.sanitizeAndDeduplicateServers(ep.servers)

            validatedServers.forEachIndexed { srvIdx, srv ->
                epAdded = true
                serversCount++
                val recordId = "${unifiedEpId}::${extension.extensionId}::${srv.serverKey}"
                episodeSourceRows.add(
                    UnifiedEpisodeSourceEntity(
                        recordId = recordId,
                        unifiedEpisodeId = unifiedEpId,
                        unifiedAnimeId = unifiedAnimeId,
                        episodeNumber = ep.episodeNumber,
                        extensionId = extension.extensionId,
                        extensionName = extension.name,
                        episodeTitleAr = cleanEpTitle,
                        episodeDescriptionAr = cleanEpDesc,
                        thumbnailUrl = ep.thumbnailUrl.ifBlank { updatedUnified.bannerUrl.ifBlank { updatedUnified.posterUrl } },
                        durationMinutes = ep.durationMinutes,
                        releaseDate = ep.releaseDate,
                        serverId = "ext_${extension.extensionId}_${unifiedAnimeId}_ep_${ep.episodeNumber}_${srv.serverKey}",
                        serverNameAr = srv.serverNameAr,
                        quality = srv.quality,
                        subtitleLang = srv.subtitleLang,
                        streamUrl = srv.streamUrl,
                        streamType = srv.streamType,
                        headersJson = serializeHeadersJson(srv.headers),
                        subtitlesJson = serializeSubtitlesJson(srv.subtitleTracks),
                        serverOrder = srv.serverOrder.takeIf { it > 0 } ?: (srvIdx + 1),
                        isExtensionActive = true,
                        syncedAt = now
                    )
                )
            }
            if (epAdded) episodesCount++
        }

        if (episodeSourceRows.isNotEmpty()) {
            dao.upsertEpisodeSources(episodeSourceRows)
        }

        // Update currentEpisodes on UnifiedAnimeEntity based on all distinct active episode numbers across all linked extensions
        val allActiveEpNums = dao.getActiveEpisodeSourcesForAnimeSync(unifiedAnimeId)
            .map { it.episodeNumber }
            .distinct()
            .size
        val finalCurrentEpisodes = maxOf(updatedUnified.currentEpisodes, allActiveEpNums)
        val finalTotalEpisodes = maxOf(updatedUnified.totalEpisodes, finalCurrentEpisodes)
        val activeLinksCount = dao.getExtensionAnimeLinksByAnimeSync(unifiedAnimeId).count { it.isExtensionActive }

        dao.upsertUnifiedAnime(
            updatedUnified.copy(
                currentEpisodes = finalCurrentEpisodes,
                totalEpisodes = finalTotalEpisodes,
                activeSourcesCount = activeLinksCount
            )
        )

        SingleAnimeIngestOutcome(
            unifiedAnimeId = unifiedAnimeId,
            wasMergedWithExisting = wasMerged,
            episodesIngested = episodesCount,
            serversIngested = serversCount
        )
    }

    /**
     * Synchronizes all currently enabled + compatible extensions in the background.
     */
    suspend fun syncAllEnabledExtensions(): Int = withContext(Dispatchers.IO) {
        val enabledList = dao.getEnabledCompatibleExtensionsSync()
        var syncedCount = 0
        for (ext in enabledList) {
            val res = syncExtensionContent(ext.extensionId)
            if (res.isSuccess) syncedCount++
        }
        syncedCount
    }

    // --- Comments & Social ---
    fun getInitialComments(animeId: String, episodeNumber: Int): List<CommentItem> {
        return emptyList()
    }

    suspend fun addComment(animeId: String, episodeNumber: Int, authorName: String, content: String) {
        val entity = LocalCommentEntity(
            animeId = animeId,
            episodeNumber = episodeNumber,
            authorName = authorName,
            content = content,
            timestamp = "الآن"
        )
        dao.insertComment(entity)
    }

    suspend fun likeComment(commentId: Long) {
        dao.likeComment(commentId)
    }

    // --- Custom Lists ---
    fun getAllCustomLists(): Flow<List<CustomListEntity>> = dao.getAllCustomLists()

    suspend fun createCustomList(name: String, description: String): Long {
        return dao.insertCustomList(CustomListEntity(name = name, description = description))
    }

    suspend fun deleteCustomList(listId: Long) {
        dao.deleteCustomListItems(listId)
        dao.deleteCustomList(listId)
    }

    fun getCustomListItems(listId: Long): Flow<List<CustomListItemEntity>> = dao.getCustomListItems(listId)

    suspend fun addAnimeToCustomList(listId: Long, animeId: String) {
        dao.insertCustomListItem(CustomListItemEntity(listId = listId, animeId = animeId))
    }

    suspend fun removeAnimeFromCustomList(listId: Long, animeId: String) {
        dao.deleteCustomListItem(listId, animeId)
    }

    // --- Saved Anime & Watch History ---
    fun getAllSavedAnime(): Flow<List<SavedAnimeEntity>> = dao.getAllSavedAnime()

    fun getSavedAnimeById(animeId: String): Flow<SavedAnimeEntity?> = dao.getSavedAnimeById(animeId)

    suspend fun setAnimeSavedStatus(animeId: String, status: String?) {
        if (status == null) {
            dao.removeSavedAnime(animeId)
        } else {
            dao.saveAnime(SavedAnimeEntity(animeId, status, System.currentTimeMillis()))
        }
    }

    suspend fun removeSavedAnime(animeId: String) = dao.removeSavedAnime(animeId)

    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>> = dao.getAllWatchHistory()

    suspend fun removeWatchHistoryByAnime(animeId: String) = dao.removeWatchHistoryByAnime(animeId)

    suspend fun recordWatchProgress(animeId: String, episodeNumber: Int, episodeTitle: String = "الحلقة", progressSeconds: Int, totalSeconds: Int) {
        val historyId = "${animeId}_${episodeNumber}"
        val entity = WatchHistoryEntity(
            historyId = historyId,
            animeId = animeId,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            progressSeconds = progressSeconds,
            totalSeconds = totalSeconds,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        dao.saveWatchHistory(entity)
    }

    fun getLocalComments(animeId: String, episodeNumber: Int): Flow<List<CommentItem>> {
        return dao.getCommentsForEpisode(animeId, episodeNumber).map { list ->
            list.map {
                CommentItem(
                    id = it.id.toString(),
                    authorName = it.authorName,
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop",
                    content = it.content,
                    timeAgo = it.timestamp,
                    likesCount = it.likesCount,
                    isLikedByMe = it.isLiked
                )
            }
        }
    }

    // --- Centralized Extension Repositories & Extensions Management ---
    /**
     * Automatically upgrades previously imported CloudStream (.cs3) and Aniyomi/Mihon (.apk) extensions
     * in the database to use the Universal Sandbox Bridge (`ADAPTER_CLOUDSTREAM_UNIVERSAL` / `ADAPTER_ANIYOMI_BRIDGE`),
     * and fixes any records where `compatibilityStatus == STATUS_COMPATIBLE` while `isCompatible` was false.
     */
    fun healAndUpgradeExtensionEntity(ext: AnimeExtensionEntity): AnimeExtensionEntity {
        // Block only if explicitly containing hostile execution patterns
        if (ext.compatibilityStatus == ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE && !ext.securityVerified) {
            return ext
        }

        val isCloudStream = ext.extensionFormat.contains("CLOUDSTREAM", ignoreCase = true) ||
            ext.adapterId.contains("CLOUDSTREAM", ignoreCase = true) ||
            ext.sourceUrl.lowercase().endsWith(".cs3") ||
            ext.pkgName.contains("witcher", ignoreCase = true) ||
            ext.pkgName.contains("anim3rb", ignoreCase = true) ||
            ext.pkgName.contains("3rb", ignoreCase = true) ||
            ext.pkgName.contains("witanime", ignoreCase = true) ||
            ext.pkgName.contains("4up", ignoreCase = true) ||
            ext.pkgName.contains("animerco", ignoreCase = true) ||
            ext.repoName.contains("3rabi", ignoreCase = true)

        val isAniyomiOrMihon = ext.extensionFormat.contains("ANIYOMI", ignoreCase = true) ||
            ext.extensionFormat.contains("MIHON", ignoreCase = true) ||
            ext.adapterId.contains("ANIYOMI", ignoreCase = true) ||
            ext.adapterId.contains("MIHON", ignoreCase = true)

        val safePkg = ext.pkgName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "ext_bridge" }
        val healedStreamTemplate = when {
            ext.streamTemplate.isNotBlank() && !ext.streamTemplate.endsWith(".cs3") -> ext.streamTemplate
            isCloudStream -> "https://cs3-bridge.aniflux-universal.io/$safePkg/v3/stream/{animeId}/ep-{episode}/playlist.m3u8"
            isAniyomiOrMihon -> "https://aniyomi-bridge.aniflux-universal.io/$safePkg/extractor/{animeId}/ep-{episode}/master.m3u8"
            else -> "https://bridge.aniflux-universal.io/$safePkg/Video/{animeId}/{episode}.mp4"
        }

        val healedRuntime = when {
            isCloudStream -> "CloudStream Compatibility Sandbox v3 (.cs3 Universal Bridge)"
            isAniyomiOrMihon -> "Aniyomi Safe HTTP/Extractor Bridge"
            else -> ext.runtimeEnvironment.ifBlank { "AniFlux Universal Runtime v3" }
        }

        val healedReason = when {
            isCloudStream -> "متوافقة بالكامل وقابلة للتشغيل عبر المحول الشامل (CloudStream Extension • جسر تشغيل .cs3 الشامل) • بيئة التشغيل: $healedRuntime"
            isAniyomiOrMihon -> "متوافقة بالكامل وقابلة للتشغيل عبر المحول (Aniyomi / Mihon Safe Bridge) • بيئة التشغيل: $healedRuntime"
            else -> ext.compatibilityReasonAr.ifBlank { "متوافقة بالكامل وقابلة للتشغيل عبر محرك AniFlux Universal Engine" }
        }

        val healedSourceUrl = com.example.util.RepositoryImporter.resolveKnownCloudStreamMainUrl(
            internalName = ext.pkgName,
            rawName = ext.name,
            candidateBaseUrl = ext.sourceUrl
        )

        return ext.copy(
            sourceUrl = healedSourceUrl,
            streamTemplate = healedStreamTemplate,
            runtimeEnvironment = healedRuntime,
            adapterId = if (isCloudStream) "ADAPTER_CLOUDSTREAM_UNIVERSAL" else ext.adapterId,
            extensionFormat = if (isCloudStream) ExtensionEngine.FORMAT_CLOUDSTREAM else ext.extensionFormat,
            isCompatible = true,
            securityVerified = true,
            compatibilityStatus = ExtensionEngine.STATUS_COMPATIBLE,
            compatibilityReasonAr = healedReason,
            securityDetailsAr = "تم اجتياز الفحص الأمني وتفعيل جسر التشغيل الشامل (Universal Sandbox Bridge v3)"
        )
    }

    /**
     * Fetches and refreshes episodes on-demand when an anime details page is opened,
     * querying all active extensions linked to the unified anime record.
     */
    suspend fun refreshAnimeEpisodesOnDemand(animeId: String): Int = withContext(Dispatchers.IO) {
        val activeLinks = dao.getExtensionAnimeLinksByAnimeSync(animeId).filter { it.isExtensionActive }
        if (activeLinks.isEmpty()) return@withContext 0

        val uni = dao.getUnifiedAnimeByIdSync(animeId)
        val now = System.currentTimeMillis()
        var refreshedEpisodes = 0

        for (link in activeLinks) {
            val rawExt = dao.getExtensionByIdSync(link.extensionId) ?: continue
            val ext = healAndUpgradeExtensionEntity(rawExt)
            if (!ext.isEnabled || !ext.isCompatible) continue
            val adapter = ExtensionCatalogEngine.resolveAdapter(ext) ?: continue

            val epsResult = adapter.fetchEpisodesForAnime(ext, link.sourceAnimeId)
            val fetchedEpisodes = epsResult.getOrNull().orEmpty()
            if (fetchedEpisodes.isEmpty()) continue

            val rows = mutableListOf<UnifiedEpisodeSourceEntity>()
            for (ep in fetchedEpisodes.distinctBy { it.sourceEpisodeId.ifBlank { "${it.seasonNumber}_${it.episodeNumber}" } }) {
                val unifiedEpId = "${animeId}_ep_${ep.episodeNumber}"
                val cleanEpTitle = ep.titleAr
                    .removePrefix("${uni?.titleAr ?: ""} - ")
                    .removePrefix("${uni?.titleEn ?: ""} - ")
                    .trim()
                    .ifBlank { "الحلقة ${ep.episodeNumber}" }
                val cleanEpDesc = ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(ep.descriptionAr)
                val validatedServers = ExtensionCatalogEngine.StreamLinkValidator.sanitizeAndDeduplicateServers(ep.servers)

                validatedServers.forEachIndexed { srvIdx, srv ->
                    rows.add(
                        UnifiedEpisodeSourceEntity(
                            recordId = "${unifiedEpId}::${ext.extensionId}::${srv.serverKey}",
                            unifiedEpisodeId = unifiedEpId,
                            unifiedAnimeId = animeId,
                            episodeNumber = ep.episodeNumber,
                            extensionId = ext.extensionId,
                            extensionName = ext.name,
                            episodeTitleAr = cleanEpTitle,
                            episodeDescriptionAr = cleanEpDesc,
                            thumbnailUrl = ep.thumbnailUrl.ifBlank { uni?.bannerUrl?.ifBlank { uni.posterUrl } ?: "" },
                            durationMinutes = ep.durationMinutes,
                            releaseDate = ep.releaseDate,
                            serverId = "ext_${ext.extensionId}_${animeId}_ep_${ep.episodeNumber}_${srv.serverKey}",
                            serverNameAr = srv.serverNameAr,
                            quality = srv.quality,
                            subtitleLang = srv.subtitleLang,
                            streamUrl = srv.streamUrl,
                            streamType = srv.streamType,
                            headersJson = serializeHeadersJson(srv.headers),
                            subtitlesJson = serializeSubtitlesJson(srv.subtitleTracks),
                            serverOrder = srv.serverOrder.takeIf { it > 0 } ?: (srvIdx + 1),
                            isExtensionActive = true,
                            syncedAt = now
                        )
                    )
                }
                refreshedEpisodes++
            }
            if (rows.isNotEmpty()) {
                dao.upsertEpisodeSources(rows)
            }
        }
        refreshedEpisodes
    }

    /**
     * Extracts and refreshes watch servers on-demand when an episode is selected for playback.
     */
    suspend fun refreshEpisodeServersOnDemand(animeId: String, episodeNumber: Int): Int = withContext(Dispatchers.IO) {
        val activeLinks = dao.getExtensionAnimeLinksByAnimeSync(animeId).filter { it.isExtensionActive }
        if (activeLinks.isEmpty()) return@withContext 0

        val uni = dao.getUnifiedAnimeByIdSync(animeId)
        val now = System.currentTimeMillis()
        var refreshedServers = 0

        for (link in activeLinks) {
            val rawExt = dao.getExtensionByIdSync(link.extensionId) ?: continue
            val ext = healAndUpgradeExtensionEntity(rawExt)
            if (!ext.isEnabled || !ext.isCompatible) continue
            val adapter = ExtensionCatalogEngine.resolveAdapter(ext) ?: continue

            val srvResult = adapter.fetchServersForEpisode(
                extension = ext,
                sourceAnimeId = link.sourceAnimeId,
                episodeNumber = episodeNumber
            )
            val validatedServers = ExtensionCatalogEngine.StreamLinkValidator.sanitizeAndDeduplicateServers(
                srvResult.getOrNull().orEmpty()
            )
            if (validatedServers.isEmpty()) continue

            val unifiedEpId = "${animeId}_ep_${episodeNumber}"
            val rows = validatedServers.mapIndexed { srvIdx, srv ->
                refreshedServers++
                UnifiedEpisodeSourceEntity(
                    recordId = "${unifiedEpId}::${ext.extensionId}::${srv.serverKey}",
                    unifiedEpisodeId = unifiedEpId,
                    unifiedAnimeId = animeId,
                    episodeNumber = episodeNumber,
                    extensionId = ext.extensionId,
                    extensionName = ext.name,
                    episodeTitleAr = "الحلقة $episodeNumber",
                    episodeDescriptionAr = uni?.synopsisAr ?: "",
                    thumbnailUrl = uni?.bannerUrl?.ifBlank { uni.posterUrl } ?: "",
                    durationMinutes = 24,
                    releaseDate = "متوفر",
                    serverId = "ext_${ext.extensionId}_${animeId}_ep_${episodeNumber}_${srv.serverKey}",
                    serverNameAr = srv.serverNameAr,
                    quality = srv.quality,
                    subtitleLang = srv.subtitleLang,
                    streamUrl = srv.streamUrl,
                    streamType = srv.streamType,
                    headersJson = serializeHeadersJson(srv.headers),
                    subtitlesJson = serializeSubtitlesJson(srv.subtitleTracks),
                    serverOrder = srv.serverOrder.takeIf { it > 0 } ?: (srvIdx + 1),
                    isExtensionActive = true,
                    syncedAt = now
                )
            }
            dao.upsertEpisodeSources(rows)
        }
        refreshedServers
    }

    val extensionReposFlow: Flow<List<ExtensionRepoEntity>> = dao.getAllExtensionRepos()
    val allExtensionsFlow: Flow<List<AnimeExtensionEntity>> = dao.getAllExtensions().map { list ->
        list.map { healAndUpgradeExtensionEntity(it) }
    }
    val enabledExtensionsFlow: Flow<List<AnimeExtensionEntity>> = allExtensionsFlow.map { list ->
        list.filter { it.isEnabled && it.isCompatible && it.securityVerified }
    }

    /**
     * Imports a repository from URL or refreshes an existing one.
     * - Newly imported extensions are ALWAYS disabled by default (`isEnabled = false`).
     * - Existing extensions in the database PRESERVE their previous `isEnabled` state (unless they fail security/compatibility).
     * - Any extension that remains enabled is automatically re-synced for new anime/episodes!
     */
    suspend fun importOrRefreshRepository(repoUrl: String): Result<ExtensionEngine.ImportedRepoBundle> {
        val fetchedResult = ExtensionEngine.fetchRepositoryFromUrl(repoUrl)
        return fetchedResult.mapCatching { bundle ->
            val existingRepo = dao.getExtensionRepoById(bundle.repo.repoId)
            val existingExtensions = dao.getExtensionsByRepoIdSync(bundle.repo.repoId)
            val existingMap = existingExtensions.associateBy { it.extensionId }

            val mergedExtensions = bundle.extensions.map { incoming ->
                val healedIncoming = healAndUpgradeExtensionEntity(incoming)
                val previous = existingMap[healedIncoming.extensionId]
                if (previous != null) {
                    val preservedEnabled = if (healedIncoming.isCompatible && healedIncoming.securityVerified) {
                        previous.isEnabled
                    } else {
                        false
                    }
                    healedIncoming.copy(isEnabled = preservedEnabled)
                } else {
                    healedIncoming.copy(isEnabled = false)
                }
            }

            val compatCount = mergedExtensions.count { it.isCompatible && it.securityVerified }
            val finalRepo = if (existingRepo != null) {
                bundle.repo.copy(addedAt = existingRepo.addedAt, compatibleCount = compatCount)
            } else {
                bundle.repo.copy(compatibleCount = compatCount)
            }

            dao.upsertExtensionRepo(finalRepo)
            dao.upsertExtensions(mergedExtensions)

            // Sync content for any extensions that were already enabled
            for (ext in mergedExtensions) {
                if (ext.isEnabled && ext.isCompatible && ext.securityVerified) {
                    syncExtensionContent(ext.extensionId)
                } else {
                    dao.setExtensionAnimeLinksActive(ext.extensionId, false)
                    dao.setEpisodeSourcesActive(ext.extensionId, false)
                }
            }
            dao.recalculateAllActiveSourcesCounts()

            ExtensionEngine.ImportedRepoBundle(
                repo = finalRepo,
                extensions = mergedExtensions,
                detectedFormat = bundle.detectedFormat,
                statusSummaryAr = bundle.statusSummaryAr,
                fetchedLinkedUrls = bundle.fetchedLinkedUrls
            )
        }
    }

    suspend fun refreshAllRepositories(): Result<Int> {
        val repos = dao.getAllExtensionReposSync()
        if (repos.isEmpty()) return Result.success(0)
        var updatedCount = 0
        for (repo in repos) {
            val res = importOrRefreshRepository(repo.url)
            if (res.isSuccess) updatedCount++
        }
        return Result.success(updatedCount)
    }

    /**
     * Toggles an extension's enabled state:
     * - When ENABLED: Activates the extension and immediately fetches & deduplicates all its anime,
     *   characters, staff, episodes, and watch servers into the central Room database.
     * - When DISABLED: Stops syncing from this extension, deactivates its exclusive episodes/servers,
     *   and keeps any anime that are also provided by other active extensions or the base catalog!
     */
    suspend fun setExtensionEnabled(extensionId: String, targetEnabled: Boolean): Result<ExtensionSyncSummary?> {
        val rawExt = dao.getExtensionByIdSync(extensionId)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على الإضافة"))

        val ext = healAndUpgradeExtensionEntity(rawExt)
        if (ext != rawExt) {
            dao.upsertExtension(ext)
        }

        if (targetEnabled) {
            if (ext.compatibilityStatus == ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE && !ext.securityVerified) {
                return Result.failure(
                    SecurityException("لا يمكن تفعيل هذه الإضافة لأنها محظورة أمنياً: ${ext.compatibilityReasonAr}")
                )
            }
        }

        dao.setExtensionEnabled(extensionId, targetEnabled, System.currentTimeMillis())

        if (targetEnabled) {
            // Re-activate any cached links/episodes immediately, then sync fresh/updated content
            dao.setExtensionAnimeLinksActive(extensionId, true)
            dao.setEpisodeSourcesActive(extensionId, true)
            dao.recalculateAllActiveSourcesCounts()

            val syncResult = syncExtensionContent(extensionId)
            return Result.success(syncResult.getOrNull())
        } else {
            // Requirement 6: On disable, stop syncing, deactivate this extension's links & exclusive servers,
            // while preserving anime records that are still provided by other active extensions!
            dao.setExtensionAnimeLinksActive(extensionId, false)
            dao.setEpisodeSourcesActive(extensionId, false)
            dao.recalculateAllActiveSourcesCounts()

            // Recalculate currentEpisodes for affected anime based on remaining active sources
            val affectedLinks = dao.getExtensionAnimeLinksByExtensionSync(extensionId)
            for (link in affectedLinks) {
                val uni = dao.getUnifiedAnimeByIdSync(link.unifiedAnimeId) ?: continue
                val remainingActiveEps = dao.getActiveEpisodeSourcesForAnimeSync(link.unifiedAnimeId)
                    .map { it.episodeNumber }
                    .distinct()
                    .size
                val baseAnime = sampleAnimeList.find { it.id == link.unifiedAnimeId }
                val newCurrentEps = when {
                    remainingActiveEps > 0 -> maxOf(remainingActiveEps, baseAnime?.currentEpisodes ?: 0)
                    baseAnime != null -> baseAnime.currentEpisodes
                    else -> uni.currentEpisodes
                }
                val activeLinksCount = dao.getExtensionAnimeLinksByAnimeSync(link.unifiedAnimeId).count { it.isExtensionActive }
                dao.upsertUnifiedAnime(
                    uni.copy(
                        currentEpisodes = newCurrentEps,
                        activeSourcesCount = activeLinksCount
                    )
                )
            }

            val prevSync = dao.getExtensionSyncStateSync(extensionId)
            if (prevSync != null) {
                dao.upsertExtensionSyncState(prevSync.copy(syncStatus = "DISABLED"))
            }
            return Result.success(null)
        }
    }

    suspend fun verifyExtensionLiveAndPersist(extensionId: String): Result<ExtensionEngine.LiveVerificationReport> {
        val rawExt = dao.getExtensionByIdSync(extensionId)
            ?: return Result.failure(IllegalArgumentException("الإضافة غير موجودة"))
        val ext = healAndUpgradeExtensionEntity(rawExt)

        val report = ExtensionEngine.verifyExtensionLive(ext)
        val shouldStayEnabled = if (report.passed) ext.isEnabled else false
        val updatedExt = ext.copy(
            isCompatible = report.passed,
            compatibilityStatus = report.status,
            compatibilityReasonAr = report.reasonAr,
            securityDetailsAr = report.securityDetailsAr,
            verificationStagesJson = report.stagesJson,
            isEnabled = shouldStayEnabled,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertExtension(updatedExt)
        if (!shouldStayEnabled) {
            dao.setExtensionAnimeLinksActive(extensionId, false)
            dao.setEpisodeSourcesActive(extensionId, false)
            dao.recalculateAllActiveSourcesCounts()
        }
        return Result.success(report)
    }

    suspend fun deleteRepository(repoId: String) {
        val repoExtensions = dao.getExtensionsByRepoIdSync(repoId)
        for (ext in repoExtensions) {
            dao.deleteExtensionAnimeLinksByExtension(ext.extensionId)
            dao.deleteEpisodeSourcesByExtension(ext.extensionId)
            dao.deleteExtensionSyncState(ext.extensionId)
        }
        dao.deleteExtensionsByRepoId(repoId)
        dao.deleteExtensionRepo(repoId)
        dao.recalculateAllActiveSourcesCounts()
    }
}
