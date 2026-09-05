import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update ContentRatingSection loop
loop_old = """            categories.forEach { category ->
                // Read from SharedPreferences
                val votedOption = prefs.getString("${anime.id}_${category.title}_vote", null)
                
                // Determine current predominant and count
                val currentOption = votedOption ?: category.defaultOption
                val currentCount = category.baseCount + if (votedOption != null) 1 else 0
                val currentColor = getColorForOption(currentOption)
                
                RatingRow(
                    title = category.title,
                    value = "$currentOption ($currentCount)",
                    color = currentColor,
                    onAddClick = {
                        val hasVoted = prefs.getBoolean("${anime.id}_${category.title}_has_voted", false)
                        if (hasVoted) {
                            android.widget.Toast.makeText(context, "لقد قمت بالتقييم مسبقاً في هذا القسم", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            selectedCategory = category
                            showBottomSheet = true
                        }
                    },
                    dummyTrigger = refreshTrigger
                )
            }"""

loop_new = """            categories.forEach { category ->
                // Read from SharedPreferences
                val votedOption = prefs.getString("${anime.id}_${category.title}_vote", null)
                val hasVoted = prefs.getBoolean("${anime.id}_${category.title}_has_voted", false)
                
                // Determine current predominant and count
                val currentOption = votedOption ?: category.defaultOption
                val currentCount = category.baseCount + if (votedOption != null) 1 else 0
                val currentColor = getColorForOption(currentOption)
                
                RatingRow(
                    title = category.title,
                    value = "$currentOption ($currentCount)",
                    color = currentColor,
                    hasVoted = hasVoted,
                    onAddClick = {
                        if (!hasVoted) {
                            selectedCategory = category
                            showBottomSheet = true
                        }
                    },
                    dummyTrigger = refreshTrigger
                )
            }"""
content = content.replace(loop_old, loop_new)

# 2. Update RatingRow
rating_row_old = """@Composable
fun RatingRow(title: String, value: String, color: Color, onAddClick: () -> Unit, dummyTrigger: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rating", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier
            .size(8.dp, 24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color))
    }
}"""

rating_row_new = """@Composable
fun RatingRow(title: String, value: String, color: Color, hasVoted: Boolean, onAddClick: () -> Unit, dummyTrigger: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !hasVoted,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rating", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier
            .size(8.dp, 24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color))
    }
}"""
content = content.replace(rating_row_old, rating_row_new)

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated successfully!")
