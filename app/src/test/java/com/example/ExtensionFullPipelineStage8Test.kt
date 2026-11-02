package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AniFluxDatabase
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import com.example.util.ExtensionCatalogEngine
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExtensionFullPipelineStage8Test {

    private lateinit var db: AniFluxDatabase
    private lateinit var repository: AnimeRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AniFluxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnimeRepository(db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // 1. اختبارات جلب المكتبة الكاملة والتصفح (Full Library & Pagination Tests)
    // =========================================================================
    @Test
    fun `stage 1 - full library pagination fetches over 70 unique anime across all pages without stopping at 60`() = runBlocking {
        val dao = db.dao()

        // Import 3rabi CloudStream repository (Anime4up, Anime3rb, Witanime, AnimeWitcher, Animerco, etc.)
        val repo3rabiUrl = "https://raw.githubusercontent.com/Abodabodd/re-3arabi/refs/heads/main/repo"
        val importRes = repository.importOrRefreshRepository(repo3rabiUrl)
        assertTrue("3rabi repository should import successfully", importRes.isSuccess)

        val extensions = dao.getAllExtensions().first()
        assertTrue("Expected at least 5 extensions in 3rabi repo", extensions.size >= 5)

        val anime4upExt = extensions.first { it.name.contains("Anime4up", ignoreCase = true) }
        val syncResult = repository.setExtensionEnabled(anime4upExt.extensionId, true)
        assertTrue("Anime4up sync must succeed", syncResult.isSuccess)

        val summary = syncResult.getOrNull()!!
        // Verify pagination fetched > 70 unique anime works across multiple pages (75 works across 7 pages)
        assertTrue(
            "Expected > 70 anime works fetched across all pages, got ${summary.fetchedAnimeCount}",
            summary.fetchedAnimeCount >= 74
        )
        assertTrue(
            "Expected at least 7 pages traversed, got ${summary.syncedPagesCount}",
            summary.syncedPagesCount >= 7
        )

        val syncState = dao.getExtensionSyncStateSync(anime4upExt.extensionId)
        assertNotNull(syncState)
        assertEquals("SUCCESS", syncState!!.syncStatus)
        assertEquals(summary.fetchedAnimeCount, syncState.syncedAnimeCount)
        assertTrue(syncState.syncedPagesCount >= 7)

        // Verify PAGE, CURSOR, OFFSET_LIMIT, and NEXT_PAGE_URL pagination requests
        val adapter = ExtensionCatalogEngine.resolveAdapter(anime4upExt)!!
        val page1 = adapter.fetchCatalogPage(
            anime4upExt,
            ExtensionCatalogEngine.PaginationRequest(page = 1, pageSize = 12, section = "ongoing")
        ).getOrThrow()
        val page2 = adapter.fetchCatalogPage(
            anime4upExt,
            ExtensionCatalogEngine.PaginationRequest(
                page = 2,
                pageSize = 12,
                cursor = page1.nextCursor,
                offset = page1.nextOffset,
                nextPageUrl = page1.nextPageUrl,
                section = "ongoing"
            )
        ).getOrThrow()

        assertEquals(12, page1.items.size)
        assertEquals(12, page2.items.size)
        assertTrue("Page 1 and Page 2 signatures must be distinct", page1.pageSignature != page2.pageSignature)
        assertTrue("Page 1 nextPageUrl must be populated", page1.nextPageUrl.contains("page=2"))
        assertEquals(12, page1.nextOffset)
    }

    // =========================================================================
    // 2. اختبارات تطبيع البيانات (Data Normalization Tests)
    // =========================================================================
    @Test
    fun `stage 2 - data normalizer accurately normalizes ratings titles and HTML synopses`() = runBlocking {
        // 2.A Rating Normalization: 10-point, 100-point, 5-point, floating-point precision, and invalid values
        assertEquals(8.9, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(8.9), 0.001)
        assertEquals(9.1, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(8.8 + 0.3), 0.0000001)
        assertEquals("9.1", ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(8.8 + 0.3).toString())
        assertEquals(8.6, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(86.0), 0.001)
        assertEquals(9.15, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(91.5), 0.001)
        assertEquals(9.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(4.5, rawScale = 5), 0.001)
        assertEquals(9.0, ExtensionCatalogEngine.AnimeDataNormalizer.parseAndNormalizeRatingString("4.5 / 5"), 0.001)
        assertEquals(8.7, ExtensionCatalogEngine.AnimeDataNormalizer.parseAndNormalizeRatingString("87%"), 0.001)

        // Reject 0, negative, voter counts (>100), and anime IDs -> returns 0.0
        assertEquals(0.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(0.0), 0.001)
        assertEquals(0.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(-5.0), 0.001)
        assertEquals(0.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(45230.0), 0.001) // Voter count
        assertEquals(0.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(52991.0), 0.001) // MAL ID
        assertEquals(0.0, ExtensionCatalogEngine.AnimeDataNormalizer.normalizeRating(85.0, malId = "85"), 0.001) // Matches ID

        // 2.B Synopsis HTML & Boilerplate Cleanup
        val dirtyHtmlSynopsis = "<p>قصة <b>شيقة</b> عن &quot;مغامرات&quot; في عالم السحر &amp; الخيال.<br/>الفصل الأول يبدأ الآن.</p>"
        val cleanedSynopsis = ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(dirtyHtmlSynopsis)
        assertFalse("HTML tags must be stripped", cleanedSynopsis.contains("<p>") || cleanedSynopsis.contains("<b>"))
        assertFalse("HTML entities must be decoded", cleanedSynopsis.contains("&quot;") || cleanedSynopsis.contains("&amp;"))
        assertTrue(cleanedSynopsis.contains("قصة شيقة عن \"مغامرات\" في عالم السحر & الخيال."))

        val fakeGenericBoilerplate = "عمل أنمي متكامل متوفر بجودات متعددة عبر مصدر Anime4up"
        assertEquals(
            "Generic fake boilerplate synopsis must be rejected",
            "",
            ExtensionCatalogEngine.AnimeDataNormalizer.cleanSynopsis(fakeGenericBoilerplate)
        )

        // 2.C Title Separation & No Fake Extension Prefixes
        val titles = ExtensionCatalogEngine.AnimeDataNormalizer.normalizeTitles(
            originalTitle = "Kusuriya no Hitorigoto",
            titleEn = "The Apothecary Diaries",
            titleAr = "يوميات الصيدلانية",
            titleJp = "薬屋のひとりごと",
            altTitles = listOf("Kusuriya", "Apothecary Diaries")
        )
        assertEquals("Kusuriya no Hitorigoto", titles.originalTitle)
        assertEquals("The Apothecary Diaries", titles.titleEn)
        assertEquals("يوميات الصيدلانية", titles.titleAr)
        assertEquals("薬屋のひとりごと", titles.titleJp)

        // Verify all synced anime in database have clean ratings (<= 10.0, max 2 decimals) and no "Special:" fake titles
        val repoUrl = com.example.util.ExtensionEngine.presetRepositories.first().url
        repository.importOrRefreshRepository(repoUrl)
        val ext = db.dao().getAllExtensions().first().first()
        repository.setExtensionEnabled(ext.extensionId, true)

        val allSyncedAnime = db.dao().getAllUnifiedAnimeSync()
        assertTrue(allSyncedAnime.isNotEmpty())
        for (anime in allSyncedAnime) {
            assertTrue("Rating must be in 0..10 range, got ${anime.rating}", anime.rating in 0.0..10.0)
            val ratingStr = anime.rating.toString()
            val decimals = ratingStr.substringAfter(".", "")
            assertTrue("Rating must not have IEEE-754 precision artifacts: $ratingStr", decimals.length <= 2)
            assertFalse("No fake extension prefix in titles: ${anime.titleEn}", anime.titleEn.contains("Special:"))
            assertFalse("No HTML tags in synopsis", anime.synopsisAr.contains("<br") || anime.synopsisAr.contains("<p>"))
        }
    }

    // =========================================================================
    // 3. اختبارات الحلقات (Episode Extraction, Linking & Deduplication Tests)
    // =========================================================================
    @Test
    fun `stage 3 - episodes are accurately linked to parent anime with clean titles real durations and season separation`() = runBlocking {
        val dao = db.dao()
        val repoUrl = com.example.util.ExtensionEngine.presetRepositories.first().url
        repository.importOrRefreshRepository(repoUrl)
        val ext = dao.getAllExtensions().first().first()
        repository.setExtensionEnabled(ext.extensionId, true)

        val catalog = repository.unifiedCatalogFlow.first()
        val enabledExts = dao.getEnabledCompatibleExtensionsSync()

        // Check TV Series with specific episode count (e.g. Cyberpunk Edgerunners = 10 episodes)
        val cyberpunk = catalog.find { it.titleEn.contains("Cyberpunk: Edgerunners", ignoreCase = true) }
        assertNotNull("Cyberpunk: Edgerunners must exist in synced catalog", cyberpunk)
        val cyberpunkEpSources = dao.getActiveEpisodeSourcesForAnimeSync(cyberpunk!!.id)
        val cyberpunkEpisodes = repository.getEpisodesForAnime(cyberpunk, enabledExts, cyberpunkEpSources)
        assertEquals("Cyberpunk: Edgerunners must have exactly 10 episodes (not forced to 12 or 24)", 10, cyberpunkEpisodes.size)

        for ((idx, ep) in cyberpunkEpisodes.withIndex()) {
            assertEquals(idx + 1, ep.episodeNumber)
            assertEquals(cyberpunk.id, ep.animeId)
            assertTrue("sourceEpisodeId must be populated", ep.sourceEpisodeId.isNotBlank())
            assertTrue("sourceAnimeId must be populated", ep.sourceAnimeId.isNotBlank())
            assertTrue("episodePageUrl must be populated", ep.episodePageUrl.startsWith("https://"))
            assertFalse(
                "Episode title must not redundantly prefix the anime title: ${ep.titleAr}",
                ep.titleAr.startsWith("${cyberpunk.titleAr} - ")
            )
        }

        // Check Movie duration accuracy (e.g. Suzume = 122 min)
        val suzume = catalog.find { it.titleEn.contains("Suzume", ignoreCase = true) }
        assertNotNull("Suzume movie must exist in synced catalog", suzume)
        assertEquals("Movie", suzume!!.type)
        val suzumeEpSources = dao.getActiveEpisodeSourcesForAnimeSync(suzume.id)
        val suzumeEpisodes = repository.getEpisodesForAnime(suzume, enabledExts, suzumeEpSources)
        assertEquals("Movie must have 1 episode entry", 1, suzumeEpisodes.size)
        assertEquals("Suzume movie duration must be 122 minutes (not hardcoded 24)", 122, suzumeEpisodes.first().durationMinutes)
    }

    // =========================================================================
    // 4. اختبارات السيرفرات، فحص الروابط، والتشغيل (Servers, Link Validation & Player Tests)
    // =========================================================================
    @Test
    fun `stage 4 and 5 - stream link validator probes HTTP HEAD and Range GET rejects HTML extracts embeds and supports failover`() = runBlocking {
        // Spin up a real local HTTP server to test StreamLinkValidator with HEAD, Range GET fallback, HTML rejection, and 404
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = server.address.port

        // Endpoint 1: Valid MP4 supporting HEAD
        server.createContext("/video/valid.mp4") { exchange ->
            exchange.responseHeaders.add("Content-Type", "video/mp4")
            if (exchange.requestMethod == "HEAD") {
                exchange.sendResponseHeaders(200, -1)
            } else {
                val bytes = ByteArray(64) { 0x11 }
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }

        // Endpoint 2: HLS endpoint that rejects HEAD (405) but accepts GET with Range: bytes=0-1023
        server.createContext("/hls/master.m3u8") { exchange ->
            if (exchange.requestMethod == "HEAD") {
                exchange.sendResponseHeaders(405, -1)
                exchange.close()
            } else {
                exchange.responseHeaders.add("Content-Type", "application/vnd.apple.mpegurl")
                val m3u8Body = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1920x1080\nchunklist.m3u8\n".toByteArray()
                exchange.sendResponseHeaders(206, m3u8Body.size.toLong())
                exchange.responseBody.use { it.write(m3u8Body) }
                exchange.close()
            }
        }

        // Endpoint 3: Fake video link that actually returns a plain HTML page without any video stream (200 OK text/html)
        server.createContext("/watch/page.html") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            val html = "<html><body><h1>403 Forbidden Cloudflare Captcha</h1></body></html>".toByteArray()
            if (exchange.requestMethod == "HEAD") {
                exchange.sendResponseHeaders(200, -1)
            } else {
                exchange.sendResponseHeaders(200, html.size.toLong())
                exchange.responseBody.use { it.write(html) }
            }
            exchange.close()
        }

        // Endpoint 4: Broken link returning 404
        server.createContext("/video/missing.mp4") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }

        server.start()
        try {
            val baseUrl = "http://127.0.0.1:$port"

            // Test 1: HEAD valid MP4
            val mp4Res = ExtensionCatalogEngine.StreamLinkValidator.probeStreamUrlWithFallback(
                url = "$baseUrl/video/valid.mp4",
                headers = mapOf("Referer" to "$baseUrl/")
            )
            assertTrue("Valid MP4 link must pass validation", mp4Res.isValid)
            assertEquals(200, mp4Res.httpStatusCode)
            assertEquals("MP4", mp4Res.detectedStreamType)

            // Test 2: HEAD 405 -> Automatic fallback to bounded GET (Range: bytes=0-1023) for M3U8
            val hlsRes = ExtensionCatalogEngine.StreamLinkValidator.probeStreamUrlWithFallback(
                url = "$baseUrl/hls/master.m3u8",
                headers = mapOf("Referer" to "$baseUrl/", "User-Agent" to "AniFluxTest/1.0")
            )
            assertTrue("HLS link must pass via bounded GET fallback when HEAD returns 405", hlsRes.isValid)
            assertEquals(206, hlsRes.httpStatusCode)
            assertEquals("HLS", hlsRes.detectedStreamType)

            // Test 3: Plain HTML page masquerading as a stream URL must be rejected
            val htmlRes = ExtensionCatalogEngine.StreamLinkValidator.probeStreamUrlWithFallback(
                url = "$baseUrl/watch/page.html"
            )
            assertFalse("HTML page without media stream must be rejected as a direct video stream", htmlRes.isValid)
            assertEquals("HTML", htmlRes.detectedStreamType)
            assertTrue(htmlRes.failureReason.contains("HTML"))

            // Test 4: 404 broken link must be rejected
            val missingRes = ExtensionCatalogEngine.StreamLinkValidator.probeStreamUrlWithFallback(
                url = "$baseUrl/video/missing.mp4"
            )
            assertFalse("404 link must be rejected", missingRes.isValid)
            assertEquals(404, missingRes.httpStatusCode)

            // Test 5: EmbedStreamExtractor extracts direct playable stream & subtitles from embed HTML
            val sampleEmbedHtml = """
                <html>
                <script>
                    jwplayer("vplayer").setup({
                        sources: [{ file: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", label: "1080p FHD" }],
                        tracks: [{ file: "https://cdn.aniflux.net/subs/ep1.ar.vtt", label: "العربية", kind: "captions" }]
                    });
                </script>
                </html>
            """.trimIndent()
            val extractedEmbed = ExtensionCatalogEngine.EmbedStreamExtractor.extractDirectStreamFromEmbed(
                embedUrl = "https://anime4up.rest/embed/ep1",
                htmlContent = sampleEmbedHtml,
                referer = "https://anime4up.rest/embed/ep1"
            )
            assertNotNull("EmbedStreamExtractor must extract m3u8 from jwplayer setup HTML", extractedEmbed)
            assertEquals("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", extractedEmbed!!.streamUrl)
            assertEquals("HLS", extractedEmbed.streamType)
            assertEquals("https://anime4up.rest/embed/ep1", extractedEmbed.headers["Referer"])
            assertEquals(1, extractedEmbed.subtitleTracks.size)
            assertEquals("VTT", extractedEmbed.subtitleTracks.first().format)
        } finally {
            server.stop(0)
        }
    }

    // =========================================================================
    // 5. اختبارات عدم كسر الميزات الحالية (Non-Regression Tests)
    // =========================================================================
    @Test
    fun `stage 8 non-regression - manual anime and custom telegram servers remain intact across extension sync and disable`() = runBlocking {
        val dao = db.dao()

        // Verify base manual catalog works before any extension is enabled
        val initialCatalog = repository.unifiedCatalogFlow.first()
        assertTrue("Initial manual catalog must not be empty", initialCatalog.isNotEmpty())
        val soloLeveling = initialCatalog.find { it.titleEn.contains("Solo Leveling", ignoreCase = true) }
        assertNotNull("Manual anime Solo Leveling must exist", soloLeveling)

        // Create a manual anime with custom/Telegram episode servers
        val manualTelegramAnime = Anime(
            id = "manual_tg_anime_01",
            titleAr = "أنمي يدوي عبر بوت التيليجرام",
            titleEn = "Manual Telegram Bot Anime",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1806/126216l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1806/126216l.jpg",
            synopsisAr = "أنمي مضاف يدوياً مع سيرفرات تيليجرام مخصصة.",
            genres = listOf("أكشن"),
            rating = 9.2,
            status = "مستمر",
            type = "TV",
            year = 2025,
            season = "شتاء",
            releaseDay = "الأحد",
            totalEpisodes = 12,
            currentEpisodes = 2,
            studio = "MAPPA",
            customEpisodes = mapOf(
                "1" to "سيرفر التيليجرام FHD|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4,سيرفر احتياطي|https://media.w3.org/2010/05/sintel/trailer.mp4",
                "2" to "سيرفر التيليجرام الرئيسي|https://media.w3.org/2010/05/sintel/trailer.mp4"
            )
        )
        repository.savePublishedAnime(manualTelegramAnime)

        // Import and enable an extension, then disable it
        val repoUrl = com.example.util.ExtensionEngine.presetRepositories.first().url
        repository.importOrRefreshRepository(repoUrl)
        val ext = dao.getAllExtensions().first().first()
        repository.setExtensionEnabled(ext.extensionId, true)
        repository.setExtensionEnabled(ext.extensionId, false)

        // Verify manual Telegram anime and its episodes are completely untouched
        val catalogAfterDisable = repository.unifiedCatalogFlow.first()
        val persistedManual = catalogAfterDisable.find { it.id == "manual_tg_anime_01" }
        assertNotNull("Manual Telegram anime must remain in catalog after extension enable/disable", persistedManual)

        val manualEpisodes = repository.getEpisodesForAnime(persistedManual!!)
        assertEquals("Manual Telegram anime must retain its 2 episodes", 2, manualEpisodes.size)
        assertTrue(
            "Episode 1 must retain its custom Telegram server label",
            manualEpisodes.first().servers.any { it.nameAr.contains("سيرفر التيليجرام") }
        )
    }

    // =========================================================================
    // 6. الاختبار الفعلي الشامل لمستودع Abodabodd/re-3arabi (جميع الخطوات الـ 15)
    // =========================================================================
    @Test
    fun `stage 6 - end to end cloudstream re-3arabi repository pipeline across all 15 steps`() = runBlocking {
        val dao = db.dao()
        val repo3rabiUrl = "https://raw.githubusercontent.com/Abodabodd/re-3arabi/refs/heads/main/repo"

        // Steps 1-4: Add repo URL, detect format, fetch pluginLists -> plugins.json, extract all extensions
        val importRes = repository.importOrRefreshRepository(repo3rabiUrl)
        assertTrue("re-3arabi repository must import successfully", importRes.isSuccess)
        val bundle = importRes.getOrThrow()

        println("LIVE_RE3ARABI_FETCHED_URLS=" + bundle.fetchedLinkedUrls.joinToString(", "))
        println("LIVE_RE3ARABI_EXTENSIONS=" + bundle.extensions.joinToString(" | ") { "${it.name}[${it.pkgName}]->${it.sourceUrl}" })

        assertTrue(
            "Must fetch linked plugins.json from pluginLists, got: ${bundle.fetchedLinkedUrls}",
            bundle.fetchedLinkedUrls.any { it.endsWith("plugins.json", ignoreCase = true) }
        )
        assertTrue(
            "Must extract extensions from re-3arabi plugins.json, got ${bundle.extensions.size}",
            bundle.extensions.size >= 3
        )

        // Step 5: Verify compatibility & provider mainUrl resolution for Anime3rb and AnimeWitcher (fixing screenshot bugs)
        val allExts = dao.getAllExtensions().first()
        val anime3rb = allExts.first { it.name.equals("Anime3rb", ignoreCase = true) }
        val animeWitcher = allExts.first { it.name.equals("AnimeWitcher", ignoreCase = true) }
        val animerco = allExts.first { it.name.equals("Animerco", ignoreCase = true) }

        assertTrue("Anime3rb must be marked compatible", anime3rb.isCompatible)
        assertEquals(com.example.util.ExtensionEngine.STATUS_COMPATIBLE, anime3rb.compatibilityStatus)
        assertEquals("https://anime3rb.com", anime3rb.sourceUrl)

        assertTrue("AnimeWitcher must be marked compatible", animeWitcher.isCompatible)
        assertEquals(com.example.util.ExtensionEngine.STATUS_COMPATIBLE, animeWitcher.compatibilityStatus)
        assertEquals("https://animewitcher.com", animeWitcher.sourceUrl)

        assertTrue("Animerco must be marked compatible", animerco.isCompatible)
        assertEquals("https://animerco.org", animerco.sourceUrl)

        // Verify .cs3 ZIP archive safe metadata inspection (without DexClassLoader)
        val cs3ByteBuffer = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(cs3ByteBuffer).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write("""{"name":"Anime3rb","pluginClassName":"com.anime3rb.Anime3rbProvider","version":1}""".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(java.util.zip.ZipEntry("classes.dex"))
            zos.write("dex\n035\u0000https://anime3rb.com\u0000".toByteArray())
            zos.closeEntry()
        }
        val inspectedCs3 = com.example.util.RepositoryImporter.Cs3PackageInspector.inspectCs3ArchiveBytes(cs3ByteBuffer.toByteArray())
        assertNotNull("Inspected .cs3 metadata must not be null", inspectedCs3)
        assertTrue(inspectedCs3!!.hasDexBytecode)
        assertEquals("Anime3rb", inspectedCs3.pluginName)
        assertEquals("com.anime3rb.Anime3rbProvider", inspectedCs3.pluginClassName)
        assertTrue(inspectedCs3.extractedMainUrls.contains("https://anime3rb.com"))

        // Steps 6-8: Enable Anime3rb and sync all catalog pages without any 75-item ceiling
        val enable3rbRes = repository.setExtensionEnabled(anime3rb.extensionId, true)
        assertTrue("Enabling Anime3rb must succeed without error", enable3rbRes.isSuccess)
        val syncSummary3rb = enable3rbRes.getOrNull()!!
        assertEquals(
            "Anime3rb must fetch all 202 anime works across 17 pages (no 75-item ceiling)",
            202,
            syncSummary3rb.fetchedAnimeCount
        )
        assertEquals("Anime3rb must traverse all 17 pages", 17, syncSummary3rb.syncedPagesCount)

        // Also enable AnimeWitcher and verify it enables cleanly and deduplicates shared anime
        val enableWitcherRes = repository.setExtensionEnabled(animeWitcher.extensionId, true)
        assertTrue("Enabling AnimeWitcher must succeed", enableWitcherRes.isSuccess)
        val syncSummaryWitcher = enableWitcherRes.getOrNull()!!
        assertEquals(202, syncSummaryWitcher.fetchedAnimeCount)
        assertTrue(
            "Enabling second extension must merge shared anime via SmartAnimeDeduplicator",
            syncSummaryWitcher.deduplicatedMergedAnimeCount >= 180
        )

        // Verify LiveProviderHtmlAndApiScraper HTML parsing for catalog & episode servers
        val sampleCatalogHtml = """
            <html><body>
            <article class="anime-card">
                <a href="https://anime3rb.com/anime/one-piece" title="One Piece">
                    <img data-src="https://cdn.myanimelist.net/images/anime/6/73245l.jpg" />
                    <h3>ون بيس - One Piece</h3>
                </a>
            </article>
            <a class="next page-numbers" href="https://anime3rb.com/anime-list/page/2/">التالي</a>
            </body></html>
        """.trimIndent()
        val parsedHtmlPage = ExtensionCatalogEngine.LiveProviderHtmlAndApiScraper.parseProviderCatalogHtml(
            extension = anime3rb,
            html = sampleCatalogHtml,
            page = 1,
            baseUrl = anime3rb.sourceUrl
        )
        assertEquals(1, parsedHtmlPage.items.size)
        assertTrue(parsedHtmlPage.hasNextPage)
        assertEquals("https://anime3rb.com/anime-list/page/2/", parsedHtmlPage.nextPageUrl)

        // Steps 9-12: Select 3 distinct anime works, fetch their episodes on-demand, and select 3 episodes to extract servers
        val unifiedCatalog = repository.unifiedCatalogFlow.first()
        assertTrue("Unified catalog must contain >= 190 distinct anime works, got ${unifiedCatalog.size}", unifiedCatalog.size >= 190)

        val threeTestAnime = listOf(
            unifiedCatalog.first { it.titleEn.contains("Solo Leveling", ignoreCase = true) },
            unifiedCatalog.first { it.titleEn.contains("Frieren", ignoreCase = true) },
            unifiedCatalog.first { it.titleEn.contains("Bleach: Thousand-Year Blood War", ignoreCase = true) }
        )

        val enabledExts = dao.getEnabledCompatibleExtensionsSync()
        for (testAnime in threeTestAnime) {
            val refreshedEpsCount = repository.refreshAnimeEpisodesOnDemand(testAnime.id)
            assertTrue("On-demand episode refresh must return > 0 episodes for ${testAnime.titleEn}", refreshedEpsCount > 0)

            val epSources = dao.getActiveEpisodeSourcesForAnimeSync(testAnime.id)
            val episodes = repository.getEpisodesForAnime(testAnime, enabledExts, epSources)
            assertTrue("Anime ${testAnime.titleEn} must have episodes", episodes.isNotEmpty())

            // Pick Episode 1, refresh servers on demand, and inspect extracted servers
            val firstEp = episodes.first()
            val refreshedSrvCount = repository.refreshEpisodeServersOnDemand(testAnime.id, firstEp.episodeNumber)
            assertTrue("On-demand server refresh must return > 0 servers", refreshedSrvCount > 0)

            val updatedEpSources = dao.getActiveEpisodeSourcesForAnimeSync(testAnime.id)
            val updatedEp = repository.getEpisodesForAnime(testAnime, enabledExts, updatedEpSources).first()
            val extServers = updatedEp.servers.filter { it.sourceId.isNotBlank() }
            assertTrue("Episode ${updatedEp.episodeNumber} of ${testAnime.titleEn} must have >= 3 extension servers", extServers.size >= 3)

            for (srv in extServers) {
                assertTrue("Server URL must be valid HTTPS: ${srv.streamUrl}", srv.streamUrl.startsWith("https://"))
                assertTrue("Server streamType must be Direct or HLS", srv.streamType in listOf("MP4", "HLS", "DASH", "WEBM"))
                assertTrue("Server must include Referer header", srv.headers["Referer"]?.isNotBlank() == true)
                assertTrue("Server must include User-Agent header", srv.headers["User-Agent"]?.isNotBlank() == true)
            }
        }

        // Verify live 9-stage diagnostic report on Anime3rb passes all 9 stages with 202 anime across 17 pages
        val liveReport = repository.verifyExtensionLiveAndPersist(anime3rb.extensionId).getOrThrow()
        assertTrue("Anime3rb 9-stage live verification must pass", liveReport.passed)
        assertTrue("Live verification must confirm 202 anime works", liveReport.reasonAr.contains("202"))
    }
}

