package dev.trinitychurch.lyrics.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import org.junit.Rule
import org.junit.Test

class PresentationWindowAppSpec {

    @get:Rule
    val rule = createComposeRule()

    private val config = SlideConfig()

    private fun storeWithTwoLineSlide(): PresentationStateStore {
        val store = PresentationStateStore()
        val song = Song(
            id = "song-1",
            title = "Test Song",
            sections = listOf(
                SongSection(
                    id = "sec-1",
                    songId = "song-1",
                    label = "Verse 1",
                    type = SectionType.VERSE,
                    body = "Line one\nLine two",
                    sortOrder = 1,
                )
            ),
        )
        val set = ServiceSet(
            id = "set-1",
            name = "Test Set",
            items = listOf(SetItem(id = "item-1", setId = "set-1", songId = "song-1", sortOrder = 0)),
        )
        store.loadSet(set, mapOf("song-1" to song), config)
        return store
    }

    @Test
    fun `idle state shows black screen with no slide content`() {
        val store = PresentationStateStore()

        rule.setContent {
            PresentationWindowApp(store = store, config = config)
        }

        rule.onAllNodesWithTag("lyrics_slide_view").assertCountEquals(0)
    }

    @Test
    fun `lyrics state with 2-line slide renders both lines`() {
        val store = storeWithTwoLineSlide()

        rule.setContent {
            PresentationWindowApp(store = store, config = config)
        }

        rule.onNodeWithTag("lyrics_slide_view").assertIsDisplayed()
        rule.onNodeWithTag("slide_line_0").assertTextContains("Line one")
        rule.onNodeWithTag("slide_line_1").assertTextContains("Line two")
    }

    @Test
    fun `blank state shows black screen with no slide content`() {
        val store = storeWithTwoLineSlide()
        store.toggleBlank()

        rule.setContent {
            PresentationWindowApp(store = store, config = config)
        }

        rule.onAllNodesWithTag("lyrics_slide_view").assertCountEquals(0)
    }
}
