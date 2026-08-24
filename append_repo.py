import sys

content = """
    // --- Restored Missing Methods ---
    fun getAllSavedAnime(): Flow<List<SavedAnimeEntity>> = dao.getAllSavedAnime()
    
    fun getSavedAnimeById(animeId: String): Flow<SavedAnimeEntity?> = dao.getSavedAnimeById(animeId)
    
    suspend fun setAnimeSavedStatus(animeId: String, status: String?) {
        if (status == null) {
            dao.removeSavedAnime(animeId)
        } else {
            dao.saveAnime(SavedAnimeEntity(animeId, status, System.currentTimeMillis()))
        }
    }
    
    suspend fun removeSavedAnime(animeId: String) = dao.removeSavedAnime(animeId)
    
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>> = dao.getAllWatchHistory()
    
    suspend fun removeWatchHistoryByAnime(animeId: String) = dao.removeWatchHistoryByAnime(animeId)
    
    suspend fun recordWatchProgress(animeId: String, episodeNumber: Int, currentSeconds: Int, totalSeconds: Int) {
        val historyId = "${animeId}_${episodeNumber}"
        val existing = dao.getEpisodeHistory(historyId)
        val entity = WatchHistoryEntity(
            historyId = historyId,
            animeId = animeId,
            episodeNumber = episodeNumber,
            currentSeconds = currentSeconds,
            totalSeconds = totalSeconds,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        dao.saveWatchHistory(entity)
    }
    
    fun getLocalComments(animeId: String, episodeNumber: Int): Flow<List<LocalCommentEntity>> {
        return dao.getCommentsForEpisode(animeId, episodeNumber)
    }
}
"""

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "r") as f:
    orig = f.read()

# Replace the last brace with the new methods
if orig.strip().endswith("}"):
    orig = orig.strip()[:-1] + content

with open("app/src/main/java/com/example/data/repository/AnimeRepository.kt", "w") as f:
    f.write(orig)

print("Appended missing methods to AnimeRepository.kt")
