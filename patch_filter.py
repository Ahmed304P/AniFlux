import re

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'r') as f:
    content = f.read()

# Add import if needed
if "import androidx.compose.foundation.rememberScrollState" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll")

# Replace LazyColumn with Column
content = re.sub(
    r'LazyColumn\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.weight\(1f, fill = false\),\s*verticalArrangement = Arrangement\.spacedBy\(16\.dp\)\s*\)',
    r'val scrollState = rememberScrollState()\n            Column(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .weight(1f, fill = false)\n                    .verticalScroll(scrollState),\n                verticalArrangement = Arrangement.spacedBy(16.dp)\n            )',
    content
)

# Remove 'item {' and '}' from around the sections
content = content.replace('                item {\n                    FilterSection(', '                FilterSection(')
content = content.replace('                    )\n                }', '                )')

content = content.replace('                item {\n                    ExpandableFilterSection(', '                ExpandableFilterSection(')

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'w') as f:
    f.write(content)
print("FilterBottomSheet patched")
