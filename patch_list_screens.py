import re
import os

def insert_back_handler(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if "import androidx.activity.compose.BackHandler" not in content:
        content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.activity.compose.BackHandler")
    
    if "BackHandler" not in content:
        # Insert BackHandler as the first child of the main composable
        content = re.sub(r'(fun \w+\(.*?\)\s*\{)', r'\1\n    BackHandler { onBack() }\n', content, count=1)
        
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Patched {os.path.basename(filepath)}")

insert_back_handler('app/src/main/java/com/example/ui/screens/CustomListDetailScreen.kt')
insert_back_handler('app/src/main/java/com/example/ui/screens/CreateListScreen.kt')
insert_back_handler('app/src/main/java/com/example/ui/screens/AddAnimeToListScreen.kt')
