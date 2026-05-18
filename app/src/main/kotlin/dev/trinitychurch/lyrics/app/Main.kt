package dev.trinitychurch.lyrics.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dev.trinitychurch.lyrics.app.di.AppModule
import dev.trinitychurch.lyrics.db.SetRepository
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.domain.AppLocale
import dev.trinitychurch.lyrics.domain.LocaleStore
import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.SettingsRepository
import dev.trinitychurch.lyrics.importer.ImportWizardScreen
import dev.trinitychurch.lyrics.lyrics.LibraryScreen
import dev.trinitychurch.lyrics.lyrics.SetBuilderScreen
import dev.trinitychurch.lyrics.lyrics.SongEditScreen
import dev.trinitychurch.lyrics.presentation.OperatorConsoleApp
import dev.trinitychurch.lyrics.presentation.PresentationStateStore
import dev.trinitychurch.lyrics.presentation.PresentationWindowApp
import dev.trinitychurch.lyrics.ui.strings.EnStrings
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.PtBrStrings
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import java.awt.GraphicsEnvironment

private sealed class AppScreen {
    object Library : AppScreen()
    data class SongEdit(val songId: String?) : AppScreen()
    data class SetBuilder(val setId: String?) : AppScreen()
    object Import : AppScreen()
    object Settings : AppScreen()
    object Presentation : AppScreen()
}

fun main() {
    val koin = startKoin { modules(AppModule) }.koin
    val localeStore = koin.get<LocaleStore>()
    val presentationStore = koin.get<PresentationStateStore>()
    val songRepository = koin.get<SongRepository>()
    val setRepository = koin.get<SetRepository>()
    val settings = koin.get<SettingsRepository>()

    runBlocking { localeStore.load() }

    application {
        val locale by localeStore.locale.collectAsState()
        val strings = when (locale) {
            AppLocale.PT_BR -> PtBrStrings
            AppLocale.EN -> EnStrings
        }

        var showLanguagePicker by remember { mutableStateOf(localeStore.isFirstRun()) }

        CompositionLocalProvider(LocalStrings provides strings) {
                if (showLanguagePicker) {
                    Window(
                        title = strings.appName,
                        onCloseRequest = ::exitApplication
                    ) {
                        MaterialTheme {
                            LanguagePickerScreen(
                                localeStore = localeStore,
                                onContinue = { showLanguagePicker = false }
                            )
                        }
                    }
                } else {
                    var navStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Library)) }
                    val currentScreen = navStack.last()

                    fun push(screen: AppScreen) { navStack = navStack + screen }
                    fun pop() { if (navStack.size > 1) navStack = navStack.dropLast(1) }

                    Window(
                        title = strings.appName,
                        onCloseRequest = ::exitApplication
                    ) {
                        MaterialTheme {
                            when (val screen = currentScreen) {
                                is AppScreen.Library -> LibraryScreen(
                                    repository = songRepository,
                                    onNavigateToEditor = { push(AppScreen.SongEdit(it)) },
                                    onNavigateToImport = { push(AppScreen.Import) },
                                    onNavigateToSets = { push(AppScreen.SetBuilder(null)) },
                                    onNavigateToSettings = { push(AppScreen.Settings) }
                                )
                                is AppScreen.SongEdit -> SongEditScreen(
                                    repository = songRepository,
                                    songId = screen.songId,
                                    onNavigateBack = ::pop
                                )
                                is AppScreen.SetBuilder -> SetBuilderScreen(
                                    setRepository = setRepository,
                                    songRepository = songRepository,
                                    presentationStore = presentationStore,
                                    setId = screen.setId,
                                    onNavigateBack = ::pop,
                                    onStartPresentation = { push(AppScreen.Presentation) }
                                )
                                is AppScreen.Import -> ImportWizardScreen(
                                    songRepository = songRepository,
                                    onNavigateBack = ::pop
                                )
                                is AppScreen.Settings -> SettingsScreen(
                                    settings = settings,
                                    localeStore = localeStore,
                                    onNavigateBack = ::pop
                                )
                                is AppScreen.Presentation -> OperatorConsoleApp(
                                    store = presentationStore,
                                    onExit = ::pop
                                )
                            }
                        }
                    }

                    val presentationState by presentationStore.state.collectAsState()
                    if (presentationState !is PresentationState.Idle) {
                        var monitorIndex by remember { mutableStateOf(1) }
                        LaunchedEffect(Unit) {
                            monitorIndex = settings.getInt("presentation.monitor_index", 1)
                        }
                        val screenDevices = GraphicsEnvironment
                            .getLocalGraphicsEnvironment()
                            .screenDevices
                        val targetDevice = screenDevices.getOrElse(monitorIndex) { screenDevices.last() }
                        val bounds = targetDevice.defaultConfiguration.bounds
                        Window(
                            title = "",
                            undecorated = true,
                            alwaysOnTop = true,
                            state = WindowState(
                                position = WindowPosition(bounds.x.dp, bounds.y.dp),
                                size = DpSize(bounds.width.dp, bounds.height.dp)
                            ),
                            onCloseRequest = {}
                        ) {
                            MaterialTheme {
                                PresentationWindowApp(presentationStore)
                            }
                        }
                    }
                }
        }
    }
}
