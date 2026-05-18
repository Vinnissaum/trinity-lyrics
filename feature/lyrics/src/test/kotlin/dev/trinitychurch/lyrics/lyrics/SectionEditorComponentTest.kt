package dev.trinitychurch.lyrics.lyrics

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import org.junit.Rule
import org.junit.Test

class SectionEditorComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `add two sections and both appear in list`() {
        var sections by mutableStateOf(emptyList<SongSection>())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SectionEditorComponent(
                    sections = sections,
                    onSectionsChanged = { sections = it },
                    songId = "song-test",
                    modifier = Modifier.size(800.dp, 600.dp)
                )
            }
        }

        composeTestRule.onNodeWithTag("add_section_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_section_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("section_card_0").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("section_card_1").assertCountEquals(1)
        assert(sections.size == 2) { "Expected 2 sections but got ${sections.size}" }
    }

    @Test
    fun `changing body text triggers preview update`() {
        val section = SongSection(
            id = "s1",
            songId = "song1",
            label = "Estrofe 1",
            type = SectionType.VERSE,
            body = "",
            sortOrder = 0
        )
        var sections by mutableStateOf(listOf(section))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SectionEditorComponent(
                    sections = sections,
                    onSectionsChanged = { sections = it },
                    modifier = Modifier.size(800.dp, 600.dp)
                )
            }
        }

        composeTestRule.onNodeWithTag("section_body_0").performTextInput("Line one\nLine two")
        // Advance clock past 300ms debounce
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preview_panel").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("preview_slide").assertCountEquals(1)
    }

    @Test
    fun `drag handle is visible for each section`() {
        val section1 = makeSectionAt("s1", "Estrofe 1", 0)
        val section2 = makeSectionAt("s2", "Estrofe 2", 1)
        var sections by mutableStateOf(listOf(section1, section2))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SectionEditorComponent(
                    sections = sections,
                    onSectionsChanged = { sections = it },
                    modifier = Modifier.size(800.dp, 600.dp)
                )
            }
        }

        composeTestRule.onNodeWithTag("drag_handle_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drag_handle_1").assertIsDisplayed()
    }

    @Test
    fun `drag first section below second swaps order`() {
        val section1 = makeSectionAt("s1", "Estrofe 1", 0)
        val section2 = makeSectionAt("s2", "Estrofe 2", 1)
        var sections by mutableStateOf(listOf(section1, section2))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides PtBrStrings) {
                SectionEditorComponent(
                    sections = sections,
                    onSectionsChanged = { sections = it },
                    modifier = Modifier.size(800.dp, 600.dp)
                )
            }
        }

        composeTestRule.waitForIdle()

        val handle0Bounds = composeTestRule.onNodeWithTag("drag_handle_0")
            .fetchSemanticsNode().boundsInRoot
        val card1Bounds = composeTestRule.onNodeWithTag("section_card_1")
            .fetchSemanticsNode().boundsInRoot

        // Distance from drag handle's center to past card1's center
        val dragDelta = card1Bounds.center.y - handle0Bounds.center.y + card1Bounds.height / 2f

        // sh.calvin.reorderable uses detectDragGesturesAfterLongPress on all platforms.
        // advanceEventTime(700) fires the internal withTimeout(500ms) long-press timer.
        composeTestRule.onNodeWithTag("drag_handle_0").performTouchInput {
            down(center)
            advanceEventTime(700L)
            repeat(20) { moveBy(Offset(0f, dragDelta / 20f)) }
            up()
        }

        composeTestRule.waitForIdle()

        assert(sections[0].id == "s2") {
            "Expected s2 first after drag, got ${sections.map { it.id }}"
        }
        assert(sections[1].id == "s1") {
            "Expected s1 second after drag, got ${sections.map { it.id }}"
        }
    }

    private fun makeSectionAt(id: String, label: String, sortOrder: Int) = SongSection(
        id = id,
        songId = "song1",
        label = label,
        type = SectionType.VERSE,
        body = "Some lyrics here",
        sortOrder = sortOrder
    )
}
