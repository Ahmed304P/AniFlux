with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

import re

# We need to replace the entire AnimeStatsTabContent function.
# Let's find its start and end.
start_idx = content.find('fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?)')
if start_idx == -1:
    print("Function not found!")
    exit(1)

# Find the end of the function.
# It ends right before '@Composable\nfun AnimeCharactersTabContent'
end_idx = content.find('@Composable\nfun AnimeCharactersTabContent', start_idx)
if end_idx == -1:
    print("End not found!")
    exit(1)

new_func = """fun AnimeStatsTabContent(anime: Anime, isSaved: Boolean, savedStatus: String?, userRating: Int?) {
    // Generate pseudo-random base stats derived from anime.id
    val hash = anime.id.hashCode().toLong()
    val baseFavorites = 5071 + (hash % 100).toInt()
    val baseWatching = 517 + (hash % 50).toInt()
    val basePlan = 6229 + (hash % 200).toInt()
    val baseCompleted = 2200 + (hash % 100).toInt()
    val baseHold = 239 + (hash % 30).toInt()
    val baseDropped = 415 + (hash % 40).toInt()

    // Add local user's status to the base numbers
    val favorites = baseFavorites + if (savedStatus == "FAVORITE") 1 else 0
    val watching = baseWatching + if (savedStatus == "WATCHING") 1 else 0
    val plan = basePlan + if (savedStatus == "PLAN_TO_WATCH") 1 else 0
    val completed = baseCompleted + if (savedStatus == "COMPLETED") 1 else 0
    val hold = baseHold + if (savedStatus == "ON_HOLD") 1 else 0
    val dropped = baseDropped + if (savedStatus == "DROPPED") 1 else 0

    val stats = listOf(
        Pair("المانجا المفضلة", favorites) to Color(0xFF64B5F6), // Light blue
        Pair("أقرأها حاليا", watching) to Color(0xFF81C784), // Light green
        Pair("ارغب بقراءتها", plan) to Color(0xFFE57373), // Red/Pink
        Pair("تم قراءتها", completed) to Color(0xFFBA68C8), // Purple
        Pair("لا ارغب بقراءتها", dropped) to Color(0xFFF06292) // Dark Pink
    )
    
    // Dynamic Global Rating calculation
    val baseStory = 7.5f + (Math.abs(hash % 20)) / 10f
    val baseChar = 7.0f + (Math.abs((hash / 2) % 25)) / 10f
    val baseArt = 7.5f + (Math.abs((hash / 3) % 20)) / 10f
    val baseMusic = 7.0f + (Math.abs((hash / 4) % 25)) / 10f
    val baseAnim = 7.2f + (Math.abs((hash / 5) % 20)) / 10f
    
    // Adjust slightly based on user rating to simulate global calculation via Firebase
    val userFactor = if (userRating != null) (userRating - 8) * 0.1f else 0f
    
    val storyAvg = (baseStory + userFactor).coerceIn(1f, 10f)
    val charAvg = (baseChar + userFactor).coerceIn(1f, 10f)
    val artAvg = (baseArt + userFactor).coerceIn(1f, 10f)
    val musicAvg = (baseMusic + userFactor).coerceIn(1f, 10f)
    val animAvg = (baseAnim + userFactor).coerceIn(1f, 10f)

    val cardColor = Color(0xFF1E1E28)
    val borderColor = Color(0xFF2D2D3D)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Bar Chart Card
        item {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تصويتات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (i in 1..10) {
                            val isUserVote = userRating == i
                            val heightRatio = if (i == 10) 1f else (Math.abs(hash % (i * 10)) / 100f).coerceIn(0.1f, 0.6f)
                            val finalHeightRatio = if (isUserVote) heightRatio + 0.2f else heightRatio
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(finalHeightRatio.coerceIn(0.1f, 1f))
                                        .background(if (isUserVote) Color(0xFFE53935) else Color(0xFF3F3F5A), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "$i", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Pie Chart Card
        item {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        stats.forEach { (pair, color) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.End, modifier2 = Modifier.fillMaxWidth()) {
                                Text(text = "${pair.first} : ${pair.second}", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing pseudo pie chart with canvas
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = 0f
                            val total = stats.sumOf { it.first.second.toDouble() }.toFloat()
                            stats.forEach { (pair, color) ->
                                val sweepAngle = (pair.second / total) * 360f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40f)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }
                }
            }
        }
        
        // 3. Detailed Ratings Card
        item {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "متوسط تقييم القصة، الشخصيات، الرسم، التحريك، والموسيقى",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RatingColumnUI("الموسيقى", String.format(java.util.Locale.US, "%.2f", musicAvg))
                        RatingColumnUI("التحريك", String.format(java.util.Locale.US, "%.2f", animAvg))
                        RatingColumnUI("الرسم", String.format(java.util.Locale.US, "%.2f", artAvg))
                        RatingColumnUI("الشخصيات", String.format(java.util.Locale.US, "%.2f", charAvg))
                        RatingColumnUI("القصة", String.format(java.util.Locale.US, "%.2f", storyAvg))
                    }
                }
            }
        }
    }
}

@Composable
fun RatingColumnUI(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color(0xFFFBC02D), fontSize = 16.sp, fontWeight = FontWeight.Bold) // Golden
    }
}
"""

content = content[:start_idx] + new_func + '\n' + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

