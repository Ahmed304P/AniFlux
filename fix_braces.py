with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

while lines[-1].strip() == '}':
    lines.pop()

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(lines)
    f.write('}\n')
    f.write('}\n')
