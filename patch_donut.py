with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

import re

start_idx = content.find('fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {')
end_idx = content.find('@Composable\nfun AnimeCharactersTabContent', start_idx)

new_func = """fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {
    // Simulated Global Stats (Ready for Firebase)
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 4900 + (hash % 100).toInt()
    val baseWatching = 450 + (hash % 50).toInt()
    val basePlan = 6100 + (hash % 200).toInt()
    val baseCompleted = 2100 + (hash % 100).toInt()
    val baseHold = 200 + (hash % 30).toInt()
    val baseDropped = 400 + (hash % 40).toInt()

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

    // Global Ratings Simulation (Independent of local user action)
    val storyAvg = 6.90f + (Math.abs(hash % 20)) / 10f
    val charAvg = 7.80f + (Math.abs((hash / 2) % 25)) / 10f
    val artAvg = 7.40f + (Math.abs((hash / 3) % 20)) / 10f
    val musicAvg = 8.20f + (Math.abs((hash / 4) % 25)) / 10f
    val animAvg = 8.00f + (Math.abs((hash / 5) % 20)) / 10f
    
    val totalVotes = favorites + watching + plan + completed + hold + dropped

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "التقييمات التفصيلية العالمية",
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
        
        item {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Legend
                    Column(modifier = Modifier.weight(1f)) {
                        stats.forEach { (pair, color) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Box(modifier = Modifier.size(14.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${pair.second} : ${pair.first}",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Center Donut Chart with Total Votes
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val total = stats.sumOf { it.first.second.toDouble() }.toFloat()
                            val strokeWidth = 35f
                            stats.forEach { (pair, color) ->
                                val sweepAngle = (pair.second / total) * 360f
                                if (sweepAngle > 0) {
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle - 2f, // Small gap
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                                startAngle += sweepAngle
                            }
                        }
                        // Total Votes in the center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "المتفاعلين",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "%,d".format(totalVotes),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
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

