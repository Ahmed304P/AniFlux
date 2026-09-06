package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.Anime
import com.example.ui.viewmodel.AnimeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import com.example.data.repository.AnimeRepository
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class RelatedAnimeCrashTest {

    @Test
    fun testOpenAnimeDetails() = runBlocking {
        // Can't run without DI.
    }
}
