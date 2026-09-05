with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.End, ) {',
    'Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {'
)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

