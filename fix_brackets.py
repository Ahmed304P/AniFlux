with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    for i, line in enumerate(lines):
        if i == 439 or i == 440:
            continue
        f.write(line)
