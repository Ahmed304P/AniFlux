with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """    LaunchedEffect(isPlaying) {
        while (isPlaying && currentProgressSeconds < totalSeconds) {
            delay(1000L * (1f / selectedSpeed).toLong())
            currentProgressSeconds += 1f
            if (currentProgressSeconds % 10f == 0f) {
                onRecordProgress(currentProgressSeconds.toInt(), totalSeconds.toInt())
            }
        }
    }"""

replacement = target + """

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)
