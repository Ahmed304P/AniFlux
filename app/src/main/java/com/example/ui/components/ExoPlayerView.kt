package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.data.model.SubtitleTrack
import com.example.util.ExtensionCatalogEngine
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    url: String,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    playbackSpeed: Float = 1.0f,
    streamType: String = "MP4",
    headers: Map<String, String> = emptyMap(),
    subtitleTracks: List<SubtitleTrack> = emptyList(),
    retryToken: Int = 0,
    onFullscreenToggle: (Boolean) -> Unit = {},
    onPlaybackReady: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
    onServerFailureLog: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnPlaybackReady by rememberUpdatedState(onPlaybackReady)
    val currentOnPlaybackError by rememberUpdatedState(onPlaybackError)
    val currentOnFullscreenToggle by rememberUpdatedState(onFullscreenToggle)
    val currentOnServerFailureLog by rememberUpdatedState(onServerFailureLog)

    var currentPositionMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(500L)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    currentOnPlaybackReady()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Report actual server failure instead of silently substituting a fake fallback video
                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "فشل الاتصال بالسيرفر (HTTP Status Error)"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "تعذر الاتصال بشبكة البث"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "صيغة حاوية الفيديو غير مدعومة"
                    else -> "تعذر تشغيل السيرفر الحالي (${error.errorCodeName})"
                }
                currentOnServerFailureLog("URL=$url | Error=${error.errorCodeName} (${error.errorCode}): ${error.message}")
                currentOnPlaybackError(msg)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Manage fullscreen orientation
    val activity = context.findActivity()
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(url, streamType, headers, subtitleTracks, retryToken) {
        if (url.isBlank()) return@LaunchedEffect

        val playableUrl = ExtensionCatalogEngine.normalizePlayableStreamUrl(url)
        val lowerUrl = playableUrl.lowercase(Locale.US)

        // Pass all legitimate HTTP request headers (Referer, Origin, Cookie, etc.) while filtering out binary package paths
        val sanitizedHeaders = headers.filter { (k, v) ->
            val lowerV = v.lowercase(Locale.US)
            !k.equals("User-Agent", ignoreCase = true) &&
                !lowerV.endsWith(".cs3") &&
                !lowerV.endsWith(".apk")
        }

        val userAgent = headers["User-Agent"] ?: "Mozilla/5.0 (Linux; Android 14; AniFluxUniversalPlayer/3.5)"
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(15_000)
            .setDefaultRequestProperties(sanitizedHeaders)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)

        val resolvedMimeType: String? = when {
            lowerUrl.contains(".m3u8") || lowerUrl.contains(".ism/.m3u8") -> MimeTypes.APPLICATION_M3U8
            lowerUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            lowerUrl.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            lowerUrl.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
            streamType.equals("HLS", ignoreCase = true) && !lowerUrl.endsWith(".mp4") -> MimeTypes.APPLICATION_M3U8
            streamType.equals("DASH", ignoreCase = true) && !lowerUrl.endsWith(".mp4") -> MimeTypes.APPLICATION_MPD
            else -> null
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(playableUrl))
        if (resolvedMimeType != null) {
            mediaItemBuilder.setMimeType(resolvedMimeType)
        }

        // Attach external subtitle tracks (SRT, VTT, ASS, SSA) if provided
        val subtitleConfigs = subtitleTracks
            .filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
            .map { sub ->
                val subMime = when (sub.format.uppercase(Locale.US)) {
                    "SRT" -> MimeTypes.APPLICATION_SUBRIP
                    "ASS", "SSA" -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.TEXT_VTT
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(subMime)
                    .setLanguage(sub.langCode.ifBlank { "ar" })
                    .setLabel(sub.label.ifBlank { "العربية" })
                    .build()
            }
        if (subtitleConfigs.isNotEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowSubtitleButton(false)

                    setFullscreenButtonClickListener { isFullScreenClicked ->
                        currentOnFullscreenToggle(isFullScreenClicked)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
