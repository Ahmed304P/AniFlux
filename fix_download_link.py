import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_click = """                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(server.streamUrl), "video/*")
                                    intent.setPackage("com.dv.adm")
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "تطبيق ADM غير مثبت", android.widget.Toast.LENGTH_SHORT).show()
                                    try {
                                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(server.streamUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {}
                                }
                                serverActionSheet = null
                            }"""

new_click = """                            .clickable {
                                val originalUrl = server.streamUrl
                                val downloadUrl = if (originalUrl.contains("pixeldrain.com") && !originalUrl.contains("?download")) {
                                    "$originalUrl?download"
                                } else {
                                    originalUrl
                                }
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.setDataAndType(android.net.Uri.parse(downloadUrl), "video/*")
                                    intent.setPackage("com.dv.adm")
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "تطبيق ADM غير مثبت", android.widget.Toast.LENGTH_SHORT).show()
                                    try {
                                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {}
                                }
                                serverActionSheet = null
                            }"""

content = content.replace(old_click, new_click)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
