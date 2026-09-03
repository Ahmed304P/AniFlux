package com.example.data.repository

import com.example.data.local.CustomListEntity
import com.example.data.local.CustomListItemEntity
import com.example.data.local.AniFluxDao
import com.example.data.local.LocalCommentEntity
import com.example.data.local.SavedAnimeEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Anime
import com.example.data.model.CommentItem
import com.example.data.model.Episode
import com.example.data.model.StreamServer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.local.PublishedAnimeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeRepository(private val dao: AniFluxDao) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val animeAdapter = moshi.adapter(Anime::class.java)

    val publishedAnimeFlow: Flow<List<Anime>> = dao.getAllPublishedAnime().map { entities ->
        entities.mapNotNull {
            try {
                animeAdapter.fromJson(it.animeJson)
            } catch(e: Exception) {
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


    // Sample Anime Catalog with realistic Arabic metadata
    val sampleAnimeList = mutableListOf(
        Anime(
            id = "dummy_1",
            titleAr = "انمي وهمي 1",
            titleEn = "Fake Anime 1",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.1,
            totalEpisodes = 24,
            currentEpisodes = 6,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 1",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 1. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 11,
            isFeatured = false
        ),
        Anime(
            id = "dummy_2",
            titleAr = "انمي وهمي 2",
            titleEn = "Fake Anime 2",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.2,
            totalEpisodes = 24,
            currentEpisodes = 7,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 2",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 2. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 12,
            isFeatured = false
        ),
        Anime(
            id = "dummy_3",
            titleAr = "انمي وهمي 3",
            titleEn = "Fake Anime 3",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.3,
            totalEpisodes = 24,
            currentEpisodes = 8,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 3",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 3. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 13,
            isFeatured = false
        ),
        Anime(
            id = "dummy_4",
            titleAr = "انمي وهمي 4",
            titleEn = "Fake Anime 4",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.4,
            totalEpisodes = 24,
            currentEpisodes = 9,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 4",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 4. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 14,
            isFeatured = false
        ),
        Anime(
            id = "dummy_5",
            titleAr = "انمي وهمي 5",
            titleEn = "Fake Anime 5",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.5,
            totalEpisodes = 24,
            currentEpisodes = 10,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 5",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 5. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 15,
            isFeatured = false
        ),
        Anime(
            id = "dummy_6",
            titleAr = "انمي وهمي 6",
            titleEn = "Fake Anime 6",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.6,
            totalEpisodes = 24,
            currentEpisodes = 11,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 6",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 6. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 16,
            isFeatured = false
        ),
        Anime(
            id = "dummy_7",
            titleAr = "انمي وهمي 7",
            titleEn = "Fake Anime 7",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.7,
            totalEpisodes = 24,
            currentEpisodes = 12,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 7",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 7. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 17,
            isFeatured = false
        ),
        Anime(
            id = "dummy_8",
            titleAr = "انمي وهمي 8",
            titleEn = "Fake Anime 8",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.8,
            totalEpisodes = 24,
            currentEpisodes = 13,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 8",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 8. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 18,
            isFeatured = false
        ),
        Anime(
            id = "dummy_9",
            titleAr = "انمي وهمي 9",
            titleEn = "Fake Anime 9",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.9,
            totalEpisodes = 24,
            currentEpisodes = 14,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 9",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 9. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 19,
            isFeatured = false
        ),
        Anime(
            id = "dummy_10",
            titleAr = "انمي وهمي 10",
            titleEn = "Fake Anime 10",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.0,
            totalEpisodes = 24,
            currentEpisodes = 15,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 10",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 10. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 20,
            isFeatured = false
        ),
        Anime(
            id = "dummy_11",
            titleAr = "انمي وهمي 11",
            titleEn = "Fake Anime 11",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.1,
            totalEpisodes = 24,
            currentEpisodes = 16,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 11",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 11. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 21,
            isFeatured = false
        ),
        Anime(
            id = "dummy_12",
            titleAr = "انمي وهمي 12",
            titleEn = "Fake Anime 12",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.2,
            totalEpisodes = 24,
            currentEpisodes = 17,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 12",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 12. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 22,
            isFeatured = false
        ),
        Anime(
            id = "dummy_13",
            titleAr = "انمي وهمي 13",
            titleEn = "Fake Anime 13",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.3,
            totalEpisodes = 24,
            currentEpisodes = 18,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 13",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 13. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 23,
            isFeatured = false
        ),
        Anime(
            id = "dummy_14",
            titleAr = "انمي وهمي 14",
            titleEn = "Fake Anime 14",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.4,
            totalEpisodes = 24,
            currentEpisodes = 19,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 14",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 14. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 24,
            isFeatured = false
        ),
        Anime(
            id = "dummy_15",
            titleAr = "انمي وهمي 15",
            titleEn = "Fake Anime 15",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            rating = 8.5,
            totalEpisodes = 24,
            currentEpisodes = 20,
            status = "مكتمل",
            type = "TV",
            year = 2023,
            studio = "Studio 15",
            synopsisAr = "هذا نص تجريبي لمعلومات الانمي الوهمي رقم 15. يتم استخدامه فقط كعنصر وهمي لتجربة التطبيق.",
            genres = listOf("أكشن", "خيال"),
            releaseDay = "الاثنين",
            trendingRank = 25,
            isFeatured = false
        ),

        Anime(
            id = "solo_leveling",
            titleAr = "سولو ليفلينج: رفع المستوى فردياً",
            titleEn = "Solo Leveling: Arise",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
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
            featuredTag = "الأكثر مشاهدة هذا الموسم 🔥"
        ),
        Anime(
            id = "demon_slayer_s4",
            titleAr = "قاتل الشياطين: تدريب الهاشيرا",
            titleEn = "Demon Slayer: Hashira Training Arc",
            posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1200&auto=format&fit=crop",
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
            featuredTag = "أنمي فائق الجودة ⚡"
        ),
        Anime(
            id = "attack_on_titan_final",
            titleAr = "هجوم العمالقة: الفصل النهائي",
            titleEn = "Attack on Titan: The Final Season",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop",
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
            featuredTag = "أسطوري ⭐"
        ),
        Anime(
            id = "jujutsu_kaisen_s2",
            titleAr = "جوجوتسو كايسن: الموسم الثاني",
            titleEn = "Jujutsu Kaisen Season 2",
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=1200&auto=format&fit=crop",
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
            isFeatured = false
        ),
        Anime(
            id = "frieren_journey",
            titleAr = "فريرين: ما بعد نهاية الرحلة",
            titleEn = "Frieren: Beyond Journey's End",
            posterUrl = "https://images.unsplash.com/photo-1514539079130-25950c84af65?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&auto=format&fit=crop",
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
            featuredTag = "تحفة فنية 🌸"
        ),
        Anime(
            id = "kaiju_no8",
            titleAr = "كايجو رقم 8",
            titleEn = "Kaiju No. 8",
            posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop",
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
            isFeatured = false
        ),
        Anime(
            id = "one_piece_egghead",
            titleAr = "ون بيس: آرك إيغ هيد",
            titleEn = "One Piece: Egghead Arc",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1200&auto=format&fit=crop",
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
            isFeatured = false
        ),
        Anime(
            id = "bleach_tybw",
            titleAr = "بليتش: حرب الدم الألفية",
            titleEn = "Bleach: Thousand-Year Blood War",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
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
            isFeatured = false
        )
    )

    fun getEpisodesForAnime(anime: com.example.data.model.Anime): List<com.example.data.model.Episode> {
        val totalEpisodes = anime.currentEpisodes
        val animeId = anime.id
        
        // Find maximum episode number (either from currentEpisodes or customEpisodes)
        val maxCustomEp = anime.customEpisodes?.keys?.mapNotNull { it.toIntOrNull() }?.maxOrNull() ?: 0
        val maxEp = maxOf(totalEpisodes, maxCustomEp)
        
        return (1..maxEp).map { num ->
            val formattedNumber = num.toString()
            val customUrl = anime.customEpisodes?.get(formattedNumber)
            
            val serversList = if (!customUrl.isNullOrBlank()) {
                val parsedServers = mutableListOf<com.example.data.model.StreamServer>()
                val parts = customUrl.split(",")
                parts.forEachIndexed { index, part ->
                    val trimmedPart = part.trim()
                    if (trimmedPart.isNotBlank()) {
                        val nameAndUrl = trimmedPart.split("|", limit = 2)
                        val name = if (nameAndUrl.size == 2) nameAndUrl[0].trim() else "سيرفر ${index + 1}"
                        val url = if (nameAndUrl.size == 2) nameAndUrl[1].trim() else nameAndUrl[0].trim()
                        parsedServers.add(
                            com.example.data.model.StreamServer(
                                id = "${animeId}_${num}_custom_$index",
                                nameAr = name,
                                quality = "متعدد",
                                streamUrl = url
                            )
                        )
                    }
                }
                if (parsedServers.isEmpty()) {
                    listOf(
                        com.example.data.model.StreamServer(
                            id = "${animeId}_${num}_custom",
                            nameAr = "سيرفر رئيسي",
                            quality = "متعدد",
                            streamUrl = customUrl
                        )
                    )
                } else parsedServers
            } else if (anime.serverId != null && anime.serverId.isNotBlank()) {
                listOf(
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_720",
                        nameAr = "سيرفر رئيسي 720p",
                        quality = "720p",
                        streamUrl = "https://server.sanime.net/Video/${anime.serverId}/${formattedNumber}.mp4"
                    ),
                    com.example.data.model.StreamServer(
                        id = "${animeId}_${num}_480",
                        nameAr = "سيرفر اقتصادي 480p",
                        quality = "480p",
                        streamUrl = "https://server.sanime.net/Video/${anime.serverId}/${formattedNumber}SD.mp4"
                    )
                )
            } else {
                emptyList()
            }
            
            com.example.data.model.Episode(
                id = "${animeId}_ep_${num}",
                animeId = animeId,
                episodeNumber = num,
                titleAr = "الحلقة $num",
                durationMinutes = 24,
                thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop",
                releaseDate = "2023-01-01",
                servers = serversList
            )
        }.filter { it.servers.isNotEmpty() }
    }
    
    fun getInitialComments(animeId: String, episodeNumber: Int): List<com.example.data.model.CommentItem> {
        return emptyList()
    }

    suspend fun addComment(animeId: String, episodeNumber: Int, authorName: String, content: String) {
        val entity = com.example.data.local.LocalCommentEntity(
            animeId = animeId,
            episodeNumber = episodeNumber,
            authorName = authorName,
            content = content,
            timestamp = "الآن"
        )
        dao.insertComment(entity)
    }

    suspend fun likeComment(commentId: Long) {
        // Implementation
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

    // --- Restored Missing Methods ---
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
    
    fun getLocalComments(animeId: String, episodeNumber: Int): Flow<List<com.example.data.model.CommentItem>> {
        return dao.getCommentsForEpisode(animeId, episodeNumber).map { list ->
            list.map {
                com.example.data.model.CommentItem(
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
}
