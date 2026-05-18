package dev.trinitychurch.lyrics.importer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.db.TrinityLyricsDatabase
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ImportWizardScreenTest {

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
    fun `holyrics path - mock parser returning 3 songs shows count in preview`() {
        val fakeSongs = listOf(
            testSong("id1", "Song One"),
            testSong("id2", "Song Two"),
            testSong("id3", "Song Three")
        )
        val fakeParser = object : HolyricsSongParser() {
            override fun parse(jsonText: String): List<Song> = fakeSongs
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                ImportWizardScreen(
                    songRepository = repository,
                    parser = fakeParser,
                    fileReader = { "fake_content" },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_holyrics").performClick()
        composeTestRule.onNodeWithTag("btn_select_file").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("text_found_songs"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("text_found_songs")
            .assertTextContains("3", substring = true)
    }

    @Test
    fun `confirm import inserts all songs into repository`() {
        val fakeSongs = listOf(
            testSong("id1", "Song One"),
            testSong("id2", "Song Two"),
            testSong("id3", "Song Three")
        )
        val fakeParser = object : HolyricsSongParser() {
            override fun parse(jsonText: String): List<Song> = fakeSongs
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                ImportWizardScreen(
                    songRepository = repository,
                    parser = fakeParser,
                    fileReader = { "fake_content" },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_holyrics").performClick()
        composeTestRule.onNodeWithTag("btn_select_file").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("btn_confirm_import"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("btn_confirm_import").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("text_import_complete"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("text_import_complete").assertIsDisplayed()
        runBlocking {
            val allSongs = repository.allSongs().first()
            assert(allSongs.size == 3) { "Expected 3 songs but got ${allSongs.size}" }
        }
    }

    @Test
    fun `parser throwing exception shows error UI without crashing`() {
        val errorParser = object : HolyricsSongParser() {
            override fun parse(jsonText: String): List<Song> =
                throw HolyricsParseException("Test parse failure")
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                ImportWizardScreen(
                    songRepository = repository,
                    parser = errorParser,
                    fileReader = { "bad_content" },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_holyrics").performClick()
        composeTestRule.onNodeWithTag("btn_select_file").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("text_import_error")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("text_import_error").assertIsDisplayed()
    }

    @Test
    fun `duplicate song shows skip overwrite options and skip action keeps existing song unchanged`() {
        // Insert a song that will be a duplicate
        val existingSong = testSong("existing-id", "Amazing Grace", "John Newton")
        runBlocking { repository.insert(existingSong) }

        // Parser returns a song with same title+artist (new ID)
        val newVersionId = UUID.randomUUID().toString()
        val duplicateSong = testSong(newVersionId, "Amazing Grace", "John Newton")
        val fakeParser = object : HolyricsSongParser() {
            override fun parse(jsonText: String): List<Song> = listOf(duplicateSong)
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                ImportWizardScreen(
                    songRepository = repository,
                    parser = fakeParser,
                    fileReader = { "fake_content" },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_holyrics").performClick()
        composeTestRule.onNodeWithTag("btn_select_file").performClick()

        // Duplicate warning should be visible
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("text_duplicate_warning"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("text_duplicate_warning").assertIsDisplayed()

        // Default action is SKIP — confirm should not insert the duplicate
        composeTestRule.onNodeWithTag("btn_confirm_import").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("text_import_complete"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Only 1 song in DB — the original, not a second copy
        runBlocking {
            val allSongs = repository.allSongs().first()
            assert(allSongs.size == 1) { "Expected 1 song (existing), got ${allSongs.size}" }
            assert(allSongs.first().id == "existing-id") { "Original song ID should be preserved" }
        }
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
            ),
            source = "holyrics"
        )
    }
}
