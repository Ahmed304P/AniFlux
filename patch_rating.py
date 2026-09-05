import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update ContentRatingSection call
content = content.replace("ContentRatingSection()", "ContentRatingSection(anime)")

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated call")
