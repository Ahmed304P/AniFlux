import sys

try:
    with open("app/build.gradle.kts", "r") as f:
        content = f.read()

    target = 'versionName = "2.6"'
    replacement = 'versionName = "2.7"'
    
    if target in content:
        content = content.replace(target, replacement)
        with open("app/build.gradle.kts", "w") as f:
            f.write(content)
        print("Success: Version bumped to 2.7")
    else:
        print("Could not find version 2.6 in build.gradle.kts")

except Exception as e:
    print(f"Error: {e}")
