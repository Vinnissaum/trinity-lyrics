package dev.trinitychurch.lyrics.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.trinitychurch.lyrics.app.di.AppModule
import dev.trinitychurch.lyrics.domain.PresentationStateStore
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(AppModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Trinity Lyrics — Operator"
        ) {
            val store = koinInject<PresentationStateStore>()
            val slideIndex by store.slideIndex.collectAsState()
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    Text("Slide: $slideIndex")
                    Button(onClick = { store.advance() }) {
                        Text("Click to advance")
                    }
                }
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Trinity Lyrics — Projection"
        ) {
            val store = koinInject<PresentationStateStore>()
            val slideIndex by store.slideIndex.collectAsState()
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Slide: $slideIndex", style = MaterialTheme.typography.h2)
                }
            }
        }
    }
}
