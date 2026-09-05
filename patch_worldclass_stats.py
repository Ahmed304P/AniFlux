with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

start_idx = content.find('fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {')
if start_idx == -1:
    print("Function not found!")
    exit(1)

end_idx = content.find('@Composable\nfun AnimeCharactersTabContent', start_idx)
if end_idx == -1:
    print("End not found!")
    exit(1)

new_func = """fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {
    // Generate pseudo-random base stats derived from anime.id (Ready for Firebase replacement)
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 4900 + (hash % 100).toInt()
    val baseWatching = 450 + (hash % 50).toInt()
    val basePlan = 6100 + (hash % 200).toInt()
    val baseCompleted = 2100 + (hash % 100).toInt()
    val baseHold = 200 + (hash % 30).toInt()
    val baseDropped = 400 + (hash % 40).toInt()

    // Add local user's status to the base numbers
    val favorites = baseFavorites + if (savedStatus == "FAVORITE") 1 else 0
    val watching = baseWatching + if (savedStatus == "WATCHING") 1 else 0
    val plan = basePlan + if (savedStatus == "PLAN_TO_WATCH") 1 else 0
    val completed = baseCompleted + if (savedStatus == "COMPLETED") 1 else 0
    val hold = baseHold + if (savedStatus == "ON_HOLD") 1 else 0
    val dropped = baseDropped + if (savedStatus == "DROPPED") 1 else 0

    val stats = listOf(
        Pair("أنمياتي المفضلة", favorites) to Color(0xFF0288D1),
        Pair("اشاهدها حاليا", watching) to Color(0xFF43A047),
        Pair("ارغب بمشاهدتها", plan) to Color(0xFF1976D2),
        Pair("تم مشاهدتها", completed) to Color(0xFF7B1FA2),
        Pair("اكملها لاحقا", hold) to Color(0xFFFBC02D),
        Pair("لا ارغب بمشاهدتها", dropped) to Color(0xFFD32F2F)
    )

    // Dynamic Global Rating calculation (Ready for Firebase)
    val baseStory = 6.90f + (Math.abs(hash % 20)) / 10f
    val baseChar = 7.80f + (Math.abs((hash / 2) % 25)) / 10f
    val baseArt = 7.40f + (Math.abs((hash / 3) % 20)) / 10f
    val baseMusic = 8.20f + (Math.abs((hash / 4) % 25)) / 10f
    val baseAnim = 8.00f + (Math.abs((hash / 5) % 20)) / 10f
    
    val userFactor = if (userRating != null) (userRating - 8) * 0.1f else 0f
    
    val storyAvg = (baseStory + userFactor).coerceIn(1f, 10f)
    val charAvg = (baseChar + userFactor).coerceIn(1f, 10f)
    val artAvg = (baseArt + userFactor).coerceIn(1f, 10f)
    val musicAvg = (baseMusic + userFactor).coerceIn(1f, 10f)
    val animAvg = (baseAnim + userFactor).coerceIn(1f, 10f)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section 1: Detailed Ratings
        item {
            Text(
                text = "متوسط تقييم القصة، الشخصيات، والرسم",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingColumnUI("الموسيقى", String.format(java.util.Locale.US, "%.2f", musicAvg))
                RatingColumnUI("التحريك", String.format(java.util.Locale.US, "%.2f", animAvg))
                RatingColumnUI("الرسم", String.format(java.util.Locale.US, "%.2f", artAvg))
                RatingColumnUI("الشخصيات", String.format(java.util.Locale.US, "%.2f", charAvg))
                RatingColumnUI("القصة", String.format(java.util.Locale.US, "%.2f", storyAvg))
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
        
        // Section 2: Legend and True Donut Chart
        item {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Legend
                    Column(modifier = Modifier.weight(1f)) {
                        stats.forEach { (pair, color) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${pair.second} : ${pair.first}",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Right side: True Donut Chart
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val total = stats.sumOf { it.first.second.toDouble() }.toFloat()
                            val strokeWidth = 45f
                            stats.forEach { (pair, color) ->
                                val sweepAngle = (pair.second / total) * 360f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                )
                                startAngle += sweepAngle
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
        Text(title, color = Color(0xFF9E9E9E), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
"""

content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

