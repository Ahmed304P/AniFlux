import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_complete = """                    18 -> CompleteProfileScreen(
                        onComplete = { name, avatar, banner ->
                            viewModel.completeLogin(name, avatar, banner)
                            viewModel.setSelectedTab(0, addToHistory = false)
                        }
                    )"""

new_complete = """                    18 -> CompleteProfileScreen(
                        onComplete = { name, avatar, banner ->
                            viewModel.completeLogin(name, avatar, banner)
                            viewModel.setSelectedTab(15, addToHistory = false) // Navigate to ProfileScreen (15) instead of Home
                        }
                    )"""

content = content.replace(old_complete, new_complete)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MainActivity updated.")
