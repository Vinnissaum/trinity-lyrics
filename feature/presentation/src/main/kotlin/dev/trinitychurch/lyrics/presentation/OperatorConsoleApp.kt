package dev.trinitychurch.lyrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.Slide
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.StringResources

@Composable
fun OperatorConsoleApp(
    store: PresentationStateStore,
    onExit: () -> Unit,
) {
    val state by store.state.collectAsState()
    val lyricsState = state as? PresentationState.Lyrics
    val strings = LocalStrings.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight, Key.Spacebar -> { store.advance(); true }
                    Key.DirectionLeft -> { store.previous(); true }
                    Key.B -> { store.toggleBlank(); true }
                    Key.F -> { store.toggleFreeze(); true }
                    Key.Escape -> { store.clear(); onExit(); true }
                    else -> false
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                    CurrentSlidePanel(
                        slide = lyricsState?.allSlides?.getOrNull(lyricsState.currentSlideIndex),
                        modifier = Modifier.weight(0.7f).fillMaxWidth(),
                    )
                    NextSlidePreview(
                        slide = lyricsState?.allSlides?.getOrNull(lyricsState.currentSlideIndex + 1),
                        strings = strings,
                        modifier = Modifier.weight(0.3f).fillMaxWidth(),
                    )
                }
                SlideThumbnailGrid(
                    slides = lyricsState?.allSlides ?: emptyList(),
                    displaySlideIndex = lyricsState?.displaySlideIndex ?: 0,
                    operatorSlideIndex = lyricsState?.currentSlideIndex ?: 0,
                    onSlideClick = { store.jumpToSlide(it) },
                    modifier = Modifier.weight(0.3f).fillMaxHeight(),
                )
            }
            ControlsBar(
                currentSlideIndex = lyricsState?.currentSlideIndex ?: 0,
                totalSlides = lyricsState?.allSlides?.size ?: 0,
                isFrozen = lyricsState?.frozen ?: false,
                onBlank = { store.toggleBlank() },
                onFreeze = { store.toggleFreeze() },
                onExit = { store.clear(); onExit() },
                strings = strings,
            )
        }
    }
}

@Composable
private fun CurrentSlidePanel(slide: Slide?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .semantics(mergeDescendants = true) { testTag = "current_slide_panel" },
        contentAlignment = Alignment.Center,
    ) {
        if (slide != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                slide.lines.forEach { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun NextSlidePreview(
    slide: Slide?,
    strings: StringResources,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF2A2A2A))
            .padding(8.dp)
            .semantics(mergeDescendants = true) { testTag = "next_slide_panel" },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (slide != null) {
            Column {
                Text(text = strings.nextSlide, color = Color.Gray, fontSize = 12.sp)
                slide.lines.forEach { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlsBar(
    currentSlideIndex: Int,
    totalSlides: Int,
    isFrozen: Boolean,
    onBlank: () -> Unit,
    onFreeze: () -> Unit,
    onExit: () -> Unit,
    strings: StringResources,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onBlank,
                modifier = Modifier.semantics(mergeDescendants = true) { testTag = "blank_button" },
            ) {
                Text(strings.blank)
            }
            Button(
                onClick = onFreeze,
                colors = if (isFrozen) {
                    ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.semantics(mergeDescendants = true) { testTag = "freeze_button" },
            ) {
                Text(strings.freeze)
            }
        }
        Text(
            text = "${currentSlideIndex + 1} ${strings.slideOf} $totalSlides",
            modifier = Modifier.semantics(mergeDescendants = true) { testTag = "slide_progress" },
        )
        Button(
            onClick = onExit,
            modifier = Modifier.semantics(mergeDescendants = true) { testTag = "exit_button" },
        ) {
            Text(strings.exitPresentation)
        }
    }
}
