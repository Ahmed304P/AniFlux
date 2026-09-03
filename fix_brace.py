import re

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('// Server Selector Bar', '}\n            // Server Selector Bar')

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
