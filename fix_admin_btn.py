with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'isCheckingLinks = false\n                                }\n                            }\n                                .height(48.dp),',
    'isCheckingLinks = false\n                                }\n                            },\n                            modifier = Modifier.weight(1f).height(48.dp),'
)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
