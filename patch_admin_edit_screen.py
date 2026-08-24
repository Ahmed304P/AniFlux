import sys

try:
    with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
        content = f.read()

    top_bar_target = """        topBar = {
            TopAppBar(
                title ="""
    top_bar_replacement = """        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                title ="""
    
    if top_bar_target in content:
        content = content.replace(top_bar_target, top_bar_replacement)
        with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
            f.write(content)
        print("Success: AdminAnimeEditScreen patched")
    else:
        print("Could not find TopAppBar in AdminAnimeEditScreen")

except Exception as e:
    print(f"Error: {e}")
