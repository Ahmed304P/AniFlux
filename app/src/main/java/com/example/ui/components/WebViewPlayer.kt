package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setSupportZoom(false)
                }
                
                webChromeClient = WebChromeClient()
                
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString() ?: ""
                        // Simple Ad-blocker logic to block common ad domains
                        val adDomains = listOf(
                            "adserv", "popads", "popcash", "adsterra", "exoclick", "propellerads",
                            "onclickads", "syndication", "doubleclick"
                        )
                        if (adDomains.any { requestUrl.contains(it) }) {
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                ByteArrayInputStream("".toByteArray())
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                
                                if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true)) {
                    val html = """
                        <html>
                        <body style="margin:0;padding:0;background:black;display:flex;justify-content:center;align-items:center;">
                            <video width="100%" height="100%" controls autoplay>
                                <source src="$url" type="video/mp4">
                            </video>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                } else {
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            if (webView.url != url) {
                if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true)) {
                    val html = """
                        <html>
                        <body style="margin:0;padding:0;background:black;display:flex;justify-content:center;align-items:center;">
                            <video width="100%" height="100%" controls autoplay>
                                <source src="$url" type="video/mp4">
                            </video>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                } else {
                    webView.loadUrl(url)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
