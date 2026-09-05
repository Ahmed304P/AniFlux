import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_vars = """    val userHandle by viewModel.userHandle.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val userBanner by viewModel.userBanner.collectAsStateWithLifecycle()"""

new_vars = """    val userHandle by viewModel.userHandle.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val userBanner by viewModel.userBanner.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val userBirthDate by viewModel.userBirthDate.collectAsStateWithLifecycle()
    val userJoinDate by viewModel.userJoinDate.collectAsStateWithLifecycle()"""
content = content.replace(old_vars, new_vars)

old_profile_call = """                    15 -> ProfileScreen(
                        userName = userName,
                        userHandle = userHandle,
                        userAvatar = userAvatar,
                        userBanner = userBanner,
                        onLogoutClick = {"""

new_profile_call = """                    15 -> ProfileScreen(
                        userName = userName,
                        userHandle = userHandle,
                        userAvatar = userAvatar,
                        userBanner = userBanner,
                        userLocation = userLocation,
                        userBirthDate = userBirthDate,
                        userJoinDate = userJoinDate,
                        onLogoutClick = {"""
content = content.replace(old_profile_call, new_profile_call)

old_edit_call = """                        EditProfileScreen(
                            currentName = userName,
                            currentHandle = userHandle,
                            currentAvatar = userAvatar,
                            currentBanner = userBanner,
                            lastHandleChangeDate = lastChange,
                            onSaveProfile = { name, avatar, banner ->
                                viewModel.updateProfile(name, avatar, banner)
                            },"""

new_edit_call = """                        EditProfileScreen(
                            currentName = userName,
                            currentHandle = userHandle,
                            currentAvatar = userAvatar,
                            currentBanner = userBanner,
                            currentLocation = userLocation,
                            currentBirthDate = userBirthDate,
                            lastHandleChangeDate = lastChange,
                            onSaveProfile = { name, avatar, banner, loc, bdate ->
                                viewModel.updateProfile(name, avatar, banner, loc, bdate)
                            },"""
content = content.replace(old_edit_call, new_edit_call)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MainActivity updated")
