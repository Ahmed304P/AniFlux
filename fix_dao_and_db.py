import re

# Update AniFluxDao
with open("app/src/main/java/com/example/data/local/AniFluxDao.kt", "r") as f:
    dao_content = f.read()

dao_additions = """
    @Query("SELECT * FROM published_anime")
    fun getAllPublishedAnime(): Flow<List<PublishedAnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePublishedAnime(entity: PublishedAnimeEntity)

    @Query("DELETE FROM published_anime WHERE id = :animeId")
    suspend fun removePublishedAnime(animeId: String)
}"""

dao_content = re.sub(r'\}$', dao_additions, dao_content)

with open("app/src/main/java/com/example/data/local/AniFluxDao.kt", "w") as f:
    f.write(dao_content)


# Update AniFluxDatabase
with open("app/src/main/java/com/example/data/local/AniFluxDatabase.kt", "r") as f:
    db_content = f.read()

db_content = db_content.replace(
    "CustomListItemEntity::class, WatchedEpisodeEntity::class]",
    "CustomListItemEntity::class, WatchedEpisodeEntity::class, PublishedAnimeEntity::class]"
).replace("version = 3", "version = 4")

with open("app/src/main/java/com/example/data/local/AniFluxDatabase.kt", "w") as f:
    f.write(db_content)

