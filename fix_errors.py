import re

# Fix AnimeViewModel.kt
with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w', encoding='utf-8') as f:
    for i, line in enumerate(lines):
        if 'val activePlayerEpisode: StateFlow<Pair<Anime, Episode>?> = _activePlayerEpisode.asStateFlow()' in line:
            continue # skip duplicate
        f.write(line)

# Fix MainActivity.kt
with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'viewModel.openPlayer(anime, ep, server)',
    'viewModel.openPlayer(anime, ep, activeServerForEp)'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
