with open("app/src/main/java/com/example/data/model/Models.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.data.model.Person\npackage com.example.data.model", "package com.example.data.model")

with open("app/src/main/java/com/example/data/model/Models.kt", "w") as f:
    f.write(content)
