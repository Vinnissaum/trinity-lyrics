package dev.trinitychurch.lyrics.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.domain.AppLocale
import dev.trinitychurch.lyrics.domain.LocaleStore
import dev.trinitychurch.lyrics.domain.SettingsRepository
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.launch
import java.awt.GraphicsEnvironment

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    localeStore: LocaleStore,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var monitorIndex by remember { mutableStateOf(0) }
    var fontSize by remember { mutableStateOf(48f) }
    var maxLines by remember { mutableStateOf(4f) }
    var selectedLocale by remember { mutableStateOf(localeStore.locale.value) }

    val screenDevices = remember {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    }

    LaunchedEffect(Unit) {
        monitorIndex = settings.getInt("presentation.monitor_index", 0)
        fontSize = settings.getInt("presentation.font_size", 48).toFloat()
        maxLines = settings.getInt("presentation.max_lines", 4).toFloat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { testTag = "btn_back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Monitor selection
            Text(
                text = strings.targetMonitor,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.semantics { testTag = "label_monitor" }
            )
            Spacer(Modifier.height(8.dp))
            screenDevices.forEachIndexed { index, device ->
                val bounds = device.defaultConfiguration.bounds
                val label = "Monitor ${index + 1} — ${bounds.width}×${bounds.height}"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "monitor_option_$index" }
                ) {
                    RadioButton(
                        selected = monitorIndex == index,
                        onClick = {
                            monitorIndex = index
                            scope.launch {
                                settings.putInt("presentation.monitor_index", index)
                            }
                        }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Font size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.fontSize,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = fontSize.toInt().toString(),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.semantics { testTag = "value_font_size" }
                )
            }
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                onValueChangeFinished = {
                    scope.launch {
                        settings.putInt("presentation.font_size", fontSize.toInt())
                    }
                },
                valueRange = 24f..96f,
                steps = 71,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "slider_font_size" }
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Max lines per slide
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.maxLinesPerSlide,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = maxLines.toInt().toString(),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.semantics { testTag = "value_max_lines" }
                )
            }
            Slider(
                value = maxLines,
                onValueChange = { maxLines = it },
                onValueChangeFinished = {
                    scope.launch {
                        settings.putInt("presentation.max_lines", maxLines.toInt())
                    }
                },
                valueRange = 2f..8f,
                steps = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "slider_max_lines" }
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Language
            Text(
                text = strings.language,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.semantics { testTag = "label_language" }
            )
            Spacer(Modifier.height(8.dp))
            AppLocale.entries.forEach { locale ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "locale_option_${locale.code}" }
                ) {
                    RadioButton(
                        selected = selectedLocale == locale,
                        onClick = {
                            selectedLocale = locale
                            scope.launch { localeStore.setLocale(locale) }
                        }
                    )
                    Text(
                        text = locale.displayName,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
