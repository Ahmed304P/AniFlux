import re
import os

def insert_back_handler(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if "import androidx.activity.compose.BackHandler" not in content:
        content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.activity.compose.BackHandler")
    
    if "BackHandler { onBack() }" not in content:
        # Find the first { after Modifier = Modifier)
        match = re.search(r'Modifier = Modifier\s*\)\s*\{', content)
        if match:
            idx = match.end()
            content = content[:idx] + "\n    BackHandler { onBack() }" + content[idx:]
            with open(filepath, 'w') as f:
                f.write(content)
            print(f"Patched {os.path.basename(filepath)}")

insert_back_handler('app/src/main/java/com/example/ui/screens/CustomListDetailScreen.kt')
insert_back_handler('app/src/main/java/com/example/ui/screens/CreateListScreen.kt')
insert_back_handler('app/src/main/java/com/example/ui/screens/AddAnimeToListScreen.kt')
