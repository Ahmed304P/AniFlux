import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

# Add version
content = content.replace('[versions]\n', '[versions]\nmedia3 = "1.2.1"\n')

# Add libraries
libs_block = """[libraries]
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }"""
content = content.replace('[libraries]\n', libs_block + '\n')

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)

with open('app/build.gradle.kts', 'r') as f:
    bg = f.read()

bg = bg.replace('implementation(libs.androidx.core.ktx)', 'implementation(libs.androidx.core.ktx)\n  implementation(libs.androidx.media3.exoplayer)\n  implementation(libs.androidx.media3.ui)')

with open('app/build.gradle.kts', 'w') as f:
    f.write(bg)
