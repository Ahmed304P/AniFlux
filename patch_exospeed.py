import re

with open('app/src/main/java/com/example/ui/components/ExoPlayerView.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "isFullscreen: Boolean = false,",
    "isFullscreen: Boolean = false,\n    playbackSpeed: Float = 1.0f,"
)

speed_logic = """    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(url) {"""

content = content.replace("    LaunchedEffect(url) {", speed_logic)

with open('app/src/main/java/com/example/ui/components/ExoPlayerView.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r') as f:
    vm = f.read()

vm = vm.replace(
    "isFullscreen = isFullscreen,",
    "isFullscreen = isFullscreen,\n                        playbackSpeed = selectedSpeed,"
)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w') as f:
    f.write(vm)
