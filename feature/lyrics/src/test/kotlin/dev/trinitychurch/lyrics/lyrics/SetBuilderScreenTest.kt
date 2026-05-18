package dev.trinitychurch.lyrics.lyrics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.db.SetRepository
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.db.TrinityLyricsDatabase
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.presentation.PresentationStateStore
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class SetBuilderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var songRepository: SongRepository
    private lateinit var setRepository: SetRepository
    private lateinit var presentationStore: PresentationStateStore

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrinityLyricsDatabase.Schema.create(driver)
        val db = TrinityLyricsDatabase(driver)
        songRepository = SongRepository(db, driver)
        setRepository = SetRepository(db)
        presentationStore = PresentationStateStore()
    }

    @Test
    fun `add three songs to set then drag to reorder shows new order`() {
        val song1 = testSong("s1", "Song Alpha")
        val song2 = testSong("s2", "Song Beta")
        val song3 = testSong("s3", "Song Gamma")
        val setId = "set-1"
        runBlocking {
            songRepository.insert(song1)
            songRepository.insert(song2)
            songRepository.insert(song3)
            setRepository.createSet(
                ServiceSet(
                    id = setId,
                    name = "Sunday Service",
                    items = listOf(
                        SetItem("item1", setId, "s1", 0),
                        SetItem("item2", setId, "s2", 1),
                        SetItem("item3", setId, "s3", 2)
                    )
                )
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SetBuilderScreen(
                    setRepository = setRepository,
                    songRepository = songRepository,
                    presentationStore = presentationStore,
                    setId = setId,
                    onNavigateBack = {},
                    onStartPresentation = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("set_item_card_0").fetchSemanticsNodes().isNotEmpty()
        }

        val handle0Bounds = composeTestRule.onNodeWithTag("set_item_drag_0")
            .fetchSemanticsNode().boundsInRoot
        val card1Bounds = composeTestRule.onNodeWithTag("set_item_card_1")
            .fetchSemanticsNode().boundsInRoot
        val dragDelta = card1Bounds.center.y - handle0Bounds.center.y + card1Bounds.height / 2f

        composeTestRule.onNodeWithTag("set_item_drag_0").performTouchInput {
            down(center)
            advanceEventTime(700L)
            repeat(20) { moveBy(Offset(0f, dragDelta / 20f)) }
            up()
        }
        composeTestRule.waitForIdle()

        val updatedSet = runBlocking { setRepository.setById(setId).first() }
        val orderedSongIds = updatedSet?.items?.sortedBy { it.sortOrder }?.map { it.songId }
        assert(orderedSongIds?.first() != "s1") {
            "Expected s1 to have moved from first position, got order: $orderedSongIds"
        }
    }

    @Test
    fun `remove item from set does not delete song from library`() {
        val song = testSong("s1", "Amazing Grace")
        val setId = "set-1"
        runBlocking {
            songRepository.insert(song)
            setRepository.createSet(
                ServiceSet(
                    id = setId,
                    name = "Test Set",
                    items = listOf(SetItem("item1", setId, "s1", 0))
                )
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SetBuilderScreen(
                    setRepository = setRepository,
                    songRepository = songRepository,
                    presentationStore = presentationStore,
                    setId = setId,
                    onNavigateBack = {},
                    onStartPresentation = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("set_item_card_0").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("set_item_delete_0").performClick()
        composeTestRule.waitForIdle()

        val songs = runBlocking { songRepository.allSongs().first() }
        assert(songs.any { it.id == "s1" }) {
            "Song 's1' should still be in the library after being removed from the set"
        }
    }

    @Test
    fun `start presentation with empty set shows guard message and does not navigate`() {
        var navigated = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SetBuilderScreen(
                    setRepository = setRepository,
                    songRepository = songRepository,
                    presentationStore = presentationStore,
                    setId = null,
                    onNavigateBack = {},
                    onStartPresentation = { navigated = true }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("btn_start_presentation").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("btn_start_presentation").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("set_empty_guard").assertIsDisplayed()
        assert(!navigated) { "Should not navigate when set is empty" }
    }

    private fun testSong(id: String, title: String): Song = Song(
        id = id,
        title = title,
        sections = listOf(
            SongSection(
                id = UUID.randomUUID().toString(),
                songId = id,
                label = "Estrofe 1",
                type = SectionType.VERSE,
                body = "Test lyrics",
                sortOrder = 0
            )
        )
    )
}
