with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
    
    if depth < 0:
        print(f"Error: Depth dropped below zero at line {i+1}: {line.strip()}")
        break
    
print(f"Final depth: {depth}")
