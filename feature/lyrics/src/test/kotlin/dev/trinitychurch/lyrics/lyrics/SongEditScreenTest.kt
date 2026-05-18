package dev.trinitychurch.lyrics.lyrics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.db.TrinityLyricsDatabase
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SongEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: SongRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrinityLyricsDatabase.Schema.create(driver)
        val db = TrinityLyricsDatabase(driver)
        repository = SongRepository(db, driver)
    }

    @Test
    fun `fill title and add section then save inserts song into repository`() {
        var navigated = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SongEditScreen(
                    repository = repository,
                    songId = null,
                    onNavigateBack = { navigated = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("field_title").performTextInput("Amazing Grace")
        composeTestRule.onNodeWithTag("add_section_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_save").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigated }

        val songs = runBlocking { repository.allSongs().first() }
        assert(songs.any { it.title == "Amazing Grace" }) {
            "Expected 'Amazing Grace' in library but found: ${songs.map { it.title }}"
        }
    }

    @Test
    fun `save with empty title shows error and does not navigate`() {
        var navigated = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SongEditScreen(
                    repository = repository,
                    songId = null,
                    onNavigateBack = { navigated = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("error_title").assertIsDisplayed()
        assert(!navigated) { "Should not navigate when title is empty" }
    }

    @Test
    fun `save with no sections shows sections error and does not navigate`() {
        var navigated = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SongEditScreen(
                    repository = repository,
                    songId = null,
                    onNavigateBack = { navigated = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("field_title").performTextInput("Title Only")
        composeTestRule.onNodeWithTag("btn_save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("error_sections").assertIsDisplayed()
        assert(!navigated) { "Should not navigate when sections list is empty" }
    }

    @Test
    fun `back with unsaved title shows discard dialog`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SongEditScreen(
                    repository = repository,
                    songId = null,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("field_title").performTextInput("Unsaved Song")
        composeTestRule.onNodeWithTag("btn_back").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("confirm_discard").assertIsDisplayed()
    }
}
