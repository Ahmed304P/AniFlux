import re

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'r') as f:
    content = f.read()

# Change skipPartiallyExpanded to true
content = content.replace("skipPartiallyExpanded = false", "skipPartiallyExpanded = true")

# Add fillMaxHeight(0.9f) to the Column inside ModalBottomSheet
content = content.replace(
"""        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),""",
"""        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),"""
)

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'w') as f:
    f.write(content)
