import re

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_loc = """                // Location
                ProfileTextField(
                    label = "الموقع الجغرافي",
                    value = location,
                    onValueChange = { if (it.length <= 30) location = it },
                    placeholder = "مثال: اليمن، صنعاء",
                    charCount = "${location.length}/30"
                )"""

new_loc = """                // Location
                ProfileTextField(
                    label = "الموقع الجغرافي",
                    value = location,
                    onValueChange = { if (it.length <= 30) location = it },
                    placeholder = "",
                    charCount = "${location.length}/30"
                )"""
content = content.replace(old_loc, new_loc)

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("EditProfileScreen updated placeholder")
