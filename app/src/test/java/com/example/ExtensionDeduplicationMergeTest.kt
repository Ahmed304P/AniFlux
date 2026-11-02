package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AniFluxDatabase
import com.example.data.repository.AnimeRepository
import com.example.util.ExtensionCatalogEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExtensionDeduplicationMergeTest {

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

    @Test
    fun `test smart deduplicator normalizes Arabic English and Romaji titles and expands knowledge aliases`() {
        val normAr = ExtensionCatalogEngine.SmartAnimeDeduplicator.normalizeTitle("هجوم العمالقة: الموسم الأخير")
        val normEn = ExtensionCatalogEngine.SmartAnimeDeduplicator.normalizeTitle("Attack on Titan: The Final Season")
        val normJp = ExtensionCatalogEngine.SmartAnimeDeduplicator.normalizeTitle("Shingeki no Kyojin: The Final Season")

        assertTrue(normAr.isNotBlank())
        assertTrue(normEn.isNotBlank())
        assertTrue(normJp.isNotBlank())

        val payloadDeathNote = ExtensionCatalogEngine.SourceAnimePayload(
            sourceAnimeId = "dn_test",
            titleAr = "ديث نوت: مذكرة الموت",
            titleEn = "Death Note",
            titleJp = "Desu Noto",
            posterUrl = "https://cdn.myanimelist.net/images/anime/9/9453l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/9/9453l.jpg",
            synopsisAr = "قصة مذكرة الموت",
            genres = listOf("غموض", "نفسي"),
            totalEpisodes = 37,
            currentEpisodes = 37,
            year = 2006,
            episodes = emptyList()
        )
        val aliases = ExtensionCatalogEngine.SmartAnimeDeduplicator.collectNormalizedAliases(payloadDeathNote)
        assertTrue(aliases.any { it.contains("death note") })
        assertTrue(aliases.any { it.contains("desu noto") })
        assertTrue(aliases.any { it.contains("مذكره الموت") || it.contains("مذكرة الموت") })
    }

    @Test
    fun `test enabling two extensions deduplicates shared anime at database level and merges episodes and servers`() = runBlocking {
        val dao = db.dao()

        // 1. Import Arabic Official Repository (contains SAnime Cloud FHD & Witanime Stream)
        val importRes = repository.importOrRefreshRepository(
            com.example.util.ExtensionEngine.presetRepositories.first { it.url.contains("extensions-main") }.url
        )
        assertTrue("Repository should import successfully", importRes.isSuccess)

        val allExtensions = dao.getAllExtensions().first()
        val sanimeExt = allExtensions.find { it.pkgName.contains("sanime") || it.name.contains("SAnime", ignoreCase = true) }
        val witanimeExt = allExtensions.find { it.pkgName.contains("witanime") || it.pkgName.contains("witestream") || it.name.contains("Witanime", ignoreCase = true) }
        assertNotNull("SAnime extension must exist", sanimeExt)
        assertNotNull("Witanime extension must exist", witanimeExt)

        // All imported extensions must start disabled by default
        assertFalse(sanimeExt!!.isEnabled)
        assertFalse(witanimeExt!!.isEnabled)

        // 2. Enable Extension 1 (SAnime Cloud FHD)
        val enable1 = repository.setExtensionEnabled(sanimeExt.extensionId, true)
        assertTrue("Enabling SAnime should succeed and sync catalog", enable1.isSuccess)
        assertNotNull("Sync summary should be returned", enable1.getOrNull())

        // Verify Death Note was ingested from SAnime with 25 episodes
        val afterExt1Db = dao.getAllUnifiedAnimeSync().filter { it.activeSourcesCount > 0 }
        val deathNoteRecordsAfterExt1 = afterExt1Db.filter {
            it.titleEn.equals("Death Note", ignoreCase = true) ||
                it.titleAr.contains("مذكرة الموت")
        }
        assertEquals("Death Note must appear exactly once after Ext 1", 1, deathNoteRecordsAfterExt1.size)
        val dnExt1 = deathNoteRecordsAfterExt1.first()
        assertEquals(25, dnExt1.currentEpisodes)
        assertEquals(1, dnExt1.activeSourcesCount)

        // Verify Chainsaw Man was ingested from SAnime with 8 episodes
        val csmAfterExt1 = afterExt1Db.filter {
            it.titleEn.equals("Chainsaw Man", ignoreCase = true)
        }
        assertEquals(1, csmAfterExt1.size)
        assertEquals(8, csmAfterExt1.first().currentEpisodes)

        // Verify Fullmetal Alchemist: Brotherhood (exclusive to SAnime) is active
        val fmabAfterExt1 = afterExt1Db.filter {
            it.titleEn.contains("Fullmetal Alchemist", ignoreCase = true)
        }
        assertEquals(1, fmabAfterExt1.size)

        // 3. Enable Extension 2 (Witanime Stream) which has Death Note (37 eps, variant Arabic title 'ديث نوت: مذكرة الموت' & 'Desu Noto')
        // and Chainsaw Man (12 eps, variant Arabic title 'تشينسو مان - رجل المنشار')
        val enable2 = repository.setExtensionEnabled(witanimeExt.extensionId, true)
        assertTrue("Enabling Witanime should succeed and merge overlapping anime", enable2.isSuccess)

        val allDbRowsAfterExt2 = dao.getAllUnifiedAnimeSync()

        // CRITICAL DATABASE VERIFICATION: Death Note must exist ONLY ONCE in the entire unified_anime_catalog table!
        val deathNoteAllRows = allDbRowsAfterExt2.filter {
            it.titleEn.equals("Death Note", ignoreCase = true) ||
                it.titleAr.contains("مذكرة الموت") ||
                it.titleJp.contains("Desu Noto", ignoreCase = true)
        }
        assertEquals(
            "Smart Deduplication must prevent duplicate Death Note rows in unified_anime_catalog",
            1,
            deathNoteAllRows.size
        )

        val mergedDeathNote = deathNoteAllRows.first()
        assertEquals("Death Note must have 2 active extension links", 2, mergedDeathNote.activeSourcesCount)
        assertEquals("Death Note episode count must expand from 25 to 37 after merging Witanime", 37, mergedDeathNote.currentEpisodes)

        // Verify extension_anime_links has 2 links pointing to the same unifiedAnimeId
        val dnLinks = dao.getExtensionAnimeLinksByAnimeSync(mergedDeathNote.unifiedAnimeId)
        assertEquals("Both SAnime and Witanime must be linked to the single Death Note record", 2, dnLinks.size)
        assertTrue(dnLinks.all { it.isExtensionActive })

        // Verify episode & watch server merging on Death Note
        val enabledExtensionsNow = dao.getEnabledCompatibleExtensionsSync()
        val activeEpisodeSourcesNow = dao.getActiveEpisodeSourcesForAnimeSync(mergedDeathNote.unifiedAnimeId)
        val activeCatalogList = repository.unifiedCatalogFlow.first()
        val mergedDomainAnime = activeCatalogList.find { it.id == mergedDeathNote.unifiedAnimeId }
        assertNotNull("Merged Death Note must appear in unifiedCatalogFlow", mergedDomainAnime)

        val mergedEpisodes = repository.getEpisodesForAnime(
            mergedDomainAnime!!,
            enabledExtensionsNow,
            activeEpisodeSourcesNow
        )

        assertEquals("Merged Death Note must expose all 37 episodes", 37, mergedEpisodes.size)

        // Episode 1 is provided by BOTH SAnime and Witanime -> servers from both must be combined!
        val ep1 = mergedEpisodes.first { it.episodeNumber == 1 }
        assertTrue(
            "Episode 1 must contain servers from SAnime",
            ep1.servers.any { it.nameAr.contains("SAnime", ignoreCase = true) }
        )
        assertTrue(
            "Episode 1 must contain servers from Witanime",
            ep1.servers.any { it.nameAr.contains("Witanime", ignoreCase = true) }
        )
        assertTrue("Episode 1 must have at least 4 combined servers", ep1.servers.size >= 4)

        // Verify no server uses deprecated gtv-videos-bucket URLs
        assertTrue(
            "All Episode 1 servers must use verified playable stream URLs",
            ep1.servers.none { it.streamUrl.contains("gtv-videos-bucket") }
        )

        // Episode 30 is only in Witanime (since SAnime only had 1..25) -> must still be present with Witanime servers!
        val ep30 = mergedEpisodes.first { it.episodeNumber == 30 }
        assertTrue(
            "Episode 30 (exclusive to Witanime) must have Witanime servers",
            ep30.servers.any { it.nameAr.contains("Witanime", ignoreCase = true) }
        )

        // Verify Chainsaw Man deduplication (8 eps in SAnime + 12 eps in Witanime -> 1 record with 12 eps)
        val csmAllRows = allDbRowsAfterExt2.filter {
            it.titleEn.equals("Chainsaw Man", ignoreCase = true)
        }
        assertEquals("Chainsaw Man must appear only once in database", 1, csmAllRows.size)
        assertEquals(12, csmAllRows.first().currentEpisodes)
        assertEquals(2, csmAllRows.first().activeSourcesCount)

        // 4. Test Extension Disable Policy: Disable Extension 1 (SAnime)
        val disable1 = repository.setExtensionEnabled(sanimeExt.extensionId, false)
        assertTrue(disable1.isSuccess)

        val activeAfterDisable1 = repository.unifiedCatalogFlow.first()

        // Death Note is shared with Witanime (still enabled), so it MUST remain visible!
        val dnAfterDisable1 = activeAfterDisable1.find { it.id == mergedDeathNote.unifiedAnimeId }
        assertNotNull("Shared anime Death Note must remain visible when only SAnime is disabled", dnAfterDisable1)
        val dnDbAfterDisable1 = dao.getUnifiedAnimeByIdSync(mergedDeathNote.unifiedAnimeId)
        assertEquals(1, dnDbAfterDisable1!!.activeSourcesCount)

        // Fullmetal Alchemist: Brotherhood was exclusive to SAnime, so it MUST disappear from active catalog!
        val fmabAfterDisable1 = activeAfterDisable1.find {
            it.titleEn.contains("Fullmetal Alchemist", ignoreCase = true)
        }
        assertNull("Exclusive SAnime anime must be hidden immediately when SAnime is disabled", fmabAfterDisable1)

        // 5. Disable Extension 2 (Witanime) -> Death Note has 0 active extensions left, so it must disappear from active catalog
        val disable2 = repository.setExtensionEnabled(witanimeExt.extensionId, false)
        assertTrue(disable2.isSuccess)

        val activeAfterDisableBoth = repository.unifiedCatalogFlow.first()
        val dnAfterDisableBoth = activeAfterDisableBoth.find { it.id == mergedDeathNote.unifiedAnimeId }
        assertNull(
            "Death Note must be hidden when all extensions providing it are disabled",
            dnAfterDisableBoth
        )

        // Base catalog anime (e.g. Solo Leveling) must remain visible
        assertTrue(
            "Base catalog anime must remain visible",
            activeAfterDisableBoth.any { it.titleEn.contains("Solo Leveling", ignoreCase = true) }
        )
    }

    @Test
    fun `test enabling CloudStream extensions fetches 40+ real anime works and places them at the top of catalog with playable streams`() = runBlocking {
        val dao = db.dao()

        // Import CloudStream repo
        val csRepoUrl = com.example.util.ExtensionEngine.presetRepositories.first {
            it.url.contains("cloudstream-extensions")
        }.url
        val importRes = repository.importOrRefreshRepository(csRepoUrl)
        assertTrue(importRes.isSuccess)

        val allExts = dao.getAllExtensions().first()
        assertTrue(allExts.size >= 2)

        val firstExt = allExts.first()
        val syncRes = repository.setExtensionEnabled(firstExt.extensionId, true)
        assertTrue(syncRes.isSuccess)
        val summary = syncRes.getOrNull()
        assertNotNull(summary)
        assertTrue("Expected at least 40 anime fetched from extension, got ${summary!!.fetchedAnimeCount}", summary.fetchedAnimeCount >= 40)
        assertTrue("Expected newly added anime > 30, got ${summary.newlyAddedAnimeCount}", summary.newlyAddedAnimeCount >= 30)

        val catalog = repository.unifiedCatalogFlow.first()
        assertTrue("Catalog should have at least 45 anime works", catalog.size >= 45)
        assertTrue("No fake dummy anime should exist in catalog", catalog.none { it.id.startsWith("dummy_") })
        assertTrue("All posters must use real CDN URLs (not unsplash)", catalog.none { it.posterUrl.contains("unsplash") })

        // Verify Kusuriya no Hitorigoto has working servers
        val kusuriya = catalog.find {
            it.originalTitle.contains("Kusuriya no Hitorigoto", ignoreCase = true) ||
                it.titleEn.contains("Apothecary Diaries", ignoreCase = true)
        }
        assertNotNull("Kusuriya no Hitorigoto must be in catalog", kusuriya)
        val activeEpSources = dao.getActiveEpisodeSourcesForAnimeSync(kusuriya!!.id)
        val episodes = repository.getEpisodesForAnime(kusuriya, listOf(firstExt.copy(isEnabled = true)), activeEpSources)
        assertTrue("Kusuriya no Hitorigoto must have episodes", episodes.isNotEmpty())
        val ep1Servers = episodes.first().servers
        assertTrue("Each episode should have at least 6 servers from the enabled extension", ep1Servers.size >= 6)
        assertTrue("No server should point to forbidden gtv-videos-bucket URLs", ep1Servers.none { it.streamUrl.contains("gtv-videos-bucket") })
    }
}
