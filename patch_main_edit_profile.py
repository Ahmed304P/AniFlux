import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_edit_call = """                    16 -> EditProfileScreen(
                        onBackClick = { viewModel.setSelectedTab(15) }
                    )"""

new_edit_call = """                    16 -> {
                        val lastChange by viewModel.lastHandleChangeDate.collectAsStateWithLifecycle()
                        EditProfileScreen(
                            currentName = userName,
                            currentHandle = userHandle,
                            currentAvatar = userAvatar,
                            currentBanner = userBanner,
                            lastHandleChangeDate = lastChange,
                            onSaveProfile = { name, avatar, banner ->
                                viewModel.updateProfile(name, avatar, banner)
                            },
                            onSaveHandle = { handle ->
                                viewModel.updateHandle(handle)
                            },
                            onBackClick = { viewModel.setSelectedTab(15) }
                        )
                    }"""

content = content.replace(old_edit_call, new_edit_call)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("MainActivity updated for EditProfile")
