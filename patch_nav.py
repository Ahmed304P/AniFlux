import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# ProfileScreen onBackClick
content = content.replace(
    'onBackClick = { viewModel.setSelectedTab(0, addToHistory = false) }',
    'onBackClick = { if (!viewModel.popTabStack()) viewModel.setSelectedTab(0, addToHistory = false) }'
)

# EditProfileScreen onBackClick
content = content.replace(
    'onBackClick = { viewModel.setSelectedTab(15) }',
    'onBackClick = { if (!viewModel.popTabStack()) viewModel.setSelectedTab(15, addToHistory = false) }'
)

# LoginScreen onBack
content = content.replace(
    'onBack = { viewModel.setSelectedTab(0, addToHistory = false) }',
    'onBack = { if (!viewModel.popTabStack()) viewModel.setSelectedTab(0, addToHistory = false) }'
)

# LoginScreen onLoginSuccess (replace so back doesn't go to login)
content = content.replace(
    'onLoginSuccess = { viewModel.setSelectedTab(18) }',
    'onLoginSuccess = { viewModel.setSelectedTab(18, addToHistory = false) }'
)

# LoginScreen onAdminBackdoor
content = content.replace(
    'onAdminBackdoor = { viewModel.setSelectedTab(19) }',
    'onAdminBackdoor = { viewModel.setSelectedTab(19, addToHistory = false) }'
)

# AdminPasswordScreen onSuccess
content = content.replace(
    'onSuccess = { viewModel.setSelectedTab(20) }',
    'onSuccess = { viewModel.setSelectedTab(20, addToHistory = false) }'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

print("Navigation patched in MainActivity")
