import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'label = { Text("رابط الفيديو المباشر (mp4/m3u8)", color = Color.Gray, fontSize = 13.sp) }',
    'label = { Text("روابط السيرفرات (مثال: ST|رابط, MF2|رابط)", color = Color.Gray, fontSize = 13.sp) }'
)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
