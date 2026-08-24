import sys

with open("app/src/main/java/com/example/data/model/Models.kt", "r") as f:
    content = f.read()

content = content.replace("val serverId: String? = null", "val serverId: String? = null,\n    val customEpisodes: Map<String, String>? = null")

with open("app/src/main/java/com/example/data/model/Models.kt", "w") as f:
    f.write(content)

print("Patched Models.kt")
