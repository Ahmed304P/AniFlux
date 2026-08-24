import re

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "r") as f:
    content = f.read()

# Update imports
imports = """
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.data.model.Person
import com.example.data.model.Anime
"""
content = content.replace("import com.example.util.AniListService", imports + "\nimport com.example.util.AniListService")

# Update viewModel param if missing
if "viewModel: com.example.ui.viewmodel.AnimeViewModel" not in content:
    content = content.replace("fun AdminAnimeEditScreen(", "fun AdminAnimeEditScreen(viewModel: com.example.ui.viewmodel.AnimeViewModel, ")

# Update state variables
content = content.replace('var characters by remember { mutableStateOf("") }', 'var characters by remember { mutableStateOf<List<Person>>(emptyList()) }')
content = content.replace('var staff by remember { mutableStateOf("") }', 'var staff by remember { mutableStateOf<List<Person>>(emptyList()) }')
content = content.replace('var trailer by remember { mutableStateOf("") }', 'var trailerUrl by remember { mutableStateOf("") }')

# Fix population logic
content = content.replace('characters = anime.characters', 'characters = anime.characters')
content = content.replace('staff = anime.staff', 'staff = anime.staff')
content = content.replace('trailer = anime.trailer', 'trailerUrl = anime.trailerUrl')

# Fix AniList mapping
content = content.replace('characters = anime.characters', 'characters = anime.charactersList')
content = content.replace('staff = anime.staff', 'staff = anime.staffList')
content = content.replace('trailer = anime.trailer', 'trailerUrl = anime.trailer')

# Replace UI for Characters and staff
ui_replace = """
            Text("الطاقم والشخصيات", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (characters.isNotEmpty()) {
                Text("الشخصيات:", color = Color.LightGray, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(characters) { person ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("لا توجد شخصيات محملة", color = Color.Gray)
            }
            
            if (staff.isNotEmpty()) {
                Text("طاقم العمل:", color = Color.LightGray, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(staff) { person ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("لا يوجد طاقم عمل محمل", color = Color.Gray)
            }
"""

content = re.sub(
    r'Text\("الطاقم والشخصيات".*?AdminTextField\("طاقم العمل", staff\) \{ staff = it \}',
    ui_replace.strip(),
    content,
    flags=re.DOTALL
)

content = content.replace('AdminTextField("رابط العرض الدعائي (Trailer URL)", trailer) { trailer = it }', 'AdminTextField("رابط العرض الدعائي (Trailer URL)", trailerUrl) { trailerUrl = it }')

save_logic = """
                    val newAnime = Anime(
                        id = animeId ?: java.util.UUID.randomUUID().toString(),
                        titleAr = titleAr,
                        titleEn = titleEn,
                        posterUrl = posterUrl,
                        bannerUrl = bannerUrl,
                        rating = rating.toDoubleOrNull() ?: 0.0,
                        totalEpisodes = 12,
                        currentEpisodes = 1,
                        status = status,
                        type = type,
                        year = year.toIntOrNull() ?: 2024,
                        studio = studio,
                        synopsisAr = synopsisAr,
                        genres = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        releaseDay = "الأحد",
                        season = season,
                        ageRating = ageRating,
                        isCommentsLocked = isCommentsLocked,
                        characters = characters,
                        staff = staff,
                        trailerUrl = trailerUrl
                    )
                    viewModel.saveAnime(newAnime)
"""
content = content.replace("delay(2000) // Mock Firebase Add/Update", delay_logic := f"delay(500)\n{save_logic}")

with open("app/src/main/java/com/example/ui/screens/AdminAnimeEditScreen.kt", "w") as f:
    f.write(content)
