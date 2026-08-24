with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("emptyList()\n    ))", "emptyList())")

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)
