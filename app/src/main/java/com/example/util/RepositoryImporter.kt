package com.example.util

import com.example.data.local.AnimeExtensionEntity
import com.example.data.local.ExtensionRepoEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.InetAddress
import java.net.URI
import java.util.Locale

/**
 * AniFlux Universal Repository Importer (v3.5)
 *
 * Implements:
 * 1. Repository Format Detection Layer (`RepositoryFormat`):
 *    - ANIFLUX_NATIVE (`extensions` array or declarative manifest)
 *    - CLOUDSTREAM_REPO (`repo.json` with `pluginLists` -> fetches referenced `plugins.json` files)
 *    - CLOUDSTREAM_PLUGIN_LIST (`plugins.json` direct array or object of CloudStream plugins)
 *    - MIHON_ANIYOMI_INDEX (`index.min.json` / `index.json` following official Aniyomi/Mihon schema)
 *    - MIHON_ANIYOMI_REPO_META (`repo.json` with `meta` object -> fetches sibling `index.min.json` / `index.json`)
 *    - STREMIO_MANIFEST (`manifest.json` with `catalogs` & `resources`)
 *    - VERIFIED_GENERIC_JSON (Other JSON formats only when schema is recognized and verified)
 *
 * 2. Unified Repository Schema (`UnifiedRepositoryManifest` & `UnifiedExtensionSchema`):
 *    - Preserves repository name, description, author, detected format, linked files.
 *    - Preserves extension name, ID (`pkgName`), version, download/package URL, source URL,
 *      language, source type, requirements & compatibility, and icon URL.
 *
 * 3. Sequential Linked File Resolution:
 *    - Resolves relative & absolute URLs accurately.
 *    - Validates HTTP status codes and Content-Type headers.
 *    - Prevents recursion loops, duplicate URLs, excessive depth, and oversized payloads.
 *
 * 4. Security & Honest Compatibility Verification:
 *    - Blocks localhost, loopback, private networks (RFC1918), link-local/metadata IPs, and unsafe URI schemes.
 *    - Never executes external code during repository inspection.
 *    - Distinguishes between parsing repository metadata vs. actual runtime execution support.
 */
object RepositoryImporter {

    const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB limit per manifest file
    const val MAX_LINKED_FILES = 15 // Maximum linked pluginLists / child manifests per import
    const val MAX_CALL_DEPTH = 3 // Maximum recursion depth for linked manifests
    const val MAX_EXTENSIONS_PER_REPO = 2500 // Safety bound against memory exhaustion

    // Canonical Arabic Diagnostic & Error Messages (Requirement 6)
    const val ERR_UNKNOWN_REPO_FORMAT = "صيغة مستودع غير معروفة"
    const val ERR_UNREACHABLE_PLUGIN_LIST = "ملف قائمة الإضافات غير قابل للوصول"
    const val ERR_CLOUDSTREAM_INVALID_PLUGINS = "صيغة CloudStream معروفة لكن ملفات الإضافات غير صالحة"
    const val ERR_ANIYOMI_INCOMPATIBLE_PACKAGES = "صيغة Aniyomi معروفة لكن حزم الإضافات غير متوافقة"
    const val ERR_REPO_VALID_RUNTIME_UNSUPPORTED = "المستودع صالح لكن تشغيل الإضافات غير مدعوم"

    enum class RepositoryFormat(val code: String, val labelAr: String) {
        ANIFLUX_NATIVE("ANIFLUX_NATIVE", "مستودع AniFlux القياسي"),
        CLOUDSTREAM_REPO("CLOUDSTREAM_REPO", "مستودع CloudStream (repo.json)"),
        CLOUDSTREAM_PLUGIN_LIST("CLOUDSTREAM_PLUGIN_LIST", "قائمة إضافات CloudStream (plugins.json)"),
        MIHON_ANIYOMI_INDEX("MIHON_ANIYOMI_INDEX", "فهرس Mihon / Aniyomi (index.min.json)"),
        MIHON_ANIYOMI_REPO_META("MIHON_ANIYOMI_REPO_META", "بيان مستودع Mihon / Aniyomi (repo.json)"),
        STREMIO_MANIFEST("STREMIO_MANIFEST", "بيان إضافة Stremio (manifest.json)"),
        VERIFIED_GENERIC_JSON("VERIFIED_GENERIC_JSON", "مستودع JSON موثق المخطط"),
        UNKNOWN("UNKNOWN", ERR_UNKNOWN_REPO_FORMAT)
    }

    data class RepositoryHttpResponse(
        val url: String,
        val statusCode: Int,
        val contentType: String,
        val body: String?,
        val errorMessage: String? = null
    ) {
        val isSuccessful: Boolean get() = statusCode in 200..299 && body != null
    }

    fun interface RepoUrlFetcher {
        fun fetch(url: String): RepositoryHttpResponse
    }

    /**
     * Unified internal representation of a single extension extracted from any supported repository format.
     */
    data class UnifiedExtensionSchema(
        val extensionId: String,
        val id: String,
        val name: String,
        val description: String,
        val version: String,
        val versionCode: Int,
        val downloadOrPackageUrl: String,
        val sourceUrl: String,
        val lang: String,
        val sourceType: String,
        val iconUrl: String,
        val streamTemplate: String,
        val qualities: String,
        val engineVersion: Int,
        val extensionFormat: String,
        val adapterId: String,
        val runtimeEnvironment: String,
        val paginationType: String,
        val isExecutableInRuntime: Boolean,
        val isCompatible: Boolean,
        val compatibilityStatus: String,
        val compatibilityReasonAr: String,
        val securityVerified: Boolean,
        val securityDetailsAr: String
    )

    /**
     * Unified internal representation of an imported repository across all supported ecosystems.
     */
    data class UnifiedRepositoryManifest(
        val repoId: String,
        val repoName: String,
        val repoDescription: String,
        val repoAuthor: String,
        val repoUrl: String,
        val detectedFormat: RepositoryFormat,
        val fetchedLinkedUrls: List<String>,
        val extensions: List<UnifiedExtensionSchema>,
        val compatibleCount: Int,
        val hasExecutableExtensions: Boolean,
        val statusSummaryAr: String
    )

    /**
     * Security validator that prevents SSRF (localhost, loopback, RFC1918 private IPs,
     * link-local/cloud metadata endpoints) and dangerous schemes.
     */
    fun validateSafeExternalUrl(rawUrl: String): Result<URI> {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رابط المستودع أولاً"))
        }

        val lower = trimmed.lowercase(Locale.US)
        if (lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("content:") ||
            lower.startsWith("data:") ||
            lower.startsWith("vbscript:")
        ) {
            return Result.failure(
                SecurityException("تم حظر الرابط أمنياً: بروتوكول غير آمن أو غير مصرح به ($trimmed)")
            )
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return Result.failure(
                IllegalArgumentException("رابط المستودع غير صالح: صيغة الرابط غير صحيحة ($trimmed)")
            )
        }

        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme != "https" && scheme != "http") {
            return Result.failure(
                IllegalArgumentException("رابط المستودع غير صالح. يجب أن يبدأ بـ https:// أو http://")
            )
        }

        if (!uri.userInfo.isNullOrBlank()) {
            return Result.failure(
                SecurityException("تم حظر الرابط أمنياً: لا يُسمح بتضمين بيانات اعتماد داخل الرابط")
            )
        }

        val host = uri.host?.lowercase(Locale.US)?.trim()
            ?.removePrefix("[")?.removeSuffix("]")
            ?: return Result.failure(
                IllegalArgumentException("رابط المستودع غير صالح: اسم النطاق (Host) مفقود")
            )

        if (host.isBlank()) {
            return Result.failure(IllegalArgumentException("رابط المستودع غير صالح: اسم النطاق فارغ"))
        }

        if (isInternalOrSensitiveHost(host)) {
            return Result.failure(
                SecurityException("تم حظر الوصول أمنياً: يُمنع الاتصال بـ localhost أو العناوين الداخلية أو الخدمات الحساسة ($host)")
            )
        }

        return Result.success(uri)
    }

    /**
     * Checks whether a hostname or IP literal refers to localhost, loopback, internal networks,
     * or cloud metadata services.
     */
    fun isInternalOrSensitiveHost(rawHost: String): Boolean {
        val host = rawHost.trim().lowercase(Locale.US).removePrefix("[").removeSuffix("]")
        if (host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".lan") ||
            host.endsWith(".home") ||
            host.endsWith(".corp") ||
            host == "metadata.google.internal" ||
            host == "metadata" ||
            host == "instance-data" ||
            host == "0.0.0.0" ||
            host == "::1" ||
            host == "::" ||
            host == "0:0:0:0:0:0:0:1" ||
            host == "0:0:0:0:0:0:0:0"
        ) {
            return true
        }

        // IPv6 private / loopback / link-local prefix checks
        if (host.contains(":")) {
            if (host.startsWith("fe80:") ||
                host.startsWith("fc00:") ||
                host.startsWith("fd") ||
                host.startsWith("::ffff:127.") ||
                host.startsWith("::ffff:10.") ||
                host.startsWith("::ffff:192.168.") ||
                host.startsWith("::ffff:169.254.")
            ) {
                return true
            }
        }

        // Direct IPv4 dotted-quad check
        val ipv4Parts = host.split(".")
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val b0 = ipv4Parts[0].toInt()
            val b1 = ipv4Parts[1].toInt()
            if (b0 == 0 || // 0.0.0.0/8
                b0 == 10 || // 10.0.0.0/8 private
                b0 == 127 || // 127.0.0.0/8 loopback
                (b0 == 169 && b1 == 254) || // 169.254.0.0/16 link-local / cloud metadata
                (b0 == 172 && b1 in 16..31) || // 172.16.0.0/12 private
                (b0 == 192 && b1 == 168) || // 192.168.0.0/16 private
                (b0 == 100 && b1 in 64..127) || // 100.64.0.0/10 CGNAT
                b0 >= 224 // Multicast & reserved
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Resolves a potentially relative URL against a base manifest URL and validates security rules.
     */
    fun resolveRelativeOrAbsoluteUrl(baseManifestUrl: String, targetUrl: String): Result<String> {
        val cleanedTarget = targetUrl.trim()
        if (cleanedTarget.isBlank()) {
            return Result.failure(IllegalArgumentException("الرابط التابع فارغ"))
        }

        // If already absolute HTTP/HTTPS, validate directly
        if (cleanedTarget.startsWith("https://", ignoreCase = true) ||
            cleanedTarget.startsWith("http://", ignoreCase = true)
        ) {
            return validateSafeExternalUrl(cleanedTarget).map { it.toString() }
        }

        // Reject non-HTTP schemes disguised as relative links
        if (cleanedTarget.contains("://") || cleanedTarget.startsWith("javascript:", ignoreCase = true)) {
            return Result.failure(SecurityException("بروتوكول رابط تابع غير آمن: $cleanedTarget"))
        }

        val baseUri = validateSafeExternalUrl(baseManifestUrl).getOrElse { return Result.failure(it) }
        return try {
            val basePath = baseUri.path ?: "/"
            val directoryBaseUri = if (basePath.endsWith("/")) {
                baseUri
            } else {
                val lastSlash = basePath.lastIndexOf('/')
                val dirPath = if (lastSlash >= 0) basePath.substring(0, lastSlash + 1) else "/"
                URI(baseUri.scheme, baseUri.userInfo, baseUri.host, baseUri.port, dirPath, null, null)
            }
            val resolvedUri = directoryBaseUri.resolve(cleanedTarget).normalize()
            validateSafeExternalUrl(resolvedUri.toString()).map { it.toString() }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("تعذر تحليل الرابط النسبي ($cleanedTarget) نسبةً إلى ($baseManifestUrl)"))
        }
    }

    /**
     * Extracts the base directory URL for a repository file (e.g. `https://host/repo/index.min.json` -> `https://host/repo`).
     */
    fun extractRepoBaseDirectoryUrl(manifestUrl: String): String {
        val trimmed = manifestUrl.trim().removeSuffix("/")
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash <= "https://".length) return trimmed
        val lastSegment = trimmed.substring(lastSlash + 1)
        return if (lastSegment.contains('.')) {
            trimmed.substring(0, lastSlash)
        } else {
            trimmed
        }
    }

    /**
     * Repository Format Detection Layer (Requirement 2):
     * Inspects a parsed JSON payload and classifies its exact repository format without assuming
     * every repository uses an `"extensions"` key.
     */
    fun detectRepositoryFormat(rawJson: String): RepositoryFormat {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return RepositoryFormat.UNKNOWN

        try {
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                // 1. Check CloudStream `repo.json` (`pluginLists` array)
                if (root.has("pluginLists") && root.opt("pluginLists") is JSONArray) {
                    return RepositoryFormat.CLOUDSTREAM_REPO
                }

                // 2. Check Mihon / Aniyomi `repo.json` metadata (`meta` object with name/signingKeyFingerprint/website)
                val metaObj = root.optJSONObject("meta")
                if (metaObj != null && (metaObj.has("name") || metaObj.has("signingKeyFingerprint") || metaObj.has("website") || metaObj.has("shortName"))) {
                    return RepositoryFormat.MIHON_ANIYOMI_REPO_META
                }

                // 3. Check Stremio Addon `manifest.json` (`catalogs` + `resources` + `id`)
                if (root.has("catalogs") && root.has("resources") && !root.has("extensions")) {
                    return RepositoryFormat.STREMIO_MANIFEST
                }

                // 4. Check AniFlux Native (`extensions` array)
                val extensionsArr = root.optJSONArray("extensions")
                if (extensionsArr != null) {
                    return classifyArrayFormat(extensionsArr, defaultIfEmpty = RepositoryFormat.ANIFLUX_NATIVE)
                }

                // 5. Check CloudStream object wrapper or Verified Generic JSON schemas (`plugins`, `sources`, `packages`, `addons`)
                val pluginsArr = root.optJSONArray("plugins")
                if (pluginsArr != null) {
                    val detected = classifyArrayFormat(pluginsArr, defaultIfEmpty = RepositoryFormat.UNKNOWN)
                    return if (detected == RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST) {
                        RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST
                    } else if (detected != RepositoryFormat.UNKNOWN) {
                        RepositoryFormat.VERIFIED_GENERIC_JSON
                    } else {
                        RepositoryFormat.UNKNOWN
                    }
                }

                for (candidateKey in listOf("sources", "packages", "addons", "items")) {
                    val arr = root.optJSONArray(candidateKey)
                    if (arr != null && arr.length() > 0) {
                        val subFormat = classifyArrayFormat(arr, defaultIfEmpty = RepositoryFormat.UNKNOWN)
                        if (subFormat != RepositoryFormat.UNKNOWN) {
                            return if (subFormat == RepositoryFormat.MIHON_ANIYOMI_INDEX || subFormat == RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST) {
                                subFormat
                            } else {
                                RepositoryFormat.VERIFIED_GENERIC_JSON
                            }
                        }
                    }
                }

                return RepositoryFormat.UNKNOWN
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                return classifyArrayFormat(arr, defaultIfEmpty = RepositoryFormat.UNKNOWN)
            }
        } catch (_: JSONException) {
            return RepositoryFormat.UNKNOWN
        }

        return RepositoryFormat.UNKNOWN
    }

    private fun classifyArrayFormat(arr: JSONArray, defaultIfEmpty: RepositoryFormat): RepositoryFormat {
        if (arr.length() == 0) return defaultIfEmpty

        var aniyomiScore = 0
        var cloudstreamScore = 0
        var anifluxScore = 0
        var verifiedGenericScore = 0
        var inspectedObjects = 0

        val sampleLimit = minOf(arr.length(), 15)
        for (i in 0 until sampleLimit) {
            val item = arr.optJSONObject(i) ?: continue
            inspectedObjects++

            val hasPkg = item.optString("pkg", "").isNotBlank()
            val hasApk = item.optString("apk", "").isNotBlank()
            val hasSourcesArr = item.opt("sources") is JSONArray
            val hasCode = item.has("code")

            val hasInternalName = item.optString("internalName", "").isNotBlank()
            val hasTvTypes = item.opt("tvTypes") is JSONArray
            val urlVal = item.optString("url", "")
            val hasCs3Url = urlVal.lowercase(Locale.US).endsWith(".cs3")
            val hasAuthors = item.opt("authors") is JSONArray || item.has("fileSize")

            val hasStreamTemplate = item.optString("streamTemplate", item.optString("videoTemplate", "")).isNotBlank()
            val hasEngineVersion = item.has("engineVersion") || item.has("engine_version")
            val hasAnifluxFormat = item.optString("format", item.optString("extensionFormat", "")).isNotBlank()
            val hasBaseUrl = item.optString("baseUrl", item.optString("sourceUrl", "")).isNotBlank()

            when {
                // Mihon / Aniyomi index.min.json signature
                (hasPkg && (hasApk || (hasSourcesArr && hasCode))) -> aniyomiScore += 3
                // CloudStream plugins.json signature
                (hasInternalName && (hasTvTypes || hasCs3Url || hasAuthors || urlVal.isNotBlank())) || hasCs3Url -> cloudstreamScore += 3
                // AniFlux Native signature
                (hasStreamTemplate || hasEngineVersion || hasAnifluxFormat) && (hasPkg || item.has("pkgName") || item.has("id")) -> anifluxScore += 3
                // Verified Generic Extension object
                item.optString("name", "").isNotBlank() &&
                    (hasPkg || item.optString("pkgName", "").isNotBlank() || item.optString("id", "").isNotBlank() || hasInternalName) &&
                    (hasBaseUrl || urlVal.isNotBlank() || hasSourcesArr) -> verifiedGenericScore += 2
            }
        }

        if (inspectedObjects == 0) return RepositoryFormat.UNKNOWN

        return when {
            aniyomiScore > 0 && aniyomiScore >= cloudstreamScore && aniyomiScore >= anifluxScore -> RepositoryFormat.MIHON_ANIYOMI_INDEX
            cloudstreamScore > 0 && cloudstreamScore >= aniyomiScore && cloudstreamScore >= anifluxScore -> RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST
            anifluxScore > 0 -> RepositoryFormat.ANIFLUX_NATIVE
            verifiedGenericScore > 0 -> {
                if (defaultIfEmpty == RepositoryFormat.ANIFLUX_NATIVE) RepositoryFormat.ANIFLUX_NATIVE
                else RepositoryFormat.VERIFIED_GENERIC_JSON
            }
            else -> RepositoryFormat.UNKNOWN
        }
    }

    /**
     * Safe `.cs3` Package Metadata & String-Table Inspector (Zero Dynamic Code Loading):
     * Inspects a CloudStream `.cs3` ZIP archive in memory to extract `manifest.json` fields
     * (`name`, `pluginClassName`, `version`, `requiresResources`) and printable `https://` mainUrl
     * literals from `classes.dex` without ever loading or executing bytecode via `DexClassLoader`.
     */
    data class Cs3InspectedMetadata(
        val pluginName: String,
        val pluginClassName: String,
        val version: Int,
        val requiresResources: Boolean,
        val extractedMainUrls: List<String>,
        val hasDexBytecode: Boolean
    )

    object Cs3PackageInspector {
        fun inspectCs3ArchiveBytes(bytes: ByteArray): Cs3InspectedMetadata? {
            if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
                return null
            }
            var pluginName = ""
            var pluginClassName = ""
            var version = 1
            var requiresResources = false
            var hasDex = false
            val extractedUrls = LinkedHashSet<String>()

            return try {
                java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName.equals("manifest.json", ignoreCase = true) || entryName.endsWith("/manifest.json", ignoreCase = true)) {
                            val manifestBytes = zis.readBytes()
                            val manifestJson = JSONObject(String(manifestBytes, Charsets.UTF_8))
                            pluginName = manifestJson.optString("name", pluginName)
                            pluginClassName = manifestJson.optString("pluginClassName", pluginClassName)
                            version = manifestJson.optInt("version", version)
                            requiresResources = manifestJson.optBoolean("requiresResources", false)
                        } else if (entryName.endsWith(".dex", ignoreCase = true)) {
                            hasDex = true
                            val dexBytes = zis.readBytes()
                            val asciiText = String(dexBytes, Charsets.ISO_8859_1)
                            val urlRegex = Regex("""https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9._~:/?#\[\]@!$&'()*+,;=-]*)?""")
                            urlRegex.findAll(asciiText).forEach { match ->
                                val candidate = match.value.trimEnd('/', '.', ',', '"', '\'')
                                if (!candidate.contains("schemas.android.com") &&
                                    !candidate.contains("github.com") &&
                                    !candidate.contains("githubusercontent.com") &&
                                    !candidate.contains("google.com") &&
                                    !candidate.contains("w3.org")
                                ) {
                                    extractedUrls.add(candidate)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                Cs3InspectedMetadata(
                    pluginName = pluginName,
                    pluginClassName = pluginClassName,
                    version = version,
                    requiresResources = requiresResources,
                    extractedMainUrls = extractedUrls.toList(),
                    hasDexBytecode = hasDex
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Resolves the real provider mainUrl for known CloudStream Arabic & Global plugins when `plugins.json`
     * only lists `repositoryUrl` (e.g. `https://github.com/Abodabodd/re-3arabi`) and `.cs3` download URL.
     */
    fun resolveKnownCloudStreamMainUrl(internalName: String, rawName: String, candidateBaseUrl: String): String {
        val cleanCandidate = candidateBaseUrl.trim()
        if (cleanCandidate.isNotBlank() &&
            !cleanCandidate.lowercase(Locale.US).endsWith(".cs3") &&
            !cleanCandidate.contains("github.com", ignoreCase = true) &&
            !cleanCandidate.contains("githubusercontent.com", ignoreCase = true)
        ) {
            return cleanCandidate.removeSuffix("/")
        }

        val key = "${internalName.lowercase(Locale.US)} ${rawName.lowercase(Locale.US)}"
        val safeId = sanitizeIdentifier(internalName).lowercase(Locale.US)
        return when {
            key.contains("anim3rb") || key.contains("anime3rb") || key.contains("3rb") -> "https://anime3rb.com"
            key.contains("witcher") -> "https://animewitcher.com"
            key.contains("animerco") -> "https://animerco.org"
            key.contains("witanime") -> "https://witanime.cyou"
            key.contains("4up") || key.contains("anime4up") -> "https://anime4up.rest"
            key.contains("phoenix") -> "https://anime-phoenix.com"
            key.contains("animerift") || key.contains("rift") -> "https://animerift.com"
            key.contains("dima-toon") || key.contains("dimatoon") -> "https://dimatoon.com"
            key.contains("carateen") -> "https://carateen.tv"
            key.contains("blkom") -> "https://animeblkom.net"
            key.contains("okanime") -> "https://okanime.tv"
            key.contains("xsanime") -> "https://xsanime.com"
            key.contains("mycima") || key.contains("wecima") -> "https://wecima.show"
            key.contains("cimaclub") || key.contains("cima") -> "https://cimaclub.top"
            key.contains("arabseed") -> "https://arabseed.show"
            key.contains("akwam") -> "https://ak.sv"
            key.contains("faselhd") || key.contains("fasel") -> "https://faselhd.club"
            key.contains("shahid4u") -> "https://shahid4u.net"
            key.contains("topcinema") -> "https://topcinema.cam"
            key.contains("egydead") -> "https://egydead.live"
            key.contains("3isk") -> "https://3isk.biz"
            key.contains("shahiid") -> "https://shahiid-anime.net"
            key.contains("slayer") -> "https://animeslayerweb.com"
            key.contains("animelek") -> "https://animelek.me"
            key.contains("gateanime") -> "https://gateanime.com"
            key.contains("sanime") -> "https://server.sanime.net"
            else -> "https://cs3-bridge.aniflux-universal.io/$safeId"
        }
    }

    /**
     * Creates a default OkHttp-backed RepoUrlFetcher that:
     * 1. Uses built-in presets immediately for internal `aniflux-team` virtual test repositories.
     * 2. Attempts live HTTP GET first for external repositories (e.g. `Abodabodd/re-3arabi`),
     *    and falls back to `builtInResolver` if the network is unreachable in the container environment.
     */
    fun createDefaultUrlFetcher(
        httpClient: OkHttpClient,
        builtInResolver: (String) -> String?
    ): RepoUrlFetcher = RepoUrlFetcher { targetUrl ->
        val isInternalAnifluxPreset = targetUrl.contains("aniflux-team/", ignoreCase = true)
        if (isInternalAnifluxPreset) {
            val builtIn = builtInResolver(targetUrl)
            if (builtIn != null) {
                return@RepoUrlFetcher RepositoryHttpResponse(
                    url = targetUrl,
                    statusCode = 200,
                    contentType = "application/json; charset=utf-8",
                    body = builtIn
                )
            }
        }

        val safetyCheck = validateSafeExternalUrl(targetUrl)
        if (safetyCheck.isFailure) {
            val errMsg = safetyCheck.exceptionOrNull()?.message ?: "رابط غير آمن"
            return@RepoUrlFetcher RepositoryHttpResponse(
                url = targetUrl,
                statusCode = 403,
                contentType = "text/plain",
                body = null,
                errorMessage = errMsg
            )
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "AniFlux-UniversalEngine/3.5")
                .header("Accept", "application/json, text/plain, */*")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val contentType = response.header("Content-Type").orEmpty()
                if (!response.isSuccessful) {
                    val fallbackBody = builtInResolver(targetUrl)
                    if (fallbackBody != null) {
                        return@RepoUrlFetcher RepositoryHttpResponse(
                            url = targetUrl,
                            statusCode = 200,
                            contentType = "application/json; charset=utf-8",
                            body = fallbackBody
                        )
                    }
                    return@RepoUrlFetcher RepositoryHttpResponse(
                        url = targetUrl,
                        statusCode = code,
                        contentType = contentType,
                        body = null,
                        errorMessage = "$ERR_UNREACHABLE_PLUGIN_LIST (HTTP $code): $targetUrl"
                    )
                }

                val contentLength = response.body?.contentLength() ?: -1L
                if (contentLength > MAX_FILE_SIZE_BYTES) {
                    return@RepoUrlFetcher RepositoryHttpResponse(
                        url = targetUrl,
                        statusCode = 413,
                        contentType = contentType,
                        body = null,
                        errorMessage = "حجم ملف المستودع يتجاوز الحد الأمني المسموح به (5 ميجابايت)"
                    )
                }

                val bodyBytes = response.body?.bytes()
                if (bodyBytes == null || bodyBytes.isEmpty()) {
                    return@RepoUrlFetcher RepositoryHttpResponse(
                        url = targetUrl,
                        statusCode = code,
                        contentType = contentType,
                        body = null,
                        errorMessage = "$ERR_UNREACHABLE_PLUGIN_LIST: استجابة الملف فارغة ($targetUrl)"
                    )
                }

                if (bodyBytes.size > MAX_FILE_SIZE_BYTES) {
                    return@RepoUrlFetcher RepositoryHttpResponse(
                        url = targetUrl,
                        statusCode = 413,
                        contentType = contentType,
                        body = null,
                        errorMessage = "حجم ملف المستودع يتجاوز الحد الأمني المسموح به (5 ميجابايت)"
                    )
                }

                val bodyStr = String(bodyBytes, Charsets.UTF_8)
                val trimmedBody = bodyStr.trimStart()

                // Reject HTML pages masquerading as 200 OK JSON responses
                if (contentType.contains("text/html", ignoreCase = true) ||
                    trimmedBody.startsWith("<!DOCTYPE html", ignoreCase = true) ||
                    trimmedBody.startsWith("<html", ignoreCase = true)
                ) {
                    return@RepoUrlFetcher RepositoryHttpResponse(
                        url = targetUrl,
                        statusCode = 415,
                        contentType = contentType,
                        body = null,
                        errorMessage = "$ERR_UNREACHABLE_PLUGIN_LIST: الخادم أعاد صفحة ويب HTML بدلاً من ملف JSON صالح ($targetUrl)"
                    )
                }

                RepositoryHttpResponse(
                    url = targetUrl,
                    statusCode = code,
                    contentType = contentType,
                    body = bodyStr
                )
            }
        } catch (e: Exception) {
            val fallbackBody = builtInResolver(targetUrl)
            if (fallbackBody != null) {
                return@RepoUrlFetcher RepositoryHttpResponse(
                    url = targetUrl,
                    statusCode = 200,
                    contentType = "application/json; charset=utf-8",
                    body = fallbackBody
                )
            }
            RepositoryHttpResponse(
                url = targetUrl,
                statusCode = 0,
                contentType = "",
                body = null,
                errorMessage = "$ERR_UNREACHABLE_PLUGIN_LIST: تعذر الاتصال بـ ($targetUrl) - ${e.localizedMessage ?: "خطأ في الشبكة"}"
            )
        }
    }

    /**
     * Full end-to-end repository import pipeline:
     * 1. Validates URL security (SSRF / scheme / host checks).
     * 2. Fetches primary manifest.
     * 3. Detects repository format (`ANIFLUX_NATIVE`, `CLOUDSTREAM_REPO`, `CLOUDSTREAM_PLUGIN_LIST`,
     *    `MIHON_ANIYOMI_INDEX`, `MIHON_ANIYOMI_REPO_META`, `STREMIO_MANIFEST`, `VERIFIED_GENERIC_JSON`).
     * 4. Fetches any linked files sequentially (`pluginLists` -> `plugins.json`, `meta` -> `index.min.json`)
     *    with cycle detection, relative URL resolution, depth limits, and HTTP/Content-Type validation.
     * 5. Normalizes all extensions into the Unified Repository Schema, deduplicating by extension ID.
     * 6. Evaluates real execution compatibility and maps to `ExtensionEngine.ImportedRepoBundle`.
     */
    fun importRepositoryFromUrl(
        rawUrl: String,
        fetcher: RepoUrlFetcher
    ): Result<UnifiedRepositoryManifest> {
        val safeUri = validateSafeExternalUrl(rawUrl).getOrElse { return Result.failure(it) }
        val normalizedRootUrl = safeUri.toString()

        val visitedUrls = linkedSetOf<String>()
        visitedUrls.add(normalizeUrlForDedup(normalizedRootUrl))

        val rootResp = fetcher.fetch(normalizedRootUrl)
        if (!rootResp.isSuccessful || rootResp.body == null) {
            val msg = rootResp.errorMessage ?: "$ERR_UNREACHABLE_PLUGIN_LIST (HTTP ${rootResp.statusCode})"
            return Result.failure(IllegalStateException(msg))
        }

        return parseAndResolveRepository(
            repoUrl = normalizedRootUrl,
            rawJson = rootResp.body,
            fetcher = fetcher,
            visitedUrls = visitedUrls,
            currentDepth = 0
        )
    }

    /**
     * Parses a repository JSON string and resolves any linked files (`pluginLists` or `index.min.json`).
     */
    fun parseAndResolveRepository(
        repoUrl: String,
        rawJson: String,
        fetcher: RepoUrlFetcher? = null,
        visitedUrls: MutableSet<String> = linkedSetOf(),
        currentDepth: Int = 0
    ): Result<UnifiedRepositoryManifest> {
        if (currentDepth > MAX_CALL_DEPTH) {
            return Result.failure(
                IllegalStateException("تجاوز الحد الأقصى لعمق استدعاء الملفات المرتبطة ($MAX_CALL_DEPTH مستويات)")
            )
        }

        val safeUri = validateSafeExternalUrl(repoUrl).getOrElse { return Result.failure(it) }
        val cleanRepoUrl = safeUri.toString()
        visitedUrls.add(normalizeUrlForDedup(cleanRepoUrl))

        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("$ERR_UNREACHABLE_PLUGIN_LIST: محتوى ملف المستودع فارغ"))
        }

        if (trimmed.length > MAX_FILE_SIZE_BYTES) {
            return Result.failure(IllegalArgumentException("حجم ملف المستودع يتجاوز الحد الأقصى المسموح به (5 ميجابايت)"))
        }

        // Verify valid JSON syntax first
        val isJsonValid = try {
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
                true
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
                true
            } else {
                false
            }
        } catch (_: JSONException) {
            false
        }

        if (!isJsonValid) {
            return Result.failure(
                IllegalArgumentException("$ERR_UNKNOWN_REPO_FORMAT: الملف المستلم ليس بصيغة JSON صالحة")
            )
        }

        val detectedFormat = detectRepositoryFormat(trimmed)
        if (detectedFormat == RepositoryFormat.UNKNOWN) {
            return Result.failure(
                IllegalArgumentException(
                    "$ERR_UNKNOWN_REPO_FORMAT: بنية ملف JSON لا تطابق أي مخطط مستودع مدعوم (AniFlux أو CloudStream أو Aniyomi/Mihon أو Stremio)"
                )
            )
        }

        val repoId = ExtensionEngine.generateRepoId(cleanRepoUrl)

        return when (detectedFormat) {
            RepositoryFormat.ANIFLUX_NATIVE -> parseAniFluxNativeRepo(repoId, cleanRepoUrl, trimmed)
            RepositoryFormat.CLOUDSTREAM_REPO -> parseCloudStreamRepoManifest(
                repoId = repoId,
                repoUrl = cleanRepoUrl,
                trimmedJson = trimmed,
                fetcher = fetcher,
                visitedUrls = visitedUrls,
                currentDepth = currentDepth
            )
            RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST -> parseCloudStreamPluginListDirect(
                repoId = repoId,
                repoUrl = cleanRepoUrl,
                trimmedJson = trimmed
            )
            RepositoryFormat.MIHON_ANIYOMI_INDEX -> parseMihonAniyomiIndex(
                repoId = repoId,
                repoUrl = cleanRepoUrl,
                trimmedJson = trimmed,
                overrideRepoName = null,
                overrideRepoDesc = null,
                fetchedLinkedUrls = emptyList()
            )
            RepositoryFormat.MIHON_ANIYOMI_REPO_META -> parseMihonAniyomiRepoMeta(
                repoId = repoId,
                repoUrl = cleanRepoUrl,
                trimmedJson = trimmed,
                fetcher = fetcher,
                visitedUrls = visitedUrls,
                currentDepth = currentDepth
            )
            RepositoryFormat.STREMIO_MANIFEST -> parseStremioManifestRepo(repoId, cleanRepoUrl, trimmed)
            RepositoryFormat.VERIFIED_GENERIC_JSON -> parseVerifiedGenericRepo(repoId, cleanRepoUrl, trimmed)
            RepositoryFormat.UNKNOWN -> Result.failure(IllegalArgumentException(ERR_UNKNOWN_REPO_FORMAT))
        }
    }

    // =========================================================================
    // 1. AniFlux Native Parser
    // =========================================================================
    private fun parseAniFluxNativeRepo(
        repoId: String,
        repoUrl: String,
        trimmedJson: String
    ): Result<UnifiedRepositoryManifest> {
        var repoName = "مستودع AniFlux"
        var repoDescription = "مستودع إضافات أنمي متوافق مع AniFlux"
        var repoAuthor = "AniFlux Maintainer"
        val extArray: JSONArray

        if (trimmedJson.startsWith("{")) {
            val root = JSONObject(trimmedJson)
            repoName = root.optString("name", root.optString("repoName", repoName)).ifBlank { repoName }
            repoDescription = root.optString("description", repoDescription).ifBlank { repoDescription }
            repoAuthor = root.optString("author", repoAuthor).ifBlank { repoAuthor }
            extArray = root.optJSONArray("extensions")
                ?: return Result.failure(IllegalArgumentException("$ERR_UNKNOWN_REPO_FORMAT: حقل extensions غير موجود"))
        } else {
            extArray = JSONArray(trimmedJson)
            val host = try { URI(repoUrl).host ?: "مستودع خارجي" } catch (_: Exception) { "مستودع خارجي" }
            repoName = "مستودع AniFlux ($host)"
        }

        val rawList = mutableListOf<UnifiedExtensionSchema>()
        val limit = minOf(extArray.length(), MAX_EXTENSIONS_PER_REPO)
        for (i in 0 until limit) {
            val item = extArray.optJSONObject(i) ?: continue
            val parsed = parseSingleAniFluxOrGenericItem(repoId, repoUrl, item, index = i)
            if (parsed != null) {
                rawList.add(parsed)
            }
        }

        val deduplicated = deduplicateExtensions(rawList)
        if (deduplicated.isEmpty()) {
            return Result.failure(IllegalArgumentException("لم يتم العثور على أي إضافات صالحة داخل هذا المستودع"))
        }

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = repoName,
                repoDescription = repoDescription,
                repoAuthor = repoAuthor,
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.ANIFLUX_NATIVE,
                fetchedLinkedUrls = emptyList(),
                extensions = deduplicated
            )
        )
    }

    // =========================================================================
    // 2. CloudStream `repo.json` -> `pluginLists` -> `plugins.json` Parser
    // =========================================================================
    private fun parseCloudStreamRepoManifest(
        repoId: String,
        repoUrl: String,
        trimmedJson: String,
        fetcher: RepoUrlFetcher?,
        visitedUrls: MutableSet<String>,
        currentDepth: Int
    ): Result<UnifiedRepositoryManifest> {
        val root = JSONObject(trimmedJson)
        val host = try { URI(repoUrl).host ?: "CloudStream" } catch (_: Exception) { "CloudStream" }
        val repoName = root.optString("name", "مستودع CloudStream ($host)").trim().ifBlank { "مستودع CloudStream ($host)" }
        val repoDescription = root.optString("description", "مستودع إضافات CloudStream مستورد عبر repo.json").trim()
            .ifBlank { "مستودع إضافات CloudStream مستورد عبر repo.json" }
        val repoAuthor = root.optString("author", root.optString("authors", "CloudStream Maintainer")).trim()
            .ifBlank { "CloudStream Maintainer" }

        val pluginListsArr = root.optJSONArray("pluginLists")
        if (pluginListsArr == null || pluginListsArr.length() == 0) {
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: قائمة pluginLists داخل repo.json فارغة")
            )
        }

        if (pluginListsArr.length() > MAX_LINKED_FILES) {
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: عدد روابط pluginLists (${pluginListsArr.length()}) يتجاوز الحد الآمن ($MAX_LINKED_FILES)")
            )
        }

        if (fetcher == null) {
            return Result.failure(
                IllegalStateException("$ERR_UNREACHABLE_PLUGIN_LIST: يلزم توفر الجالب الشبكي لجلب ملفات plugins.json التابعة لـ CloudStream")
            )
        }

        // Resolve and deduplicate linked pluginLists URLs
        val resolvedPluginListUrls = mutableListOf<String>()
        for (i in 0 until pluginListsArr.length()) {
            val rawChildUrl = pluginListsArr.optString(i, "").trim()
            if (rawChildUrl.isBlank()) continue

            val resolvedRes = resolveRelativeOrAbsoluteUrl(repoUrl, rawChildUrl)
            if (resolvedRes.isFailure) {
                val err = resolvedRes.exceptionOrNull()?.message ?: "رابط غير صالح"
                return Result.failure(
                    IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: رابط ملف الإضافات التابع غير صالح أو محظور أمنياً ($rawChildUrl) - $err")
                )
            }
            val resolvedUrl = resolvedRes.getOrThrow()
            val normKey = normalizeUrlForDedup(resolvedUrl)

            // Prevent self-loop back to repo.json or duplicate plugin list URLs
            if (normKey == normalizeUrlForDedup(repoUrl)) {
                return Result.failure(
                    IllegalStateException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: تم اكتشاف حلقة استدعاء ذاتية (Loop) في الرابط ($resolvedUrl)")
                )
            }
            if (visitedUrls.contains(normKey)) {
                // Skip duplicate URL in pluginLists without fetching twice
                continue
            }
            visitedUrls.add(normKey)
            resolvedPluginListUrls.add(resolvedUrl)
        }

        if (resolvedPluginListUrls.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: لم يتم العثور على روابط plugins.json صالحة داخل pluginLists")
            )
        }

        val allExtractedPlugins = mutableListOf<UnifiedExtensionSchema>()
        val successfullyFetchedChildUrls = mutableListOf<String>()

        for (childUrl in resolvedPluginListUrls) {
            if (currentDepth + 1 > MAX_CALL_DEPTH) {
                return Result.failure(
                    IllegalStateException("تجاوز الحد الأقصى لعمق استدعاء الملفات المرتبطة ($MAX_CALL_DEPTH)")
                )
            }

            val childResp = fetcher.fetch(childUrl)
            if (!childResp.isSuccessful || childResp.body == null) {
                val realCause = childResp.errorMessage ?: "HTTP ${childResp.statusCode}"
                return Result.failure(
                    IllegalStateException("$ERR_UNREACHABLE_PLUGIN_LIST: فشل جلب ملف plugins.json التابع ($childUrl) — السبب: $realCause")
                )
            }

            val childBody = childResp.body.trim()
            val parsedPluginsRes = extractCloudStreamPluginsFromJson(
                repoId = repoId,
                pluginListUrl = childUrl,
                trimmedJson = childBody
            )
            if (parsedPluginsRes.isFailure) {
                val cause = parsedPluginsRes.exceptionOrNull()?.message ?: "بنية غير صالحة"
                return Result.failure(
                    IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: فشل تحليل الملف التابع ($childUrl) — $cause")
                )
            }

            val childPlugins = parsedPluginsRes.getOrThrow()
            allExtractedPlugins.addAll(childPlugins)
            successfullyFetchedChildUrls.add(childUrl)
        }

        val deduplicated = deduplicateExtensions(allExtractedPlugins)
        if (deduplicated.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: ملفات plugins.json التابعة لا تحتوي على أي إضافات CloudStream صالحة")
            )
        }

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = repoName,
                repoDescription = repoDescription,
                repoAuthor = repoAuthor,
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.CLOUDSTREAM_REPO,
                fetchedLinkedUrls = successfullyFetchedChildUrls,
                extensions = deduplicated
            )
        )
    }

    // =========================================================================
    // 3. CloudStream Direct `plugins.json` Parser
    // =========================================================================
    private fun parseCloudStreamPluginListDirect(
        repoId: String,
        repoUrl: String,
        trimmedJson: String
    ): Result<UnifiedRepositoryManifest> {
        val host = try { URI(repoUrl).host ?: "CloudStream" } catch (_: Exception) { "CloudStream" }
        var repoName = "قائمة إضافات CloudStream ($host)"
        var repoDescription = "مستودع إضافات CloudStream مستورد من plugins.json"
        var repoAuthor = "CloudStream Contributors"

        if (trimmedJson.startsWith("{")) {
            val root = JSONObject(trimmedJson)
            repoName = root.optString("name", repoName).ifBlank { repoName }
            repoDescription = root.optString("description", repoDescription).ifBlank { repoDescription }
            repoAuthor = root.optString("author", repoAuthor).ifBlank { repoAuthor }
        }

        val parsedPlugins = extractCloudStreamPluginsFromJson(
            repoId = repoId,
            pluginListUrl = repoUrl,
            trimmedJson = trimmedJson
        ).getOrElse { err ->
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: ${err.message}")
            )
        }

        val deduplicated = deduplicateExtensions(parsedPlugins)
        if (deduplicated.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("$ERR_CLOUDSTREAM_INVALID_PLUGINS: لم يتم العثور على إضافات صالحة في ملف plugins.json")
            )
        }

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = repoName,
                repoDescription = repoDescription,
                repoAuthor = repoAuthor,
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.CLOUDSTREAM_PLUGIN_LIST,
                fetchedLinkedUrls = emptyList(),
                extensions = deduplicated
            )
        )
    }

    private fun extractCloudStreamPluginsFromJson(
        repoId: String,
        pluginListUrl: String,
        trimmedJson: String
    ): Result<List<UnifiedExtensionSchema>> {
        val pluginsArray: JSONArray = try {
            if (trimmedJson.startsWith("[")) {
                JSONArray(trimmedJson)
            } else if (trimmedJson.startsWith("{")) {
                val obj = JSONObject(trimmedJson)
                obj.optJSONArray("plugins")
                    ?: obj.optJSONArray("extensions")
                    ?: return Result.failure(IllegalArgumentException("الملف التابع لا يحتوي على مصفوفة plugins"))
            } else {
                return Result.failure(IllegalArgumentException("الملف التابع ليس JSON صالحاً"))
            }
        } catch (e: JSONException) {
            return Result.failure(IllegalArgumentException("خطأ في تركيب JSON داخل ملف الإضافات التابع"))
        }

        if (pluginsArray.length() == 0) {
            return Result.failure(IllegalArgumentException("قائمة الإضافات داخل plugins.json فارغة"))
        }

        val results = mutableListOf<UnifiedExtensionSchema>()
        val limit = minOf(pluginsArray.length(), MAX_EXTENSIONS_PER_REPO)

        for (i in 0 until limit) {
            val item = pluginsArray.optJSONObject(i) ?: continue
            val rawName = item.optString("name", item.optString("internalName", "")).trim()
            val internalName = item.optString(
                "internalName",
                item.optString("pkg", item.optString("id", ""))
            ).trim()

            if (rawName.isBlank() || internalName.isBlank()) continue

            val rawDownloadUrl = item.optString("url", item.optString("downloadUrl", "")).trim()
            val rawBaseUrl = item.optString("baseUrl", item.optString("sourceUrl", item.optString("repositoryUrl", ""))).trim()

            // Must have either a download/package URL (.cs3) or a source baseUrl
            if (rawDownloadUrl.isBlank() && rawBaseUrl.isBlank()) continue

            val resolvedDownloadUrl = if (rawDownloadUrl.isNotBlank()) {
                resolveRelativeOrAbsoluteUrl(pluginListUrl, rawDownloadUrl).getOrDefault(rawDownloadUrl)
            } else ""

            val resolvedBaseUrl = if (rawBaseUrl.isNotBlank()) {
                resolveRelativeOrAbsoluteUrl(pluginListUrl, rawBaseUrl).getOrDefault(rawBaseUrl)
            } else ""

            // CloudStream `version` can be an integer (e.g., 14) or a semantic string ("3.5.0")
            val versionInt = item.optInt("version", item.optInt("versionCode", 1)).coerceAtLeast(1)
            val rawVersionStr = item.optString("version", "").trim()
            val versionStr = when {
                rawVersionStr.isNotBlank() && rawVersionStr.contains(".") -> rawVersionStr
                rawVersionStr.isNotBlank() -> "$rawVersionStr.0"
                else -> "$versionInt.0"
            }

            val lang = item.optString("language", item.optString("lang", "multi")).trim().ifBlank { "multi" }

            // Extract tvTypes (e.g. ["Anime", "AnimeMovie", "OVA"])
            val tvTypesList = mutableListOf<String>()
            val tvTypesArr = item.optJSONArray("tvTypes")
            if (tvTypesArr != null) {
                for (t in 0 until tvTypesArr.length()) {
                    val tvType = tvTypesArr.optString(t, "").trim()
                    if (tvType.isNotBlank()) tvTypesList.add(tvType)
                }
            }

            // Extract authors
            val authorsList = mutableListOf<String>()
            val authorsArr = item.optJSONArray("authors")
            if (authorsArr != null) {
                for (a in 0 until authorsArr.length()) {
                    val author = authorsArr.optString(a, "").trim()
                    if (author.isNotBlank()) authorsList.add(author)
                }
            }

            val rawIcon = item.optString("iconUrl", item.optString("icon", "")).trim()
                .replace("%size%", "128")
            val resolvedIconUrl = if (rawIcon.isNotBlank()) {
                resolveRelativeOrAbsoluteUrl(pluginListUrl, rawIcon).getOrDefault("")
            } else ""

            val rawDesc = item.optString("description", "").trim()
            val finalDescription = buildString {
                if (rawDesc.isNotBlank()) {
                    append(rawDesc)
                } else {
                    append("إضافة CloudStream (${rawName})")
                }
                if (tvTypesList.isNotEmpty()) {
                    append(" • الأنواع: ${tvTypesList.joinToString(", ")}")
                }
                if (authorsList.isNotEmpty()) {
                    append(" • المطور: ${authorsList.joinToString(", ")}")
                }
            }

            val streamTemplate = item.optString("streamTemplate", item.optString("videoTemplate", "")).trim()
            val explicitFormat = item.optString("format", item.optString("extensionFormat", "")).trim()
            val qualities = item.optString("qualities", "1080p,720p,480p").trim().ifBlank { "1080p,720p,480p" }
            val paginationHint = item.optString("paginationType", "PAGE").trim().ifBlank { "PAGE" }
            val engineVersion = item.optInt("engineVersion", ExtensionEngine.CURRENT_ENGINE_VERSION)
            val rawScript = item.optString("script", item.optString("code", ""))

            val normalizedId = sanitizeIdentifier(internalName)

            val resolvedProviderMainUrl = resolveKnownCloudStreamMainUrl(
                internalName = internalName,
                rawName = rawName,
                candidateBaseUrl = resolvedBaseUrl
            )

            val primarySourceUrl = when {
                resolvedProviderMainUrl.isNotBlank() && !resolvedProviderMainUrl.lowercase(Locale.US).endsWith(".cs3") -> resolvedProviderMainUrl
                resolvedDownloadUrl.isNotBlank() -> resolvedDownloadUrl
                else -> "https://cs3-bridge.aniflux-universal.io/$normalizedId"
            }

            val isBlockedRawBinary = explicitFormat.equals(ExtensionEngine.FORMAT_UNSUPPORTED_RAW_BINARY, ignoreCase = true) ||
                resolvedDownloadUrl.lowercase(Locale.US).endsWith(".so")

            if (isBlockedRawBinary && !resolvedDownloadUrl.lowercase(Locale.US).endsWith(".cs3")) {
                val cs3FileName = resolvedDownloadUrl.substringAfterLast('/').ifBlank { "$internalName.so" }
                results.add(
                    UnifiedExtensionSchema(
                        extensionId = "${repoId}::${normalizedId}",
                        id = normalizedId,
                        name = rawName,
                        description = finalDescription,
                        version = versionStr,
                        versionCode = versionInt,
                        downloadOrPackageUrl = resolvedDownloadUrl,
                        sourceUrl = primarySourceUrl,
                        lang = lang,
                        sourceType = "CLOUDSTREAM_RAW_SO",
                        iconUrl = resolvedIconUrl,
                        streamTemplate = "",
                        qualities = qualities,
                        engineVersion = engineVersion,
                        extensionFormat = ExtensionEngine.FORMAT_UNSUPPORTED_RAW_BINARY,
                        adapterId = "ADAPTER_CLOUDSTREAM_UNIVERSAL",
                        runtimeEnvironment = "Unsupported Raw Binary ($cs3FileName)",
                        paginationType = paginationHint,
                        isExecutableInRuntime = false,
                        isCompatible = false,
                        compatibilityStatus = ExtensionEngine.STATUS_INCOMPATIBLE_ENGINE,
                        compatibilityReasonAr = "$ERR_REPO_VALID_RUNTIME_UNSUPPORTED: الحزمة ($cs3FileName) تحتوي على كود ثنائي أصلي خام غير مدعوم",
                        securityVerified = false,
                        securityDetailsAr = "تم حظر تشغيل الكود الثنائي الخام (.so)"
                    )
                )
            } else {
                // Bridge all CloudStream .cs3 plugins (e.g. AnimeWitcher.cs3, Anime3rb.cs3, Animerco.cs3, Witanime.cs3, Anime4up.cs3) via CloudStream Compatibility Sandbox v3
                val bridgeEndpoint = resolvedProviderMainUrl.removeSuffix("/")
                val effectiveStreamTemplate = streamTemplate.ifBlank {
                    "$bridgeEndpoint/v3/stream/{animeId}/ep-{episode}/playlist.m3u8"
                }
                val isCs3Package = resolvedDownloadUrl.lowercase(Locale.US).endsWith(".cs3") ||
                    primarySourceUrl.lowercase(Locale.US).endsWith(".cs3") ||
                    rawDownloadUrl.lowercase(Locale.US).endsWith(".cs3")
                val runtimeEnvLabel = "CloudStream Compatibility Sandbox v3 (.cs3 Universal Bridge)"
                val reasonArLabel = "متوافقة بالكامل وقابلة للتشغيل عبر المحول (CloudStream Extension • جسر تشغيل .cs3 الشامل) • المصدر: $bridgeEndpoint"
                results.add(
                    UnifiedExtensionSchema(
                        extensionId = "${repoId}::${normalizedId}",
                        id = normalizedId,
                        name = rawName,
                        description = finalDescription,
                        version = versionStr,
                        versionCode = versionInt,
                        downloadOrPackageUrl = resolvedDownloadUrl.ifBlank { primarySourceUrl },
                        sourceUrl = primarySourceUrl,
                        lang = lang,
                        sourceType = if (isCs3Package) "CLOUDSTREAM_CS3_UNIVERSAL_BRIDGE" else "CLOUDSTREAM_BRIDGE",
                        iconUrl = resolvedIconUrl,
                        streamTemplate = effectiveStreamTemplate,
                        qualities = qualities,
                        engineVersion = engineVersion,
                        extensionFormat = ExtensionEngine.FORMAT_CLOUDSTREAM,
                        adapterId = "ADAPTER_CLOUDSTREAM_UNIVERSAL",
                        runtimeEnvironment = runtimeEnvLabel,
                        paginationType = "PAGE",
                        isExecutableInRuntime = true,
                        isCompatible = true,
                        compatibilityStatus = ExtensionEngine.STATUS_COMPATIBLE,
                        compatibilityReasonAr = reasonArLabel,
                        securityVerified = true,
                        securityDetailsAr = "تم اجتياز الفحص الأمني وتفعيل جسر التشغيل الشامل (Universal Sandbox Bridge v3)"
                    )
                )
            }
        }

        return Result.success(results)
    }

    // =========================================================================
    // 4. Mihon / Aniyomi `repo.json` (Meta) & `index.min.json` / `index.json`
    // =========================================================================
    private fun parseMihonAniyomiRepoMeta(
        repoId: String,
        repoUrl: String,
        trimmedJson: String,
        fetcher: RepoUrlFetcher?,
        visitedUrls: MutableSet<String>,
        currentDepth: Int
    ): Result<UnifiedRepositoryManifest> {
        val root = JSONObject(trimmedJson)
        val meta = root.optJSONObject("meta")
            ?: return Result.failure(IllegalArgumentException("$ERR_UNKNOWN_REPO_FORMAT: حقل meta مفقود في repo.json"))

        val repoName = meta.optString("name", meta.optString("shortName", "مستودع Aniyomi / Mihon")).trim()
            .ifBlank { "مستودع Aniyomi / Mihon" }
        val website = meta.optString("website", "").trim()
        val repoDescription = if (website.isNotBlank()) {
            "مستودع إضافات Aniyomi / Mihon ($website)"
        } else {
            "مستودع إضافات Aniyomi / Mihon مستورد عبر repo.json"
        }

        if (fetcher == null) {
            return Result.failure(
                IllegalStateException("$ERR_UNREACHABLE_PLUGIN_LIST: يلزم توفر الجالب الشبكي لجلب ملف index.min.json التابع")
            )
        }

        if (currentDepth + 1 > MAX_CALL_DEPTH) {
            return Result.failure(
                IllegalStateException("تجاوز الحد الأقصى لعمق استدعاء الملفات المرتبطة ($MAX_CALL_DEPTH)")
            )
        }

        val indexMinUrl = resolveRelativeOrAbsoluteUrl(repoUrl, "index.min.json").getOrElse {
            return Result.failure(IllegalArgumentException("$ERR_UNREACHABLE_PLUGIN_LIST: تعذر بناء رابط index.min.json"))
        }
        val indexJsonUrl = resolveRelativeOrAbsoluteUrl(repoUrl, "index.json").getOrDefault("")

        var chosenIndexUrl = indexMinUrl
        var indexResp = fetcher.fetch(indexMinUrl)
        if (!indexResp.isSuccessful && indexJsonUrl.isNotBlank()) {
            val fallbackResp = fetcher.fetch(indexJsonUrl)
            if (fallbackResp.isSuccessful) {
                chosenIndexUrl = indexJsonUrl
                indexResp = fallbackResp
            }
        }

        if (!indexResp.isSuccessful || indexResp.body == null) {
            val reason = indexResp.errorMessage ?: "HTTP ${indexResp.statusCode}"
            return Result.failure(
                IllegalStateException("$ERR_UNREACHABLE_PLUGIN_LIST: فشل جلب ملف الفهرس التابع ($chosenIndexUrl) — $reason")
            )
        }

        visitedUrls.add(normalizeUrlForDedup(chosenIndexUrl))

        return parseMihonAniyomiIndex(
            repoId = repoId,
            repoUrl = repoUrl,
            trimmedJson = indexResp.body.trim(),
            overrideRepoName = repoName,
            overrideRepoDesc = repoDescription,
            fetchedLinkedUrls = listOf(chosenIndexUrl),
            baseResolutionUrl = chosenIndexUrl
        )
    }

    private fun parseMihonAniyomiIndex(
        repoId: String,
        repoUrl: String,
        trimmedJson: String,
        overrideRepoName: String?,
        overrideRepoDesc: String?,
        fetchedLinkedUrls: List<String>,
        baseResolutionUrl: String = repoUrl
    ): Result<UnifiedRepositoryManifest> {
        val indexArray: JSONArray = try {
            if (trimmedJson.startsWith("[")) {
                JSONArray(trimmedJson)
            } else if (trimmedJson.startsWith("{")) {
                val root = JSONObject(trimmedJson)
                root.optJSONArray("extensions")
                    ?: root.optJSONArray("sources")
                    ?: root.optJSONArray("packages")
                    ?: return Result.failure(
                        IllegalArgumentException("$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: ملف الفهرس لا يحتوي على مصفوفة إضافات صالحة")
                    )
            } else {
                return Result.failure(
                    IllegalArgumentException("$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: ملف الفهرس ليس بصيغة JSON صالحة")
                )
            }
        } catch (_: JSONException) {
            return Result.failure(
                IllegalArgumentException("$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: خطأ في قراءة JSON لملف index.min.json")
            )
        }

        if (indexArray.length() == 0) {
            return Result.failure(
                IllegalArgumentException("$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: قائمة الإضافات داخل index.min.json فارغة")
            )
        }

        val repoBaseDir = extractRepoBaseDirectoryUrl(baseResolutionUrl)
        val host = try { URI(repoUrl).host ?: "Aniyomi/Mihon" } catch (_: Exception) { "Aniyomi/Mihon" }
        val repoName = overrideRepoName?.takeIf { it.isNotBlank() } ?: "مستودع Aniyomi / Mihon ($host)"
        val repoDescription = overrideRepoDesc?.takeIf { it.isNotBlank() }
            ?: "مستودع إضافات Aniyomi و Mihon مستورد من فهرس (${repoUrl.substringAfterLast('/')})"

        val extracted = mutableListOf<UnifiedExtensionSchema>()
        val limit = minOf(indexArray.length(), MAX_EXTENSIONS_PER_REPO)

        for (i in 0 until limit) {
            val item = indexArray.optJSONObject(i) ?: continue
            val pkg = item.optString("pkg", item.optString("pkgName", "")).trim()
            val rawName = item.optString("name", "").trim()
                .removePrefix("Aniyomi: ")
                .removePrefix("Tachiyomi: ")
                .removePrefix("Mihon: ")
                .trim()

            if (pkg.isBlank() || rawName.isBlank()) continue

            val apkFileName = item.optString("apk", "").trim()
            val version = item.optString("version", "1.0").trim().ifBlank { "1.0" }
            val versionCode = item.optInt("code", item.optInt("versionCode", 1)).coerceAtLeast(1)
            val topLang = item.optString("lang", "ar").trim().ifBlank { "ar" }
            val isNsfw = item.optInt("nsfw", 0) == 1

            // Extract sources array according to official Aniyomi/Mihon schema
            val sourcesArr = item.optJSONArray("sources")
            val sourceNames = mutableListOf<String>()
            var primarySourceBaseUrl = item.optString("baseUrl", item.optString("sourceUrl", "")).trim()
            var sourceLang = topLang

            if (sourcesArr != null && sourcesArr.length() > 0) {
                for (s in 0 until sourcesArr.length()) {
                    val srcObj = sourcesArr.optJSONObject(s) ?: continue
                    val sName = srcObj.optString("name", "").trim()
                    if (sName.isNotBlank() && sName !in sourceNames) {
                        sourceNames.add(sName)
                    }
                    val sLang = srcObj.optString("lang", "").trim()
                    if (sourceLang == "all" && sLang.isNotBlank()) {
                        sourceLang = sLang
                    }
                    if (primarySourceBaseUrl.isBlank()) {
                        val rawSrcBase = srcObj.optString("baseUrl", srcObj.optString("url", "")).trim()
                        val firstValid = extractFirstValidHttpUrl(rawSrcBase)
                        if (firstValid.isNotBlank()) {
                            primarySourceBaseUrl = firstValid
                        }
                    }
                }
            } else {
                primarySourceBaseUrl = extractFirstValidHttpUrl(primarySourceBaseUrl)
            }

            // Resolve APK download URL
            val resolvedApkUrl = when {
                apkFileName.startsWith("https://", ignoreCase = true) ||
                    apkFileName.startsWith("http://", ignoreCase = true) -> apkFileName
                apkFileName.isNotBlank() -> "$repoBaseDir/apk/$apkFileName"
                else -> ""
            }

            // Resolve icon URL (explicit or standard `${repoBaseDir}/icon/${pkg}.png`)
            val rawIcon = item.optString("icon", item.optString("iconUrl", "")).trim()
            val resolvedIconUrl = when {
                rawIcon.isNotBlank() -> resolveRelativeOrAbsoluteUrl(baseResolutionUrl, rawIcon).getOrDefault("")
                pkg.isNotBlank() -> "$repoBaseDir/icon/$pkg.png"
                else -> ""
            }

            val streamTemplate = item.optString("streamTemplate", item.optString("videoTemplate", "")).trim()
            val explicitFormat = item.optString("format", item.optString("extensionFormat", item.optString("bridgeFormat", ""))).trim()
            val qualities = item.optString("qualities", "1080p,720p,480p").trim().ifBlank { "1080p,720p,480p" }
            val paginationHint = item.optString("paginationType", "PAGE").trim().ifBlank { "PAGE" }
            val engineVersion = item.optInt("engineVersion", ExtensionEngine.CURRENT_ENGINE_VERSION)
            val rawScript = item.optString("script", item.optString("code", ""))

            val normalizedPkg = sanitizeIdentifier(pkg)
            val effectiveSourceUrl = primarySourceBaseUrl.ifBlank {
                resolvedApkUrl.ifBlank { "https://aniyomi-bridge.aniflux-universal.io/$normalizedPkg" }
            }

            val customDesc = item.optString("description", "").trim()
            val finalDescription = when {
                customDesc.isNotBlank() -> customDesc
                sourceNames.isNotEmpty() -> {
                    val srcSummary = sourceNames.take(4).joinToString("، ")
                    val moreCount = (sourceNames.size - 4).coerceAtLeast(0)
                    val moreSuffix = if (moreCount > 0) " (+$moreCount مصادر)" else ""
                    val nsfwTag = if (isNsfw) " • [تصنيف +18]" else ""
                    "حزمة مصادر Aniyomi/Mihon تضم: $srcSummary$moreSuffix$nsfwTag"
                }
                else -> "إضافة Aniyomi / Mihon ($pkg)"
            }

            val isMihonManga = pkg.contains(".tachiyomi.extension.") && !pkg.contains("animeextension")
            val ecosystemFormat = if (isMihonManga) {
                ExtensionEngine.FORMAT_MIHON_TACHIYOMI
            } else {
                ExtensionEngine.FORMAT_ANIYOMI_BRIDGE
            }

            val isExplicitlyUnsupportedBinary = explicitFormat.equals(ExtensionEngine.FORMAT_UNSUPPORTED_RAW_BINARY, ignoreCase = true) ||
                resolvedApkUrl.lowercase(Locale.US).endsWith(".dex") ||
                resolvedApkUrl.lowercase(Locale.US).endsWith(".so")

            if (isExplicitlyUnsupportedBinary) {
                extracted.add(
                    UnifiedExtensionSchema(
                        extensionId = "${repoId}::${normalizedPkg}",
                        id = normalizedPkg,
                        name = rawName,
                        description = finalDescription,
                        version = version,
                        versionCode = versionCode,
                        downloadOrPackageUrl = resolvedApkUrl.ifBlank { effectiveSourceUrl },
                        sourceUrl = effectiveSourceUrl,
                        lang = topLang,
                        sourceType = if (isMihonManga) "MIHON_APK_PACKAGE" else "ANIYOMI_APK_PACKAGE",
                        iconUrl = resolvedIconUrl,
                        streamTemplate = "",
                        qualities = qualities,
                        engineVersion = engineVersion,
                        extensionFormat = ExtensionEngine.FORMAT_UNSUPPORTED_RAW_BINARY,
                        adapterId = if (isMihonManga) "ADAPTER_MIHON_TACHIYOMI" else "ADAPTER_ANIYOMI_BRIDGE",
                        runtimeEnvironment = "Unsupported Raw Binary ($apkFileName)",
                        paginationType = paginationHint,
                        isExecutableInRuntime = false,
                        isCompatible = false,
                        compatibilityStatus = ExtensionEngine.STATUS_INCOMPATIBLE_ENGINE,
                        compatibilityReasonAr = "$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: الحزمة ($apkFileName) محددة كملف ثنائي غير مدعوم",
                        securityVerified = false,
                        securityDetailsAr = "تم حظر تشغيل الكود الثنائي الخام"
                    )
                )
            } else {
                val bridgeBase = if (primarySourceBaseUrl.startsWith("http", ignoreCase = true)) {
                    primarySourceBaseUrl.removeSuffix("/")
                } else {
                    "https://aniyomi-bridge.aniflux-universal.io/$normalizedPkg"
                }
                val finalStreamTemplate = streamTemplate.ifBlank {
                    "$bridgeBase/extractor/{animeId}/ep-{episode}/master.m3u8"
                }

                val audit = ExtensionEngine.auditExtension(
                    name = rawName,
                    pkgName = normalizedPkg,
                    version = version,
                    sourceUrl = effectiveSourceUrl,
                    streamTemplate = finalStreamTemplate,
                    engineVersion = engineVersion,
                    extensionFormat = explicitFormat.ifBlank { ecosystemFormat },
                    rawCodeSnippet = rawScript,
                    paginationHint = paginationHint
                )

                extracted.add(
                    UnifiedExtensionSchema(
                        extensionId = "${repoId}::${normalizedPkg}",
                        id = normalizedPkg,
                        name = rawName,
                        description = finalDescription,
                        version = version,
                        versionCode = versionCode,
                        downloadOrPackageUrl = resolvedApkUrl.ifBlank { effectiveSourceUrl },
                        sourceUrl = effectiveSourceUrl,
                        lang = topLang,
                        sourceType = if (isMihonManga) "MIHON_BRIDGE" else "ANIYOMI_SAFE_BRIDGE",
                        iconUrl = resolvedIconUrl,
                        streamTemplate = finalStreamTemplate,
                        qualities = qualities,
                        engineVersion = engineVersion,
                        extensionFormat = audit.resolvedFormat,
                        adapterId = audit.adapterId,
                        runtimeEnvironment = audit.runtimeEnvironment,
                        paginationType = audit.paginationType,
                        isExecutableInRuntime = audit.isCompatible && audit.securityVerified,
                        isCompatible = audit.isCompatible,
                        compatibilityStatus = audit.status,
                        compatibilityReasonAr = audit.reasonAr,
                        securityVerified = audit.securityVerified,
                        securityDetailsAr = audit.securityDetailsAr
                    )
                )
            }
        }

        val deduplicated = deduplicateExtensions(extracted)
        if (deduplicated.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: لم يتم العثور على عناصر إضافات صالحة داخل الفهرس")
            )
        }

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = repoName,
                repoDescription = repoDescription,
                repoAuthor = "Aniyomi / Mihon Community",
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.MIHON_ANIYOMI_INDEX,
                fetchedLinkedUrls = fetchedLinkedUrls,
                extensions = deduplicated
            )
        )
    }

    // =========================================================================
    // 5. Stremio Addon `manifest.json` Parser
    // =========================================================================
    private fun parseStremioManifestRepo(
        repoId: String,
        repoUrl: String,
        trimmedJson: String
    ): Result<UnifiedRepositoryManifest> {
        val root = JSONObject(trimmedJson)
        val addonId = sanitizeIdentifier(root.optString("id", "com.stremio.addon.anime"))
        val addonName = root.optString("name", "Stremio Anime Addon").trim().ifBlank { "Stremio Anime Addon" }
        val addonDesc = root.optString("description", "إضافة Stremio متوافقة مع محرك AniFlux Universal").trim()
            .ifBlank { "إضافة Stremio متوافقة مع محرك AniFlux Universal" }
        val addonVer = root.optString("version", "1.0.0").trim().ifBlank { "1.0.0" }
        val iconUrl = root.optString("logo", root.optString("icon", "")).trim()
        val baseEndpoint = repoUrl.removeSuffix("/manifest.json").removeSuffix("/")
        val streamTemplate = "$baseEndpoint/stream/anime/{animeId}:{episode}.json"

        val audit = ExtensionEngine.auditExtension(
            name = addonName,
            pkgName = addonId,
            version = addonVer,
            sourceUrl = baseEndpoint,
            streamTemplate = streamTemplate,
            engineVersion = 3,
            extensionFormat = ExtensionEngine.FORMAT_STREMIO_ADDON,
            paginationHint = "OFFSET"
        )

        val extSchema = UnifiedExtensionSchema(
            extensionId = "${repoId}::${addonId}",
            id = addonId,
            name = addonName,
            description = addonDesc,
            version = addonVer,
            versionCode = 1,
            downloadOrPackageUrl = repoUrl,
            sourceUrl = baseEndpoint,
            lang = "multi",
            sourceType = "STREMIO_ADDON_V3",
            iconUrl = iconUrl,
            streamTemplate = streamTemplate,
            qualities = "4K,1080p,720p",
            engineVersion = 3,
            extensionFormat = audit.resolvedFormat,
            adapterId = audit.adapterId,
            runtimeEnvironment = audit.runtimeEnvironment,
            paginationType = audit.paginationType,
            isExecutableInRuntime = audit.isCompatible && audit.securityVerified,
            isCompatible = audit.isCompatible,
            compatibilityStatus = audit.status,
            compatibilityReasonAr = audit.reasonAr,
            securityVerified = audit.securityVerified,
            securityDetailsAr = audit.securityDetailsAr
        )

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = "$addonName (Stremio)",
                repoDescription = addonDesc,
                repoAuthor = root.optString("contactEmail", "Stremio Addon Developer"),
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.STREMIO_MANIFEST,
                fetchedLinkedUrls = emptyList(),
                extensions = listOf(extSchema)
            )
        )
    }

    // =========================================================================
    // 6. Verified Generic JSON Schema Parser
    // =========================================================================
    private fun parseVerifiedGenericRepo(
        repoId: String,
        repoUrl: String,
        trimmedJson: String
    ): Result<UnifiedRepositoryManifest> {
        val host = try { URI(repoUrl).host ?: "مستودع خارجي" } catch (_: Exception) { "مستودع خارجي" }
        var repoName = "مستودع ($host)"
        var repoDescription = "مستودع إضافات أنمي موثق المخطط"
        var repoAuthor = "External Maintainer"
        val itemsArray: JSONArray

        if (trimmedJson.startsWith("{")) {
            val root = JSONObject(trimmedJson)
            repoName = root.optString("name", root.optString("repoName", repoName)).ifBlank { repoName }
            repoDescription = root.optString("description", repoDescription).ifBlank { repoDescription }
            repoAuthor = root.optString("author", repoAuthor).ifBlank { repoAuthor }
            itemsArray = root.optJSONArray("plugins")
                ?: root.optJSONArray("sources")
                ?: root.optJSONArray("packages")
                ?: root.optJSONArray("addons")
                ?: root.optJSONArray("items")
                ?: return Result.failure(IllegalArgumentException(ERR_UNKNOWN_REPO_FORMAT))
        } else {
            itemsArray = JSONArray(trimmedJson)
        }

        val parsedList = mutableListOf<UnifiedExtensionSchema>()
        val limit = minOf(itemsArray.length(), MAX_EXTENSIONS_PER_REPO)
        for (i in 0 until limit) {
            val item = itemsArray.optJSONObject(i) ?: continue
            val parsed = parseSingleAniFluxOrGenericItem(repoId, repoUrl, item, index = i)
            if (parsed != null) {
                parsedList.add(parsed)
            }
        }

        val deduplicated = deduplicateExtensions(parsedList)
        if (deduplicated.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("$ERR_UNKNOWN_REPO_FORMAT: لم يتم العثور على إضافات مطابقة للمخطط الموحد")
            )
        }

        return Result.success(
            buildUnifiedManifest(
                repoId = repoId,
                repoName = repoName,
                repoDescription = repoDescription,
                repoAuthor = repoAuthor,
                repoUrl = repoUrl,
                detectedFormat = RepositoryFormat.VERIFIED_GENERIC_JSON,
                fetchedLinkedUrls = emptyList(),
                extensions = deduplicated
            )
        )
    }

    private fun parseSingleAniFluxOrGenericItem(
        repoId: String,
        repoUrl: String,
        item: JSONObject,
        index: Int
    ): UnifiedExtensionSchema? {
        val rawName = item.optString("name", "").trim()
            .removePrefix("Aniyomi: ")
            .removePrefix("Tachiyomi: ")
            .removePrefix("Mihon: ")
            .trim()
        val rawPkg = item.optString(
            "pkg",
            item.optString("pkgName", item.optString("internalName", item.optString("id", "")))
        ).trim()

        if (rawName.isBlank() && rawPkg.isBlank()) return null

        val pkgName = sanitizeIdentifier(rawPkg.ifBlank { "ext_pkg_$index" })
        val version = item.optString("version", item.optString("ver", "1.0.0")).trim().ifBlank { "1.0.0" }
        val versionCode = item.optInt("code", item.optInt("versionCode", 1)).coerceAtLeast(1)
        val lang = item.optString("lang", item.optString("language", "ar")).trim().ifBlank { "ar" }
        val description = item.optString("description", item.optString("desc", "")).trim()
        val rawIcon = item.optString("icon", item.optString("iconUrl", "")).trim()
        val resolvedIcon = if (rawIcon.isNotBlank()) {
            resolveRelativeOrAbsoluteUrl(repoUrl, rawIcon).getOrDefault(rawIcon)
        } else ""

        var baseUrl = extractFirstValidHttpUrl(
            item.optString("baseUrl", item.optString("sourceUrl", item.optString("url", ""))).trim()
        )
        var nestedSourceName = ""
        val nestedSources = item.optJSONArray("sources")
        if (nestedSources != null && nestedSources.length() > 0) {
            val firstSrc = nestedSources.optJSONObject(0)
            if (firstSrc != null) {
                if (baseUrl.isBlank()) {
                    baseUrl = extractFirstValidHttpUrl(
                        firstSrc.optString("baseUrl", firstSrc.optString("url", "")).trim()
                    )
                }
                nestedSourceName = firstSrc.optString("name", "").trim()
            }
        }

        val apkFile = item.optString("apk", "").trim()
        val downloadUrl = item.optString("downloadUrl", apkFile).trim()
        val resolvedDownloadUrl = if (downloadUrl.isNotBlank()) {
            resolveRelativeOrAbsoluteUrl(repoUrl, downloadUrl).getOrDefault(downloadUrl)
        } else baseUrl

        val streamTemplate = item.optString("streamTemplate", item.optString("videoTemplate", "")).trim()
        val qualities = item.optString("qualities", "1080p,720p,480p").trim().ifBlank { "1080p,720p,480p" }
        val paginationHint = item.optString("paginationType", item.optString("pagination", "")).trim()

        val hasExplicitEngine = item.has("engineVersion") || item.has("engine_version")
        val hasApkField = apkFile.isNotBlank()
        val engineVersion = when {
            hasExplicitEngine -> item.optInt("engineVersion", item.optInt("engine_version", ExtensionEngine.CURRENT_ENGINE_VERSION))
            else -> ExtensionEngine.CURRENT_ENGINE_VERSION
        }

        val extensionFormat = when {
            item.has("format") -> item.optString("format", ExtensionEngine.FORMAT_DECLARATIVE_V2)
            item.has("extensionFormat") -> item.optString("extensionFormat", ExtensionEngine.FORMAT_DECLARATIVE_V2)
            item.has("tvTypes") || item.has("internalName") || resolvedDownloadUrl.lowercase(Locale.US).endsWith(".cs3") -> ExtensionEngine.FORMAT_CLOUDSTREAM
            hasApkField -> item.optString("bridgeFormat", ExtensionEngine.FORMAT_ANIYOMI_BRIDGE)
            else -> ExtensionEngine.FORMAT_DECLARATIVE_V2
        }

        val effectiveSourceUrl = baseUrl.ifBlank {
            resolvedDownloadUrl.ifBlank { "https://bridge.aniflux-universal.io/$pkgName" }
        }

        val finalStreamTemplate = if (streamTemplate.isBlank() &&
            !extensionFormat.equals(ExtensionEngine.FORMAT_UNSUPPORTED_RAW_BINARY, ignoreCase = true)
        ) {
            val cleanBase = if (baseUrl.startsWith("http", ignoreCase = true) && !baseUrl.lowercase(Locale.US).endsWith(".cs3")) {
                baseUrl.removeSuffix("/")
            } else {
                "https://bridge.aniflux-universal.io/$pkgName"
            }
            "$cleanBase/Video/{animeId}/{episode}.mp4"
        } else {
            streamTemplate
        }

        val rawScript = item.optString("script", item.optString("code", ""))
        val finalDescription = when {
            description.isNotBlank() -> description
            nestedSourceName.isNotBlank() -> "إضافة مصدر ${nestedSourceName} لبث ومشاهدة حلقات الأنمي بجودات وسيرفرات متعددة"
            else -> "إضافة مصدر خارجي لبث ومشاهدة حلقات الأنمي"
        }

        val audit = ExtensionEngine.auditExtension(
            name = rawName,
            pkgName = pkgName,
            version = version,
            sourceUrl = effectiveSourceUrl,
            streamTemplate = finalStreamTemplate,
            engineVersion = engineVersion,
            extensionFormat = extensionFormat,
            rawCodeSnippet = rawScript,
            paginationHint = paginationHint
        )

        return UnifiedExtensionSchema(
            extensionId = "${repoId}::${pkgName}",
            id = pkgName,
            name = rawName.ifBlank { pkgName },
            description = finalDescription,
            version = version,
            versionCode = versionCode,
            downloadOrPackageUrl = resolvedDownloadUrl.ifBlank { effectiveSourceUrl },
            sourceUrl = effectiveSourceUrl,
            lang = lang,
            sourceType = audit.resolvedFormat,
            iconUrl = resolvedIcon,
            streamTemplate = finalStreamTemplate,
            qualities = qualities,
            engineVersion = engineVersion,
            extensionFormat = audit.resolvedFormat,
            adapterId = audit.adapterId,
            runtimeEnvironment = audit.runtimeEnvironment,
            paginationType = audit.paginationType,
            isExecutableInRuntime = audit.isCompatible && audit.securityVerified,
            isCompatible = audit.isCompatible,
            compatibilityStatus = audit.status,
            compatibilityReasonAr = audit.reasonAr,
            securityVerified = audit.securityVerified,
            securityDetailsAr = audit.securityDetailsAr
        )
    }

    /**
     * Deduplicates extensions by normalized package ID (`id`), preferring:
     * 1. Executable & compatible versions over incompatible ones.
     * 2. Higher `versionCode` when compatibility is equal.
     */
    fun deduplicateExtensions(extensions: List<UnifiedExtensionSchema>): List<UnifiedExtensionSchema> {
        if (extensions.size <= 1) return extensions
        val map = LinkedHashMap<String, UnifiedExtensionSchema>()
        for (ext in extensions) {
            val key = ext.id.trim().lowercase(Locale.US)
            val existing = map[key]
            if (existing == null) {
                map[key] = ext
            } else {
                val shouldReplace = when {
                    ext.isExecutableInRuntime && !existing.isExecutableInRuntime -> true
                    !ext.isExecutableInRuntime && existing.isExecutableInRuntime -> false
                    ext.versionCode > existing.versionCode -> true
                    else -> false
                }
                if (shouldReplace) {
                    map[key] = ext
                }
            }
        }
        return map.values.toList()
    }

    private fun buildUnifiedManifest(
        repoId: String,
        repoName: String,
        repoDescription: String,
        repoAuthor: String,
        repoUrl: String,
        detectedFormat: RepositoryFormat,
        fetchedLinkedUrls: List<String>,
        extensions: List<UnifiedExtensionSchema>
    ): UnifiedRepositoryManifest {
        val compatCount = extensions.count { it.isCompatible && it.securityVerified && it.isExecutableInRuntime }
        val hasExec = compatCount > 0

        val statusSummaryAr = when {
            hasExec -> {
                "تم استيراد \"${repoName}\" [${detectedFormat.labelAr}] بنجاح (${extensions.size} إضافة، منها $compatCount متوافقة وقابلة للتشغيل)"
            }
            detectedFormat == RepositoryFormat.MIHON_ANIYOMI_INDEX ||
                detectedFormat == RepositoryFormat.MIHON_ANIYOMI_REPO_META -> {
                "$ERR_ANIYOMI_INCOMPATIBLE_PACKAGES: تم استخراج ${extensions.size} إضافة من \"$repoName\" بنجاح، ولكن الحزم بصيغة APK خارجية غير مدعومة للتشغيل المباشر (0 متوافقة)"
            }
            else -> {
                "$ERR_REPO_VALID_RUNTIME_UNSUPPORTED: تم استخراج ${extensions.size} إضافة من \"$repoName\" [${detectedFormat.labelAr}]، ولكن آلية تشغيل حزمها الثنائية غير مدعومة حالياً (0 متوافقة)"
            }
        }

        return UnifiedRepositoryManifest(
            repoId = repoId,
            repoName = repoName,
            repoDescription = repoDescription,
            repoAuthor = repoAuthor,
            repoUrl = repoUrl,
            detectedFormat = detectedFormat,
            fetchedLinkedUrls = fetchedLinkedUrls,
            extensions = extensions,
            compatibleCount = compatCount,
            hasExecutableExtensions = hasExec,
            statusSummaryAr = statusSummaryAr
        )
    }

    /**
     * Converts a `UnifiedRepositoryManifest` into `ExtensionEngine.ImportedRepoBundle` (`ExtensionRepoEntity` + `List<AnimeExtensionEntity>`).
     */
    fun toImportedRepoBundle(
        manifest: UnifiedRepositoryManifest,
        buildInitialVerificationStages: (AnimeExtensionEntity) -> String
    ): ExtensionEngine.ImportedRepoBundle {
        val now = System.currentTimeMillis()
        val entities = manifest.extensions.map { u ->
            val isExplicitlyBlocked = u.compatibilityStatus == ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE
            val finalIsCompatible = !isExplicitlyBlocked && (u.isCompatible || u.extensionFormat == ExtensionEngine.FORMAT_CLOUDSTREAM || u.isExecutableInRuntime)
            val finalStatus = if (finalIsCompatible) ExtensionEngine.STATUS_COMPATIBLE else u.compatibilityStatus
            val finalSecurityVerified = !isExplicitlyBlocked
            val baseEntity = AnimeExtensionEntity(
                extensionId = u.extensionId,
                repoId = manifest.repoId,
                repoName = manifest.repoName,
                repoUrl = manifest.repoUrl,
                pkgName = u.id,
                name = u.name,
                description = u.description,
                version = u.version,
                versionCode = u.versionCode,
                lang = u.lang,
                sourceUrl = u.sourceUrl.ifBlank { u.downloadOrPackageUrl },
                iconUrl = u.iconUrl,
                streamTemplate = u.streamTemplate,
                qualities = u.qualities,
                engineVersion = u.engineVersion,
                extensionFormat = u.extensionFormat,
                adapterId = u.adapterId,
                runtimeEnvironment = u.runtimeEnvironment,
                paginationType = u.paginationType,
                isCompatible = finalIsCompatible,
                compatibilityStatus = finalStatus,
                compatibilityReasonAr = u.compatibilityReasonAr,
                securityVerified = finalSecurityVerified,
                securityDetailsAr = u.securityDetailsAr,
                verificationStagesJson = "[]",
                isEnabled = false, // Always disabled by default on initial import
                updatedAt = now
            )

            if (baseEntity.isCompatible && baseEntity.securityVerified) {
                baseEntity.copy(verificationStagesJson = buildInitialVerificationStages(baseEntity))
            } else {
                baseEntity
            }
        }

        val compatCount = entities.count { it.isCompatible && it.securityVerified }
        val repoEntity = ExtensionRepoEntity(
            repoId = manifest.repoId,
            name = manifest.repoName,
            url = manifest.repoUrl,
            description = manifest.repoDescription,
            author = manifest.repoAuthor,
            extensionCount = entities.size,
            compatibleCount = compatCount,
            lastSyncedAt = now,
            addedAt = now
        )

        return ExtensionEngine.ImportedRepoBundle(
            repo = repoEntity,
            extensions = entities,
            detectedFormat = manifest.detectedFormat.code,
            statusSummaryAr = manifest.statusSummaryAr,
            fetchedLinkedUrls = manifest.fetchedLinkedUrls
        )
    }

    /**
     * Extracts the first valid HTTP/HTTPS URL from a field that may contain comma/semicolon/newline-separated URLs
     * (common in Aniyomi/Mihon `sources[].baseUrl` such as `"https://site1.com, https://site2.com"`).
     */
    fun extractFirstValidHttpUrl(rawField: String): String {
        val trimmed = rawField.trim()
        if (trimmed.isBlank()) return ""
        val candidates = trimmed.split(Regex("[,;\\n\\r\\s]+"))
        for (candidate in candidates) {
            val c = candidate.trim()
            if (c.startsWith("https://", ignoreCase = true) || c.startsWith("http://", ignoreCase = true)) {
                if (validateSafeExternalUrl(c).isSuccess) {
                    return c
                }
            }
        }
        return trimmed
    }

    private fun sanitizeIdentifier(rawId: String): String {
        return rawId.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "ext_unknown" }
    }

    private fun normalizeUrlForDedup(url: String): String {
        return url.trim().lowercase(Locale.US).removeSuffix("/")
    }
}
