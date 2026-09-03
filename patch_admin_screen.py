import re

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_label = 'label = { Text("عدد الحلقات المتاحة حالياً", color = Color.Gray, fontSize = 13.sp) },'
new_label = 'label = { Text("عدد الحلقات المتاحة (هام جداً لتوليد الحلقات)", color = Color.Gray, fontSize = 13.sp) },'

content = content.replace(old_label, new_label)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
