import re

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "r") as f:
    content = f.read()

imports = """import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.local.PublishedAnimeEntity
"""

content = content.replace("import com.example.data.model.StreamServer\n", "import com.example.data.model.StreamServer\n" + imports)

moshi_setup = """    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val animeAdapter = moshi.adapter(Anime::class.java)

    val publishedAnimeFlow: Flow<List<Anime>> = dao.getAllPublishedAnime().map { entities ->
        entities.mapNotNull {
            try {
                animeAdapter.fromJson(it.animeJson)
            } catch(e: Exception) {
                null
            }
        }
    }

    suspend fun savePublishedAnime(anime: Anime) {
        val json = animeAdapter.toJson(anime)
        dao.savePublishedAnime(PublishedAnimeEntity(anime.id, json))
    }
    
    suspend fun removePublishedAnime(animeId: String) {
        dao.removePublishedAnime(animeId)
    }
"""

content = content.replace("class AnimeRepository(private val dao: AniFluxDao) {", "class AnimeRepository(private val dao: AniFluxDao) {\n" + moshi_setup)

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "w") as f:
    f.write(content)

