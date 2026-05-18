package dev.trinitychurch.lyrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import dev.trinitychurch.lyrics.domain.Slide
import dev.trinitychurch.lyrics.domain.SlideConfig

@Composable
fun LyricsSlideView(
    slide: Slide,
    config: SlideConfig,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics { testTag = "lyrics_slide_view" },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            slide.lines.forEachIndexed { index, line ->
                Text(
                    text = line,
                    fontSize = config.fontSizeSp.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { testTag = "slide_line_$index" },
                )
            }
        }
    }
}
