with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

# count open and close braces
open_braces = text.count('{')
close_braces = text.count('}')
diff = open_braces - close_braces

print(f"Open: {open_braces}, Close: {close_braces}, Diff: {diff}")

lines = text.split('\n')
while lines and lines[-1].strip() == '}':
    lines.pop()

text = '\n'.join(lines)
open_braces = text.count('{')
close_braces = text.count('}')
diff = open_braces - close_braces

print(f"Open: {open_braces}, Close: {close_braces}, Diff: {diff}")

if diff > 0:
    text += '\n' + '}\n' * diff

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
