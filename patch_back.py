import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("onBack = { viewModel.setSelectedTab(17) }", "onBack = { viewModel.popTabStack() }")
content = content.replace("onBack = { viewModel.setSelectedTab(19) }", "onBack = { viewModel.popTabStack() }")
content = content.replace("onBack = { viewModel.setSelectedTab(20) }", "onBack = { viewModel.popTabStack() }")
content = content.replace("onBack = { viewModel.setSelectedTab(21) }", "onBack = { viewModel.popTabStack() }")
content = content.replace("onBack = { viewModel.setSelectedTab(22) }", "onBack = { viewModel.popTabStack() }")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

print("Patched back navigation.")
