with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if i == 577: # "val epNumInt = epNum.toIntOrNull()"
        skip = True
    if skip and i == 615: # End of the old button click block
        skip = False
        
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/AdminEpisodeManagerScreen.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
