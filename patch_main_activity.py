import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_vars = """    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val userBanner by viewModel.userBanner.collectAsStateWithLifecycle()"""

new_vars = """    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userHandle by viewModel.userHandle.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val userBanner by viewModel.userBanner.collectAsStateWithLifecycle()"""
content = content.replace(old_vars, new_vars)

old_profile_call = """                    15 -> ProfileScreen(
                        userName = userName,
                        userAvatar = userAvatar,
                        userBanner = userBanner,
                        onLogoutClick = {"""

new_profile_call = """                    15 -> ProfileScreen(
                        userName = userName,
                        userHandle = userHandle,
                        userAvatar = userAvatar,
                        userBanner = userBanner,
                        onLogoutClick = {"""
content = content.replace(old_profile_call, new_profile_call)

old_complete_call = """                    18 -> CompleteProfileScreen(
                        onComplete = { name, avatar, banner ->
                            viewModel.completeLogin(name, avatar, banner)
                            viewModel.setSelectedTab(15, addToHistory = false) // Navigate to ProfileScreen (15) instead of Home
                        }
                    )"""

new_complete_call = """                    18 -> CompleteProfileScreen(
                        onComplete = { name, handle, avatar, banner ->
                            viewModel.completeLogin(name, handle, avatar, banner)
                            viewModel.setSelectedTab(15, addToHistory = false) // Navigate to ProfileScreen (15) instead of Home
                        }
                    )"""
content = content.replace(old_complete_call, new_complete_call)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MainActivity updated")
