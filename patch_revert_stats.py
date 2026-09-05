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
        Pair("أنمياتي المفضلة", favorites) to Color(0xFF0277BD),
        Pair("اشاهدها حاليا", watching) to Color(0xFF388E3C),
        Pair("ارغب بمشاهدتها", plan) to Color(0xFF1976D2),
        Pair("تم مشاهدتها", completed) to Color(0xFF7B1FA2),
        Pair("اكملها لاحقا", hold) to Color(0xFFFBC02D),
        Pair("لا ارغب بمشاهدتها", dropped) to Color(0xFFD32F2F)
    )

    // Dynamic Global Rating calculation (Ready for Firebase)
    val baseStory = 7.5f + (Math.abs(hash % 20)) / 10f
    val baseChar = 7.0f + (Math.abs((hash / 2) % 25)) / 10f
    val baseArt = 7.5f + (Math.abs((hash / 3) % 20)) / 10f
    val baseMusic = 7.0f + (Math.abs((hash / 4) % 25)) / 10f
    val baseAnim = 7.2f + (Math.abs((hash / 5) % 20)) / 10f
    
    val userFactor = if (userRating != null) (userRating - 8) * 0.1f else 0f
    
    val storyAvg = (baseStory + userFactor).coerceIn(1f, 10f)
    val charAvg = (baseChar + userFactor).coerceIn(1f, 10f)
    val artAvg = (baseArt + userFactor).coerceIn(1f, 10f)
    val musicAvg = (baseMusic + userFactor).coerceIn(1f, 10f)
    val animAvg = (baseAnim + userFactor).coerceIn(1f, 10f)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Detailed Ratings (Replaces the old Bar Chart)
        item {
            Text("متوسط تقييم القصة، الشخصيات، والرسم", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        
        // Old Pie Chart Design
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular pseudo-pie chart
                Box(
                    modifier = Modifier.size(120.dp).border(12.dp, Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(120.dp).padding(12.dp).border(12.dp, Color(0xFF1976D2), CircleShape))
                }
                
                Column(modifier = Modifier.weight(1f).padding(start = 24.dp)) {
                    stats.forEach { (pair, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(text = "${pair.first} : ${pair.second}", color = Color.LightGray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(12.dp).background(color))
                        }
                    }
                }
            }
        }
    }
}

"""

# Ensure RatingColumnUI is preserved if it already exists, otherwise add it if we overwrote it.
# Actually, RatingColumnUI is defined right before AnimeCharactersTabContent in the current code, so it might get overwritten by our end_idx.
# Let's check where RatingColumnUI is.
if "fun RatingColumnUI" in content[start_idx:end_idx]:
    new_func += """
@Composable
fun RatingColumnUI(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color(0xFFFBC02D), fontSize = 16.sp, fontWeight = FontWeight.Bold) // Golden
    }
}
"""

content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AnimeDetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
