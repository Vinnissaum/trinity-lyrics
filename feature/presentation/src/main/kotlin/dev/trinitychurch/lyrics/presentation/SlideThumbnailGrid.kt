package dev.trinitychurch.lyrics.presentation

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import dev.trinitychurch.lyrics.domain.Slide

@Composable
fun SlideThumbnailGrid(
    slides: List<Slide>,
    displaySlideIndex: Int,
    operatorSlideIndex: Int,
    onSlideClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(displaySlideIndex) {
        if (slides.isNotEmpty() && displaySlideIndex in slides.indices) {
            gridState.animateScrollToItem(displaySlideIndex)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        state = gridState,
        modifier = modifier,
    ) {
        itemsIndexed(slides) { index, slide ->
            val isDisplay = index == displaySlideIndex
            val isOperator = index == operatorSlideIndex && !isDisplay
            SlideThumbnailCard(
                slide = slide,
                isCurrentDisplay = isDisplay,
                isCurrentOperator = isOperator,
                onClick = { onSlideClick(index) },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    testTag = "thumbnail_$index"
                    selected = isDisplay
                    if (isOperator) stateDescription = "operator_position"
                },
            )
        }
    }
}
