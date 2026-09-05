import re

with open('app/src/main/java/com/example/ui/screens/SearchScreenOverlay.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add title: String? = null to signature
content = content.replace("onResetFilters: () -> Unit\n, isGridMode: Boolean = true) {", "onResetFilters: () -> Unit\n, isGridMode: Boolean = true, title: String? = null) {")

# 2. Modify LaunchedEffect to only focus if title is null
launched_effect_old = """        LaunchedEffect(visible, isDetailScreenOpen) {
            if (visible && !isDetailScreenOpen) {
                kotlinx.coroutines.delay(300) // Wait for transition
                focusRequester.requestFocus()
                keyboardController?.show()
            } else {
                keyboardController?.hide()
            }
        }"""
launched_effect_new = """        LaunchedEffect(visible, isDetailScreenOpen) {
            if (visible && !isDetailScreenOpen && title == null) {
                kotlinx.coroutines.delay(300) // Wait for transition
                focusRequester.requestFocus()
                keyboardController?.show()
            } else {
                keyboardController?.hide()
            }
        }"""
content = content.replace(launched_effect_old, launched_effect_new)

# 3. Replace the Row content for Search Top App Bar
top_bar_old = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "أدخل اسم الانمي...",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            maxLines = 1
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = { /* Sort */ }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                }"""

top_bar_new = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    if (title != null) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "أدخل اسم الانمي...",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                maxLines = 1
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { /* Sort */ }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                }"""

content = content.replace(top_bar_old, top_bar_new)

with open('app/src/main/java/com/example/ui/screens/SearchScreenOverlay.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("SearchScreenOverlay.kt patched.")
