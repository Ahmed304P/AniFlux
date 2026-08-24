import re

with open("app/src/main/java/com/example/ui/screens/AdminAnimeManagerScreen.kt", "r") as f:
    content = f.read()

replacement = """
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = animeList.filter { 
        it.titleAr.contains(searchQuery, true) || it.titleEn.contains(searchQuery, true) 
    }

    Scaffold(
"""

content = content.replace("    Scaffold(", replacement)

top_bar_replacement = """        topBar = {
            Column {
                TopAppBar(
                    title = { Text("إدارة الأنمي", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ابحث عن أنمي...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        },"""

content = re.sub(r'        topBar = \{.*?        \},', top_bar_replacement, content, flags=re.DOTALL)

items_replacement = """        ) {
            items(filteredList, key = { it.id }) { anime ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable { onEditAnime(anime.id) },"""

content = re.sub(r'        \) \{\n            items\(animeList\) \{ anime ->\n                Card\(\n                    modifier = Modifier\n                        \.fillMaxWidth\(\)\n                        \.clickable \{ onEditAnime\(anime\.id\) \},', items_replacement, content)

with open("app/src/main/java/com/example/ui/screens/AdminAnimeManagerScreen.kt", "w") as f:
    f.write(content)
