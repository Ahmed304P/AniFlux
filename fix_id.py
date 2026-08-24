import re

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "class AnimeViewModel(application: Application) : AndroidViewModel(application) {",
    "class AnimeViewModel(application: Application) : AndroidViewModel(application) {\n\n    var adminEditingAnimeId: String? = null"
)

with open("app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    main = f.read()

main = main.replace(
    '''                        onEditAnime = { id -> 
                            // In a real app we'd pass ID through ViewModel or Navigation Args
                            // For now we just route to edit screen
                            viewModel.setSelectedTab(22) 
                        },''',
    '''                        onEditAnime = { id -> 
                            viewModel.adminEditingAnimeId = id
                            viewModel.setSelectedTab(22) 
                        },'''
)

main = main.replace(
    '''                    22 -> AdminAnimeEditScreen(viewModel, 
                        animeId = "mock_id", // For mock purposes
                        onBack = { viewModel.setSelectedTab(21) },
                        onManageEpisodes = { id -> viewModel.setSelectedTab(23) }
                    )''',
    '''                    22 -> AdminAnimeEditScreen(viewModel, 
                        animeId = viewModel.adminEditingAnimeId,
                        onBack = { viewModel.setSelectedTab(21) },
                        onManageEpisodes = { id -> viewModel.setSelectedTab(23) }
                    )'''
)

main = main.replace(
    '''                    23 -> AdminEpisodeManagerScreen(
                        animeId = "mock_id",
                        onBack = { viewModel.setSelectedTab(22) }
                    )''',
    '''                    23 -> AdminEpisodeManagerScreen(
                        animeId = viewModel.adminEditingAnimeId ?: "mock_id",
                        onBack = { viewModel.setSelectedTab(22) }
                    )'''
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(main)

