package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AniFluxDatabase
import com.example.data.repository.AnimeRepository
import com.example.util.ExtensionEngine
import com.example.util.RepositoryImporter
import com.example.util.RepositoryImporter.RepositoryFormat
import com.example.util.RepositoryImporter.RepositoryHttpResponse
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryImporterTest {

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
    fun `test format detection layer recognizes AniFlux CloudStream Aniyomi Stremio and rejects unknown JSON`() {
        val anifluxJson = """
            {
              "name": "AniFlux Official",
              "extensions": [
                {
                  "pkg": "com.aniflux.ext.ar.sanime",
                  "name": "SAnime Cloud",
                  "version": "3.4.0",
                  "baseUrl": "https://server.sanime.net",
                  "streamTemplate": "https://server.sanime.net/Video/{animeId}/{episode}.mp4",
                  "engineVersion": 3,
                  "format": "ANIFLUX_DECLARATIVE_V3"
                }
              ]
            }
        """.trimIndent()
        assertEquals(RepositoryFormat.ANIFLUX_NATIVE, RepositoryImporter.detectRepositoryFormat(anifluxJson))

        val cloudstreamRepoJson = """
            {
              "name": "Vietnamese & Arabic CS3 Repo",
              "description": "CloudStream plugin repository",
              "manifestVersion": 1,
              "pluginLists": [
                "https://raw.githubusercontent.com/example/cs-repo/builds/plugins.json"
              ]
            }
        """.trimIndent()
        assertEquals(RepositoryFormat.CLOUDSTREAM_REPO, RepositoryImporter.detectRepositoryFormat(cloudstreamRepoJson))

        val cloudstreamPluginsJson = """
            [
              {
                "name": "WitanimeProvider",
                "internalName": "com.lagradost.WitanimeProvider",
                "url": "https://raw.githubusercontent.com/example/cs-repo/builds/WitanimeProvider.cs3",
                "version": 18,
                "tvTypes": ["Anime", "AnimeMovie"],
                "language": "ar",
                "status": 1,
                "fileSize": 45120
              }
            ]
        """.trimIndent()
        assertEquals(RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST, RepositoryImporter.detectRepositoryFormat(cloudstreamPluginsJson))

        val aniyomiIndexMinJson = """
            [
              {
                "name": "Aniyomi: Anime4Up",
                "pkg": "eu.kanade.tachiyomi.animeextension.ar.anime4up",
                "apk": "aniyomi-ar.anime4up-v14.28.apk",
                "lang": "ar",
                "code": 28,
                "version": "14.28",
                "nsfw": 0,
                "sources": [
                  {
                    "name": "Anime4Up",
                    "lang": "ar",
                    "id": "2849283948293",
                    "baseUrl": "https://anime4up.rest, https://anime4up.tv"
                  }
                ]
              }
            ]
        """.trimIndent()
        assertEquals(RepositoryFormat.MIHON_ANIYOMI_INDEX, RepositoryImporter.detectRepositoryFormat(aniyomiIndexMinJson))

        val mihonRepoMetaJson = """
            {
              "meta": {
                "name": "Keiyoushi / Yuzono Extensions",
                "shortName": "Yuzono",
                "website": "https://example.org/repo",
                "signingKeyFingerprint": "1234567890abcdef"
              }
            }
        """.trimIndent()
        assertEquals(RepositoryFormat.MIHON_ANIYOMI_REPO_META, RepositoryImporter.detectRepositoryFormat(mihonRepoMetaJson))

        val stremioManifestJson = """
            {
              "id": "org.stremio.animecatalog",
              "version": "2.1.0",
              "name": "Anime Catalog Addon",
              "resources": ["catalog", "meta", "stream"],
              "catalogs": [{"type": "anime", "id": "top"}]
            }
        """.trimIndent()
        assertEquals(RepositoryFormat.STREMIO_MANIFEST, RepositoryImporter.detectRepositoryFormat(stremioManifestJson))

        val unknownRandomJson = """{"hello": "world", "count": 42}"""
        assertEquals(RepositoryFormat.UNKNOWN, RepositoryImporter.detectRepositoryFormat(unknownRandomJson))

        val unknownArrayJson = """[{"foo": "bar", "number": 123}]"""
        assertEquals(RepositoryFormat.UNKNOWN, RepositoryImporter.detectRepositoryFormat(unknownArrayJson))
    }

    @Test
    fun `test CloudStream repo_json fetches relative and absolute plugins_json deduplicates extensions and checks runtime support`() {
        val repoUrl = "https://cdn.example-cs-repo.org/main/repo.json"
        val repoJson = """
            {
              "name": "Arab & Global CloudStream Hub",
              "description": "Official CloudStream multi-list repository",
              "manifestVersion": 1,
              "pluginLists": [
                "plugins-ar.json",
                "./plugins-ar.json",
                "https://cdn.example-cs-repo.org/main/sub/plugins-en.json"
              ]
            }
        """.trimIndent()

        // Contains 2 plugins: one v10 of 'com.cs.shared' (raw .cs3) and one 'com.cs.ar.only' (raw .cs3)
        val pluginsArJson = """
            [
              {
                "name": "SharedAnime CS3 (Old v10)",
                "internalName": "com.cs.shared",
                "url": "./SharedAnime.cs3",
                "version": 10,
                "tvTypes": ["Anime"],
                "language": "ar",
                "iconUrl": "https://cdn.example-cs-repo.org/icons/shared_%size%.png"
              },
              {
                "name": "Anime4Up Raw CS3",
                "internalName": "com.cs.ar.only",
                "url": "builds/Anime4Up.cs3",
                "version": 15,
                "tvTypes": ["Anime", "AnimeMovie"],
                "language": "ar"
              }
            ]
        """.trimIndent()

        // Contains updated v22 of 'com.cs.shared' WITH a valid HTTP bridge, plus 'com.cs.en.gogo'
        val pluginsEnJson = """
            [
              {
                "name": "SharedAnime CS3 (Bridged v22)",
                "internalName": "com.cs.shared",
                "url": "https://cdn.example-cs-repo.org/main/sub/SharedAnime.cs3",
                "baseUrl": "https://stream.shared-anime.org",
                "streamTemplate": "https://stream.shared-anime.org/v3/stream/{animeId}/ep-{episode}/playlist.m3u8",
                "version": 22,
                "tvTypes": ["Anime"],
                "language": "en"
              },
              {
                "name": "GogoCloud Bridge",
                "internalName": "com.cs.en.gogo",
                "url": "https://cdn.example-cs-repo.org/main/sub/GogoCloud.cs3",
                "baseUrl": "https://api.gogocloud-stream.org",
                "streamTemplate": "https://api.gogocloud-stream.org/hls/{animeId}/ep-{episode}/master.m3u8",
                "version": 30,
                "tvTypes": ["Anime"],
                "language": "en"
              }
            ]
        """.trimIndent()

        var arFetchCount = 0
        val fakeFetcher = RepositoryImporter.RepoUrlFetcher { requestedUrl ->
            when (requestedUrl) {
                repoUrl -> RepositoryHttpResponse(repoUrl, 200, "application/json", repoJson)
                "https://cdn.example-cs-repo.org/main/plugins-ar.json" -> {
                    arFetchCount++
                    RepositoryHttpResponse(requestedUrl, 200, "application/json", pluginsArJson)
                }
                "https://cdn.example-cs-repo.org/main/sub/plugins-en.json" -> {
                    RepositoryHttpResponse(requestedUrl, 200, "application/json", pluginsEnJson)
                }
                else -> RepositoryHttpResponse(requestedUrl, 404, "text/plain", null, "HTTP 404")
            }
        }

        val result = RepositoryImporter.importRepositoryFromUrl(repoUrl, fakeFetcher)
        assertTrue("CloudStream repo.json import should succeed", result.isSuccess)
        val manifest = result.getOrThrow()

        assertEquals(RepositoryFormat.CLOUDSTREAM_REPO, manifest.detectedFormat)
        assertEquals("Arab & Global CloudStream Hub", manifest.repoName)
        // Verify duplicate URL (`plugins-ar.json` and `./plugins-ar.json`) was only fetched once!
        assertEquals(1, arFetchCount)
        assertEquals(2, manifest.fetchedLinkedUrls.size)

        // Total raw entries across both files = 4, but 'com.cs.shared' is duplicated -> must deduplicate to 3!
        assertEquals("Deduplicated extensions count must be 3", 3, manifest.extensions.size)
        // All 3 CloudStream .cs3 plugins are bridged via ADAPTER_CLOUDSTREAM_UNIVERSAL and executable in runtime!
        assertEquals("All 3 CloudStream .cs3 plugins should be bridged and compatible", 3, manifest.compatibleCount)

        val sharedExt = manifest.extensions.find { it.id == "com.cs.shared" }
        assertNotNull(sharedExt)
        assertEquals(22, sharedExt!!.versionCode)
        assertTrue(sharedExt.isCompatible)
        assertTrue(sharedExt.isExecutableInRuntime)

        val bridgedCs3Ext = manifest.extensions.find { it.id == "com.cs.ar.only" }
        assertNotNull(bridgedCs3Ext)
        assertTrue("CloudStream .cs3 plugin must be bridged via CloudStream Compatibility Sandbox v3", bridgedCs3Ext!!.isCompatible)
        assertTrue(bridgedCs3Ext.isExecutableInRuntime)
        assertEquals("ADAPTER_CLOUDSTREAM_UNIVERSAL", bridgedCs3Ext.adapterId)
        assertEquals("https://cdn.example-cs-repo.org/main/builds/Anime4Up.cs3", bridgedCs3Ext.downloadOrPackageUrl)
    }

    @Test
    fun `test Mihon and Aniyomi index_min_json and repo_json meta extraction and Universal Bridge execution`() {
        val repoMetaUrl = "https://raw.example-aniyomi.org/repo/repo.json"
        val indexMinUrl = "https://raw.example-aniyomi.org/repo/index.min.json"

        val repoMetaJson = """
            {
              "meta": {
                "name": "Yuzono Official Anime Extensions",
                "shortName": "Yuzono",
                "website": "https://raw.example-aniyomi.org/repo",
                "signingKeyFingerprint": "abcdef1234567890"
              }
            }
        """.trimIndent()

        val indexMinJson = """
            [
              {
                "name": "Aniyomi: WitAnime APK",
                "pkg": "eu.kanade.tachiyomi.animeextension.ar.witanime",
                "apk": "aniyomi-ar.witanime-v14.20.apk",
                "lang": "ar",
                "code": 20,
                "version": "14.20",
                "nsfw": 0,
                "sources": [
                  {
                    "name": "WitAnime",
                    "lang": "ar",
                    "id": "111222333444",
                    "baseUrl": "https://witanime.cyou, https://witanime.red"
                  }
                ]
              },
              {
                "name": "Aniyomi: AnimePahe APK",
                "pkg": "eu.kanade.tachiyomi.animeextension.en.animepahe",
                "apk": "aniyomi-en.animepahe-v14.35.apk",
                "lang": "en",
                "code": 35,
                "version": "14.35",
                "nsfw": 0,
                "sources": [
                  {
                    "name": "AnimePahe",
                    "lang": "en",
                    "id": "555666777888",
                    "baseUrl": "https://animepahe.ru, https://animepahe.com"
                  }
                ]
              }
            ]
        """.trimIndent()

        val fakeFetcher = RepositoryImporter.RepoUrlFetcher { url ->
            when (url) {
                repoMetaUrl -> RepositoryHttpResponse(url, 200, "application/json", repoMetaJson)
                indexMinUrl -> RepositoryHttpResponse(url, 200, "application/json", indexMinJson)
                else -> RepositoryHttpResponse(url, 404, "text/plain", null, "Not found")
            }
        }

        // Test importing via repo.json (which automatically resolves and fetches sibling index.min.json)
        val result = RepositoryImporter.importRepositoryFromUrl(repoMetaUrl, fakeFetcher)
        assertTrue("Aniyomi repo.json -> index.min.json should parse cleanly", result.isSuccess)
        val manifest = result.getOrThrow()

        assertEquals("Yuzono Official Anime Extensions", manifest.repoName)
        assertEquals(2, manifest.extensions.size)
        assertEquals(2, manifest.compatibleCount)
        assertTrue(manifest.hasExecutableExtensions)

        val witExt = manifest.extensions.first { it.id == "eu.kanade.tachiyomi.animeextension.ar.witanime" }
        assertEquals("WitAnime APK", witExt.name)
        assertEquals("14.20", witExt.version)
        assertEquals(20, witExt.versionCode)
        // Multi-URL baseUrl must resolve to the first valid URL
        assertEquals("https://witanime.cyou", witExt.sourceUrl)
        // Relative APK filename must resolve to ${repoBaseDir}/apk/${apk}
        assertEquals("https://raw.example-aniyomi.org/repo/apk/aniyomi-ar.witanime-v14.20.apk", witExt.downloadOrPackageUrl)
        // Standard icon URL must resolve to ${repoBaseDir}/icon/${pkg}.png
        assertEquals("https://raw.example-aniyomi.org/repo/icon/eu.kanade.tachiyomi.animeextension.ar.witanime.png", witExt.iconUrl)
        assertTrue(witExt.isCompatible)
        assertEquals("ADAPTER_ANIYOMI_BRIDGE", witExt.adapterId)
    }

    @Test
    fun `test security guardrails block localhost internal IPs metadata endpoints and unsafe schemes`() {
        val blockedUrls = listOf(
            "http://localhost/repo.json",
            "http://127.0.0.1:8080/index.min.json",
            "http://0.0.0.0/repo.json",
            "http://10.0.0.15/repo.json",
            "http://172.16.5.4/repo.json",
            "http://172.31.255.1/repo.json",
            "http://192.168.1.100/repo.json",
            "http://169.254.169.254/latest/meta-data/",
            "https://metadata.google.internal/computeMetadata/v1/",
            "http://myserver.local/repo.json",
            "http://intranet.internal/repo.json",
            "javascript:alert(1)",
            "file:///sdcard/Download/repo.json"
        )

        for (unsafeUrl in blockedUrls) {
            val check = RepositoryImporter.validateSafeExternalUrl(unsafeUrl)
            assertTrue("URL must be blocked by security validator: $unsafeUrl", check.isFailure)
        }

        // Also test SSRF inside CloudStream pluginLists (external repo.json pointing to internal 127.0.0.1)
        val externalRepoUrl = "https://safe-external-host.org/repo.json"
        val maliciousCsRepo = """
            {
              "name": "Malicious SSRF Repo",
              "pluginLists": [
                "http://127.0.0.1:9000/internal-secret.json"
              ]
            }
        """.trimIndent()
        val res = RepositoryImporter.parseAndResolveRepository(
            repoUrl = externalRepoUrl,
            rawJson = maliciousCsRepo,
            fetcher = { url -> RepositoryHttpResponse(url, 200, "application/json", "[]") }
        )
        assertTrue("SSRF inside pluginLists must be rejected", res.isFailure)
        assertTrue(res.exceptionOrNull()!!.message!!.contains(RepositoryImporter.ERR_CLOUDSTREAM_INVALID_PLUGINS))
    }

    @Test
    fun `test specific Arabic diagnostic error messages for unknown format unreachable linked file and invalid CloudStream plugins`() {
        // 1. Unknown repository format
        val unknownRes = RepositoryImporter.parseAndResolveRepository(
            repoUrl = "https://example.org/unknown.json",
            rawJson = """{"randomKey": [1, 2, 3]}"""
        )
        assertTrue(unknownRes.isFailure)
        assertTrue(unknownRes.exceptionOrNull()!!.message!!.contains(RepositoryImporter.ERR_UNKNOWN_REPO_FORMAT))

        // 2. Unreachable linked plugin list
        val csRepoWithDeadChild = """
            {
              "name": "Dead Link CS Repo",
              "pluginLists": ["https://example.org/missing-plugins.json"]
            }
        """.trimIndent()
        val deadChildRes = RepositoryImporter.parseAndResolveRepository(
            repoUrl = "https://example.org/repo.json",
            rawJson = csRepoWithDeadChild,
            fetcher = { url ->
                RepositoryHttpResponse(
                    url = url,
                    statusCode = 404,
                    contentType = "text/plain",
                    body = null,
                    errorMessage = "${RepositoryImporter.ERR_UNREACHABLE_PLUGIN_LIST} (HTTP 404)"
                )
            }
        )
        assertTrue(deadChildRes.isFailure)
        assertTrue(deadChildRes.exceptionOrNull()!!.message!!.contains(RepositoryImporter.ERR_UNREACHABLE_PLUGIN_LIST))

        // 3. Recognized CloudStream repo.json but invalid plugins.json content
        val csRepoWithCorruptPlugins = """
            {
              "name": "Corrupt Plugins CS Repo",
              "pluginLists": ["https://example.org/corrupt-plugins.json"]
            }
        """.trimIndent()
        val corruptChildRes = RepositoryImporter.parseAndResolveRepository(
            repoUrl = "https://example.org/repo.json",
            rawJson = csRepoWithCorruptPlugins,
            fetcher = { url ->
                RepositoryHttpResponse(
                    url = url,
                    statusCode = 200,
                    contentType = "application/json",
                    body = """[{"notAPlugin": true}]"""
                )
            }
        )
        assertTrue(corruptChildRes.isFailure)
        assertTrue(corruptChildRes.exceptionOrNull()!!.message!!.contains(RepositoryImporter.ERR_CLOUDSTREAM_INVALID_PLUGINS))
    }

    @Test
    fun `test existing AniFlux preset repositories and new CloudStream and Aniyomi presets import into Room without regression`() = runBlocking {
        for (preset in ExtensionEngine.presetRepositories) {
            val res = repository.importOrRefreshRepository(preset.url)
            assertTrue("Preset repository (${preset.titleAr}) must import cleanly", res.isSuccess)
            val bundle = res.getOrThrow()
            assertTrue("Preset (${preset.titleAr}) must have at least 2 extensions", bundle.extensions.size >= 2)
            assertEquals(bundle.extensions.size, bundle.repo.extensionCount)
        }

        val allReposInDb = db.dao().getAllExtensionRepos().first()
        assertEquals(ExtensionEngine.presetRepositories.size, allReposInDb.size)
    }

    @Test
    fun `test 3rabi CloudStream cs3 extensions AnimeWitcher Anime3rb Animerco can be enabled synced and verified`() = runBlocking {
        val repoUrl = "https://raw.githubusercontent.com/Abodabodd/3rabi/main/repo.json"
        val pluginsUrl = "https://raw.githubusercontent.com/Abodabodd/3rabi/builds/plugins.json"
        val repoJson = """
            {
              "name": "3rabi عربي",
              "description": "CloudStream Arabic Extensions",
              "manifestVersion": 1,
              "pluginLists": ["$pluginsUrl"]
            }
        """.trimIndent()
        val pluginsJson = """
            [
              {
                "name": "AnimeWitcher",
                "internalName": "Animewitcher",
                "url": "https://github.com/Abodabodd/3rabi/raw/builds/Animewitcher.cs3",
                "repositoryUrl": "https://github.com/Abodabodd/3rabi",
                "version": 3,
                "tvTypes": ["Anime"],
                "language": "ar",
                "authors": ["Abodabodd"]
              },
              {
                "name": "Anime3rb",
                "internalName": "Anim3rb",
                "url": "https://github.com/Abodabodd/3rabi/raw/builds/Anim3rb.cs3",
                "repositoryUrl": "https://github.com/Abodabodd/3rabi",
                "version": 1,
                "tvTypes": ["TvSeries", "Anime"],
                "language": "ar",
                "authors": ["Abodabodd"]
              },
              {
                "name": "Animerco",
                "internalName": "Animerco",
                "url": "https://github.com/Abodabodd/3rabi/raw/builds/Animerco.cs3",
                "repositoryUrl": "https://github.com/Abodabodd/3rabi",
                "version": 2,
                "tvTypes": ["Anime"],
                "language": "ar",
                "authors": ["Abodabodd"]
              }
            ]
        """.trimIndent()

        val manifestRes = RepositoryImporter.importRepositoryFromUrl(repoUrl) { url ->
            when (url) {
                repoUrl -> RepositoryHttpResponse(url, 200, "application/json", repoJson)
                pluginsUrl -> RepositoryHttpResponse(url, 200, "application/json", pluginsJson)
                else -> RepositoryHttpResponse(url, 404, "text/plain", null, "404")
            }
        }
        assertTrue(manifestRes.isSuccess)
        val bundle = RepositoryImporter.toImportedRepoBundle(manifestRes.getOrThrow()) { "[]" }
        db.dao().upsertExtensionRepo(bundle.repo)
        db.dao().upsertExtensions(bundle.extensions)

        for (ext in bundle.extensions) {
            assertTrue("${ext.name} must be compatible", ext.isCompatible)
            assertEquals(ExtensionEngine.STATUS_COMPATIBLE, ext.compatibilityStatus)

            val toggleRes = repository.setExtensionEnabled(ext.extensionId, true)
            assertTrue("${ext.name} must enable and sync cleanly", toggleRes.isSuccess)
            val syncSummary = toggleRes.getOrThrow()
            assertNotNull(syncSummary)
            assertTrue(syncSummary!!.fetchedAnimeCount >= 15)
            assertTrue(syncSummary.syncedServersCount >= 30)

            val verifyRes = repository.verifyExtensionLiveAndPersist(ext.extensionId)
            assertTrue(verifyRes.isSuccess)
            assertTrue("${ext.name} must pass all 9 verification stages", verifyRes.getOrThrow().passed)
        }
    }
}
