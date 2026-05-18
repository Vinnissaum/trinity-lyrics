package dev.trinitychurch.lyrics.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OperatorConsoleAppSpec {

    @get:Rule
    val rule = createComposeRule()

    private fun createStoreWith5Slides(): PresentationStateStore {
        val store = PresentationStateStore()
        val sections = (1..5).map { i ->
            SongSection(
                id = "sec-$i",
                songId = "song-1",
                label = "Verse $i",
                type = SectionType.VERSE,
                body = "Slide text $i",
                sortOrder = i,
            )
        }
        val song = Song(id = "song-1", title = "Test Song", sections = sections)
        val set = ServiceSet(
            id = "set-1",
            name = "Test Set",
            items = listOf(SetItem(id = "item-1", setId = "set-1", songId = "song-1", sortOrder = 0)),
        )
        store.loadSet(set, mapOf("song-1" to song), SlideConfig())
        return store
    }

    @Test
    fun `lyrics state with 5 slides shows slide 0 text in current panel`() {
        val store = createStoreWith5Slides()

        rule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                OperatorConsoleApp(store = store, onExit = {})
            }
        }

        rule.onNodeWithTag("current_slide_panel").assertTextContains("Slide text 1")
    }

    @Test
    fun `advance updates current panel and slide counter`() {
        val store = createStoreWith5Slides()

        rule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                OperatorConsoleApp(store = store, onExit = {})
            }
        }

        rule.runOnIdle { store.advance() }

        rule.onNodeWithTag("current_slide_panel").assertTextContains("Slide text 2")
        rule.onNodeWithTag("slide_progress").assertTextContains("2 de 5")
    }

    @Test
    fun `blank button click toggles store to blank state`() {
        val store = createStoreWith5Slides()

        rule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                OperatorConsoleApp(store = store, onExit = {})
            }
        }

        rule.onNodeWithTag("blank_button").performClick()

        assertTrue(store.state.value is PresentationState.Blank)
    }
}
