import re

with open('app/src/main/java/com/example/ui/components/WebViewPlayer.kt', 'r', encoding='utf-8') as f:
    content = f.read()

load_logic_factory = """                if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true)) {
                    val html = \"\"\"
                        <html>
                        <body style="margin:0;padding:0;background:black;display:flex;justify-content:center;align-items:center;">
                            <video width="100%" height="100%" controls autoplay>
                                <source src="$url" type="video/mp4">
                            </video>
                        </body>
                        </html>
                    \"\"\".trimIndent()
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                } else {
                    loadUrl(url)
                }"""

load_logic_update = """            if (webView.url != url) {
                if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true)) {
                    val html = \"\"\"
                        <html>
                        <body style="margin:0;padding:0;background:black;display:flex;justify-content:center;align-items:center;">
                            <video width="100%" height="100%" controls autoplay>
                                <source src="$url" type="video/mp4">
                            </video>
                        </body>
                        </html>
                    \"\"\".trimIndent()
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                } else {
                    webView.loadUrl(url)
                }
            }"""

content = content.replace("loadUrl(url)", load_logic_factory, 1)

old_update = """            if (webView.url != url) {
                webView.loadUrl(url)
            }"""
content = content.replace(old_update, load_logic_update)

with open('app/src/main/java/com/example/ui/components/WebViewPlayer.kt', 'w', encoding='utf-8') as f:
    f.write(content)
