with open('app/src/main/java/com/example/util/AniListService.kt', 'r') as f:
    lines = f.readlines()

with open('app/src/main/java/com/example/util/AniListService.kt', 'w') as f:
    for line in lines:
        if line.endswith('\\\n'):
            f.write(line[:-2] + '\n')
        elif line.endswith('\\'):
            f.write(line[:-1] + '\n')
        else:
            f.write(line)
