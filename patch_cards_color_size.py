with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

start_idx = content.find('fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {')
end_idx = content.find('@Composable\nfun AnimeCharactersTabContent', start_idx)

new_func = """fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {
    // Global Stats Simulation (Ready for Firebase)
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 4818 + (hash % 100).toInt()
    val baseWatching = 418 + (hash % 50).toInt()
    val basePlan = 6018 + (hash % 200).toInt()
    val baseCompleted = 2018 + (hash % 100).toInt()
    val baseHold = 178 + (hash % 30).toInt()
    val baseDropped = 398 + (hash % 40).toInt()

    // Add local user's status to the base numbers (Independent from Detailed Ratings)
    val favorites = baseFavorites + if (savedStatus == "FAVORITE") 1 else 0
    val watching = baseWatching + if (savedStatus == "WATCHING") 1 else 0
    val plan = basePlan + if (savedStatus == "PLAN_TO_WATCH") 1 else 0
    val completed = baseCompleted + if (savedStatus == "COMPLETED") 1 else 0
    val hold = baseHold + if (savedStatus == "ON_HOLD") 1 else 0
    val dropped = baseDropped + if (savedStatus == "DROPPED") 1 else 0

    val stats = listOf(
        Pair("أنمياتي المفضلة", favorites) to Color(0xFF64B5F6),
        Pair("اشاهدها حاليا", watching) to Color(0xFF81C784),
        Pair("ارغب بمشاهدتها", plan) to Color(0xFFE57373),
        Pair("تم مشاهدتها", completed) to Color(0xFFBA68C8),
        Pair("اكملها لاحقا", hold) to Color(0xFFFFCA28),
        Pair("لا ارغب بمشاهدتها", dropped) to Color(0xFFF06292)
    )

    // Global Ratings Simulation (Independent of user interaction)
    val storyAvg = 9.50f - (Math.abs(hash % 15)) / 10f
    val charAvg = 9.20f - (Math.abs((hash / 2) % 15)) / 10f
    val artAvg = 9.80f - (Math.abs((hash / 3) % 15)) / 10f

    // Native app colors (Using MaterialTheme surface/surfaceVariant for perfect match)
    val cardColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    val borderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        // Card 1: Detailed Ratings
        item {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "متوسط تقييم القصة، الشخصيات، والرسم",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        RatingColumnUI("القصة", String.format(java.util.Locale.US, "%.2f", storyAvg))
                        RatingColumnUI("الشخصيات", String.format(java.util.Locale.US, "%.2f", charAvg))
                        RatingColumnUI("الرسم", String.format(java.util.Locale.US, "%.2f", artAvg))
                    }
                }
            }
        }

        // Card 2: Donut Chart
        item {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right side in RTL (Donut Chart)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(110.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                val total = stats.sumOf { it.first.second.toDouble() }.toFloat()
                                val strokeWidth = 30f // Normal stroke
                                stats.forEach { (pair, color) ->
                                    val sweepAngle = (pair.second / total) * 360f
                                    if (sweepAngle > 0) {
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth
                                            )
                                        )
                                    }
                                    startAngle += sweepAngle
                                }
                            }
                        }
                    }

                    // Left side in RTL (Legend)
                    Column(modifier = Modifier.weight(1.2f)) {
                        stats.forEach { (pair, color) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${pair.first}: ${pair.second}",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 12.sp 
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingColumnUI(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) 
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold) 
    }
}
"""

content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
