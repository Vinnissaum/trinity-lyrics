package dev.trinitychurch.lyrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.SlideConfig

@Composable
fun PresentationWindowApp(
    store: PresentationStateStore,
    config: SlideConfig = SlideConfig(),
) {
    val state by store.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val s = state) {
            is PresentationState.Idle, is PresentationState.Blank -> Unit
            is PresentationState.Lyrics -> LyricsSlideView(
                slide = s.allSlides[s.displaySlideIndex],
                config = config,
            )
        }
    }
}
