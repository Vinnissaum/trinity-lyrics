package dev.trinitychurch.lyrics.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Slide
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SlideThumbnailGridSpec {

    @get:Rule
    val rule = createComposeRule()

    private fun slide(index: Int) = Slide(
        sectionId = "sec-$index",
        sectionLabel = "Section",
        sectionType = SectionType.VERSE,
        lines = listOf("Line $index"),
        slideIndexInSection = index,
        totalSlidesInSection = 10,
    )

    @Test
    fun `clicking slide 7 calls onSlideClick with index 7`() {
        val clicked = mutableListOf<Int>()
        val slides = (0 until 10).map { slide(it) }

        rule.setContent {
            SlideThumbnailGrid(
                slides = slides,
                displaySlideIndex = 0,
                operatorSlideIndex = 0,
                onSlideClick = { clicked.add(it) },
            )
        }

        rule.onNodeWithTag("thumbnail_7").performClick()
        assertEquals(listOf(7), clicked)
    }

    @Test
    fun `displaySlideIndex=3 marks thumbnail 3 as selected`() {
        val slides = (0 until 10).map { slide(it) }

        rule.setContent {
            SlideThumbnailGrid(
                slides = slides,
                displaySlideIndex = 3,
                operatorSlideIndex = 3,
                onSlideClick = {},
            )
        }

        rule.onNodeWithTag("thumbnail_3").assertIsSelected()
    }

    @Test
    fun `frozen state - thumbnail 2 highlighted and thumbnail 5 has operator indicator`() {
        val slides = (0 until 10).map { slide(it) }

        rule.setContent {
            SlideThumbnailGrid(
                slides = slides,
                displaySlideIndex = 2,
                operatorSlideIndex = 5,
                onSlideClick = {},
            )
        }

        rule.onNodeWithTag("thumbnail_2").assertIsSelected()
        rule.onNodeWithTag("thumbnail_5").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "operator_position")
        )
    }
}
