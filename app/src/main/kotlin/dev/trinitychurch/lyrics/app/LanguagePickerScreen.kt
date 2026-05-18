package dev.trinitychurch.lyrics.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.domain.AppLocale
import dev.trinitychurch.lyrics.domain.LocaleStore
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.launch

@Composable
fun LanguagePickerScreen(
    localeStore: LocaleStore,
    onContinue: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var selectedLocale by remember { mutableStateOf(AppLocale.PT_BR) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(strings.chooseLanguage, style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(8.dp))
        Text(strings.chooseLanguageSubtitle, style = MaterialTheme.typography.body2)
        Spacer(Modifier.height(32.dp))

        AppLocale.entries.forEach { locale ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = selectedLocale == locale,
                    onClick = { selectedLocale = locale }
                )
                Text(
                    text = locale.displayName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                scope.launch {
                    localeStore.setLocale(selectedLocale)
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.continueButton)
        }
    }
}
