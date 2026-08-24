import sys

try:
    with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
        content = f.read()

    target = """animeId = viewModel.adminEditingAnimeId ?: "mock_id","""
    replacement = """animeId = viewModel.adminEditingAnimeId ?: "","""
    
    if target in content:
        content = content.replace(target, replacement)
        with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
            f.write(content)
        print("Success: MainActivity mock patched")
    else:
        print("Could not find mock target in MainActivity")

except Exception as e:
    print(f"Error: {e}")
