import re

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I removed an extra brace previously, but it seems there is a missing brace now at the end of the file.
content = content + "\n}\n"

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added brace back.")
