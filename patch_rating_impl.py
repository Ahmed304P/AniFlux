import re

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

start_idx = content.find('fun ContentRatingSection(anime: Anime) {')
end_idx = content.find('fun TrailerSection(anime: Anime) {', start_idx)

if start_idx != -1 and end_idx != -1:
    new_impl = """@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ContentRatingSection(anime: Anime) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("anime_content_ratings", android.content.Context.MODE_PRIVATE)

    data class ContentRatingCategory(
        val title: String,
        val defaultOption: String,
        val baseCount: Int
    )

    val hash = anime.id.hashCode().toLong()
    val categories = remember {
        listOf(
            ContentRatingCategory("عنف ودموية", if (hash % 2L == 0L) "شديد" else "خفيف", 89 + (hash % 20).toInt()),
            ContentRatingCategory("ألفاظ نابية", if (hash % 3L == 0L) "متوسط" else "لا يوجد", 77 + (hash % 30).toInt()),
            ContentRatingCategory("مشاهد غير لائقة", if (hash % 4L == 0L) "شديد" else "لا يوجد", 162 + (hash % 40).toInt()),
            ContentRatingCategory("مشاهد مخيفة او صادمة", if (hash % 5L == 0L) "متوسط" else "شديد", 87 + (hash % 25).toInt()),
            ContentRatingCategory("مخدرات وكحول وتدخين", if (hash % 2L != 0L) "خفيف" else "لا يوجد", 72 + (hash % 30).toInt())
        )
    }

    var selectedCategory by remember { mutableStateOf<ContentRatingCategory?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun getColorForOption(option: String): Color {
        return when (option) {
            "لا يوجد" -> Color(0xFF2196F3) // Blue
            "خفيف" -> Color(0xFF4CAF50) // Green
            "متوسط" -> Color(0xFFFF9800) // Orange
            "شديد" -> Color(0xFFF44336) // Red
            else -> Color.Gray
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تصنيف المحتوى",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.End
            )

            categories.forEach { category ->
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
            }
        }
    }

    if (showBottomSheet && selectedCategory != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val cat = selectedCategory!!
            val votedOption = prefs.getString("${anime.id}_${cat.title}_vote", null)
            val currentOption = votedOption ?: cat.defaultOption
            val currentCount = cat.baseCount + if (votedOption != null) 1 else 0
            val currentColor = getColorForOption(currentOption)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cat.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = currentOption,
                    color = currentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "عدد المصوتين $currentCount",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "هل يحتوي على ${cat.title}؟",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val options = listOf("لا يوجد", "خفيف", "متوسط", "شديد")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    options.forEach { option ->
                        val optColor = getColorForOption(option)
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                prefs.edit()
                                    .putBoolean("${anime.id}_${cat.title}_has_voted", true)
                                    .putString("${anime.id}_${cat.title}_vote", option)
                                    .apply()
                                refreshTrigger++ // trigger recomposition
                                showBottomSheet = false
                                android.widget.Toast.makeText(context, "تم تسجيل تقييمك بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, optColor),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = optColor,
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
                        ) {
                            Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
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
}

@Composable
"""
    # Replace including @Composable keyword above fun TrailerSection
    start_replace = content.rfind('@Composable', 0, start_idx)
    content = content[:start_replace] + new_impl + content[end_idx:]
    with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Implementation updated using index!")
else:
    print("Could not find start or end index.")
