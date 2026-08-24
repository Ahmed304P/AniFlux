import sys

try:
    with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
        content = f.read()

    scaffold_target = """    Scaffold(
        modifier = modifier.fillMaxSize(),"""
    scaffold_replacement = """    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),"""
    
    if scaffold_target in content:
        content = content.replace(scaffold_target, scaffold_replacement)
        with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
            f.write(content)
        print("Success: AdminAnimeEditScreen scaffold patched")
    else:
        print("Could not find Scaffold target in AdminAnimeEditScreen")

except Exception as e:
    print(f"Error: {e}")
