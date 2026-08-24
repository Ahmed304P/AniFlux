import sys

try:
    with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
        content = f.read()

    target = """                                                type = anime.format
                                                season = anime.season
                                                year = anime.year
                                                characters = anime.charactersList"""
    replacement = """                                                type = anime.format
                                                season = anime.season
                                                year = anime.year
                                                if (anime.studio.isNotBlank()) studio = anime.studio
                                                characters = anime.charactersList"""
    
    if target in content:
        content = content.replace(target, replacement)
        with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
            f.write(content)
        print("Success: AdminAnimeEditScreen patched to autofill studio")
    else:
        print("Could not find target in AdminAnimeEditScreen")

except Exception as e:
    print(f"Error: {e}")
