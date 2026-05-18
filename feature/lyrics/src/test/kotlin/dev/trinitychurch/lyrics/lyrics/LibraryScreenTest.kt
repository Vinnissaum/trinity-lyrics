package dev.trinitychurch.lyrics.lyrics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.db.TrinityLyricsDatabase
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class LibraryScreenTest {

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
    fun `shows all three songs in library`() {
        runBlocking {
            repository.insert(testSong("id1", "Amazing Grace"))
            repository.insert(testSong("id2", "How Great Thou Art"))
            repository.insert(testSong("id3", "Oceans"))
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                LibraryScreen(
                    repository = repository,
                    onNavigateToEditor = {},
                    onNavigateToImport = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Amazing Grace"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Amazing Grace").assertIsDisplayed()
        composeTestRule.onNodeWithText("How Great Thou Art").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oceans").assertIsDisplayed()
    }

    @Test
    fun `search filters songs by title`() {
        runBlocking {
            repository.insert(testSong("id1", "Amazing Grace"))
            repository.insert(testSong("id2", "How Great Thou Art"))
            repository.insert(testSong("id3", "Oceans"))
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                LibraryScreen(
                    repository = repository,
                    onNavigateToEditor = {},
                    onNavigateToImport = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Amazing Grace"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Grace")

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("How Great Thou Art"))
                .fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithText("Amazing Grace").assertIsDisplayed()
    }

    @Test
    fun `clear search shows all songs again`() {
        runBlocking {
            repository.insert(testSong("id1", "Amazing Grace"))
            repository.insert(testSong("id2", "Oceans"))
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                LibraryScreen(
                    repository = repository,
                    onNavigateToEditor = {},
                    onNavigateToImport = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Amazing Grace"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("Grace")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Oceans")).fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithTag("search_field").performTextClearance()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Oceans")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Amazing Grace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oceans").assertIsDisplayed()
    }

    @Test
    fun `soft-deleted song does not appear`() {
        runBlocking {
            repository.insert(testSong("id1", "Amazing Grace"))
            repository.insert(testSong("id2", "Oceans"))
            repository.softDelete("id1")
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                LibraryScreen(
                    repository = repository,
                    onNavigateToEditor = {},
                    onNavigateToImport = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Oceans"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Oceans").assertIsDisplayed()
        assert(
            composeTestRule.onAllNodesWithTag("song_card_id1")
                .fetchSemanticsNodes().isEmpty()
        ) { "Soft-deleted song should not appear in library" }
    }

    private fun testSong(id: String, title: String, artist: String = ""): Song {
        val sectionId = UUID.randomUUID().toString()
        return Song(
            id = id,
            title = title,
            artist = artist,
            sections = listOf(
                SongSection(
                    id = sectionId,
                    songId = id,
                    label = "Estrofe 1",
                    type = SectionType.VERSE,
                    body = "Test lyrics line 1\nTest lyrics line 2",
                    sortOrder = 0
                )
            )
        )
    }
}
