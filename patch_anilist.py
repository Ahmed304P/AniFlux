import sys

try:
    with open("app/src/main/java/com/example/util/AniListService.kt", "r") as f:
        content = f.read()

    # 1. Add studio to AniListResult
    target_result = """    val charactersList: List<Person>,
    val staffList: List<Person>,
    val trailer: String
)"""
    replacement_result = """    val charactersList: List<Person>,
    val staffList: List<Person>,
    val trailer: String,
    val studio: String = ""
)"""
    content = content.replace(target_result, replacement_result)

    # 2. Add studio to GraphQL query
    target_query = """                  characters(sort: ROLE, perPage: 5) {"""
    replacement_query = """                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                  characters(sort: ROLE, perPage: 5) {"""
    content = content.replace(target_query, replacement_query)

    # 3. Parse studio
    target_parse = """                    // Parse Characters"""
    replacement_parse = """                    // Parse Studio
                    val studiosNodes = item.optJSONObject("studios")?.optJSONArray("nodes")
                    var studioName = ""
                    if (studiosNodes != null && studiosNodes.length() > 0) {
                        studioName = studiosNodes.optJSONObject(0)?.optString("name", "") ?: ""
                    }

                    // Parse Characters"""
    content = content.replace(target_parse, replacement_parse)

    # 4. Add studio to AniListResult initialization
    target_init = """                            trailer = trailerUrl
                        )"""
    replacement_init = """                            trailer = trailerUrl,
                            studio = studioName
                        )"""
    content = content.replace(target_init, replacement_init)
    
    # 5. Fix catch block
    target_catch = """        } catch (e: IOException) {"""
    replacement_catch = """        } catch (e: Exception) {"""
    content = content.replace(target_catch, replacement_catch)

    with open("app/src/main/java/com/example/util/AniListService.kt", "w") as f:
        f.write(content)
    print("Success: AniListService patched")

except Exception as e:
    print(f"Error: {e}")
