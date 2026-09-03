with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Insert if (!isFullscreen) { before Server Selector Bar (Collapsible)
lines.insert(294, "            if (!isFullscreen) {\n")

# Insert } after LazyColumn closing bracket
lines.insert(440, "            }\n")

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    for line in lines:
        f.write(line)
