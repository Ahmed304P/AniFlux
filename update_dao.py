import re

with open("app/src/main/java/com/example/data/local/AniFluxDao.kt", "r") as f:
    content = f.read()

content = content.replace(
    '@Query("SELECT * FROM published_anime")',
    '@Query("SELECT * FROM published_anime ORDER BY rowid DESC")'
)

with open("app/src/main/java/com/example/data/local/AniFluxDao.kt", "w") as f:
    f.write(content)
