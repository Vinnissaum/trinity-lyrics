package dev.trinitychurch.lyrics.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.trinitychurch.lyrics.app.di.AppModule
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
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trinity Lyrics — Phase 1 in progress")
                }
            }
        }
    }
}
