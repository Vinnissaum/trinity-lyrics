package dev.trinitychurch.lyrics.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
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
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID

private enum class DuplicateAction { SKIP, OVERWRITE }

private sealed interface WizardStep {
    object SourceChooser : WizardStep
    object HolyricsFile : WizardStep
    object PlainText : WizardStep
    // duplicateNewToExisting: maps new song ID → existing song ID for title+artist matches
    data class Preview(
        val songs: List<Song>,
        val duplicateNewToExisting: Map<String, String> = emptyMap()
    ) : WizardStep
    data class Success(val count: Int) : WizardStep
    data class Error(val message: String) : WizardStep
}

private fun openFileDialog(): String? {
    val dialog = FileDialog(null as Frame?, "Selecionar arquivo Holyrics", FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(dir, file).readText()
}

@Composable
fun ImportWizardScreen(
    songRepository: SongRepository,
    parser: HolyricsSongParser = HolyricsSongParser(),
    fileReader: () -> String? = ::openFileDialog,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<WizardStep>(WizardStep.SourceChooser) }
    var plainTitle by remember { mutableStateOf("") }
    var plainText by remember { mutableStateOf("") }
    // key = new song ID, value = chosen action; initialised when entering Preview
    var duplicateResolutions by remember { mutableStateOf<Map<String, DuplicateAction>>(emptyMap()) }

    fun goToPreview(parsedSongs: List<Song>) {
        scope.launch {
            val existing = songRepository.allSongs().first()
            val existingByKey = existing.associateBy {
                "${it.title.trim().lowercase()}::${it.artist.trim().lowercase()}"
            }
            val dupMap = parsedSongs.mapNotNull { song ->
                val key = "${song.title.trim().lowercase()}::${song.artist.trim().lowercase()}"
                existingByKey[key]?.let { ex -> song.id to ex.id }
            }.toMap()
            duplicateResolutions = dupMap.keys.associateWith { DuplicateAction.SKIP }
            step = WizardStep.Preview(parsedSongs, dupMap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.importSongs) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (step) {
                                is WizardStep.SourceChooser -> onNavigateBack()
                                else -> step = WizardStep.SourceChooser
                            }
                        },
                        modifier = Modifier.semantics { testTag = "btn_back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            when (val s = step) {
                is WizardStep.SourceChooser -> SourceChooserStep(
                    holyricsLabel = strings.importHolyrics,
                    plainTextLabel = strings.plainTextImport,
                    onHolyrics = { step = WizardStep.HolyricsFile },
                    onPlainText = { step = WizardStep.PlainText }
                )

                is WizardStep.HolyricsFile -> HolyricsFileStep(
                    selectFileLabel = strings.selectFile,
                    onSelectFile = {
                        val content = fileReader()
                        if (content != null) {
                            try {
                                val songs = parser.parse(content)
                                goToPreview(songs)
                            } catch (e: HolyricsParseException) {
                                step = WizardStep.Error(strings.fileMalformed)
                            } catch (e: Exception) {
                                step = WizardStep.Error(e.message ?: strings.importError)
                            }
                        }
                    }
                )

                is WizardStep.PlainText -> PlainTextStep(
                    title = plainTitle,
                    onTitleChange = { plainTitle = it },
                    text = plainText,
                    onTextChange = { plainText = it },
                    analyzeLabel = strings.confirm,
                    pastePlaceholder = strings.pasteLyrics,
                    titleLabel = strings.songTitle,
                    onAnalyze = {
                        val songId = UUID.randomUUID().toString()
                        val sections = PlainTextSongParser.parse(plainText)
                            .map { it.copy(songId = songId) }
                        val song = Song(
                            id = songId,
                            title = plainTitle.ifBlank { "Nova Música" },
                            sections = sections,
                            source = "plain_text"
                        )
                        goToPreview(listOf(song))
                    }
                )

                is WizardStep.Preview -> PreviewStep(
                    songs = s.songs,
                    duplicateNewToExisting = s.duplicateNewToExisting,
                    duplicateResolutions = duplicateResolutions,
                    onResolutionChange = { songId, action ->
                        duplicateResolutions = duplicateResolutions + (songId to action)
                    },
                    foundLabel = strings.foundSongs,
                    confirmLabel = strings.confirm,
                    cancelLabel = strings.cancel,
                    duplicateFoundLabel = strings.duplicateFound,
                    skipLabel = strings.skip,
                    overwriteLabel = strings.overwrite,
                    onConfirm = {
                        scope.launch {
                            try {
                                // Insert songs that have no duplicate
                                val newSongs = s.songs.filter { it.id !in s.duplicateNewToExisting }
                                val source = s.songs.firstOrNull()?.source ?: "import"
                                if (newSongs.isNotEmpty()) {
                                    songRepository.insertAll(newSongs, source)
                                }
                                // Handle duplicates per resolution
                                var overwriteCount = 0
                                s.songs.filter { it.id in s.duplicateNewToExisting }.forEach { song ->
                                    if (duplicateResolutions[song.id] == DuplicateAction.OVERWRITE) {
                                        val existingId = s.duplicateNewToExisting[song.id]!!
                                        songRepository.update(
                                            song.copy(
                                                id = existingId,
                                                sections = song.sections.map { it.copy(songId = existingId) }
                                            )
                                        )
                                        overwriteCount++
                                    }
                                }
                                step = WizardStep.Success(newSongs.size + overwriteCount)
                            } catch (e: Exception) {
                                step = WizardStep.Error(e.message ?: strings.importError)
                            }
                        }
                    },
                    onCancel = { step = WizardStep.SourceChooser }
                )

                is WizardStep.Success -> SuccessStep(
                    count = s.count,
                    completeLabel = strings.importComplete,
                    closeLabel = strings.close,
                    onClose = onNavigateBack
                )

                is WizardStep.Error -> ErrorStep(
                    message = s.message,
                    errorLabel = strings.importError,
                    retryLabel = strings.selectFile,
                    onRetry = { step = WizardStep.SourceChooser }
                )
            }
        }
    }
}

@Composable
private fun SourceChooserStep(
    holyricsLabel: String,
    plainTextLabel: String,
    onHolyrics: () -> Unit,
    onPlainText: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onHolyrics,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "btn_holyrics" }
        ) {
            Text(holyricsLabel)
        }
        OutlinedButton(
            onClick = onPlainText,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "btn_plain_text" }
        ) {
            Text(plainTextLabel)
        }
    }
}

@Composable
private fun HolyricsFileStep(
    selectFileLabel: String,
    onSelectFile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSelectFile,
            modifier = Modifier.semantics { testTag = "btn_select_file" }
        ) {
            Text(selectFileLabel)
        }
    }
}

@Composable
private fun PlainTextStep(
    title: String,
    onTitleChange: (String) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    analyzeLabel: String,
    pastePlaceholder: String,
    titleLabel: String,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(titleLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "field_plain_title" }
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(pastePlaceholder) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { testTag = "field_plain_text" },
            maxLines = 20
        )
        Button(
            onClick = onAnalyze,
            modifier = Modifier.semantics { testTag = "btn_analyze" }
        ) {
            Text(analyzeLabel)
        }
    }
}

@Composable
private fun PreviewStep(
    songs: List<Song>,
    duplicateNewToExisting: Map<String, String>,
    duplicateResolutions: Map<String, DuplicateAction>,
    onResolutionChange: (String, DuplicateAction) -> Unit,
    foundLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    duplicateFoundLabel: String,
    skipLabel: String,
    overwriteLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${songs.size} $foundLabel",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.semantics { testTag = "text_found_songs" }
        )

        if (duplicateNewToExisting.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = duplicateFoundLabel,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.semantics { testTag = "text_duplicate_warning" }
            )
            duplicateNewToExisting.forEach { (songId, _) ->
                val song = songs.find { it.id == songId } ?: return@forEach
                val action = duplicateResolutions[songId] ?: DuplicateAction.SKIP
                DuplicateResolutionRow(
                    song = song,
                    action = action,
                    skipLabel = skipLabel,
                    overwriteLabel = overwriteLabel,
                    onActionChange = { onResolutionChange(songId, it) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(song.title, style = MaterialTheme.typography.subtitle1)
                        if (song.artist.isNotBlank()) {
                            Text(song.artist, style = MaterialTheme.typography.body2)
                        }
                        Text(
                            "${song.sections.size} seções",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "btn_confirm_import" }
        ) {
            Text(confirmLabel)
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(cancelLabel)
        }
    }
}

@Composable
private fun DuplicateResolutionRow(
    song: Song,
    action: DuplicateAction,
    skipLabel: String,
    overwriteLabel: String,
    onActionChange: (DuplicateAction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics { testTag = "dup_row_${song.id}" },
        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.08f)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.subtitle2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = action == DuplicateAction.SKIP,
                    onClick = { onActionChange(DuplicateAction.SKIP) },
                    modifier = Modifier.semantics { testTag = "dup_skip_${song.id}" }
                )
                Text(
                    text = skipLabel,
                    modifier = Modifier.padding(end = 16.dp)
                )
                RadioButton(
                    selected = action == DuplicateAction.OVERWRITE,
                    onClick = { onActionChange(DuplicateAction.OVERWRITE) },
                    modifier = Modifier.semantics { testTag = "dup_overwrite_${song.id}" }
                )
                Text(text = overwriteLabel)
            }
        }
    }
}

@Composable
private fun SuccessStep(
    count: Int,
    completeLabel: String,
    closeLabel: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$completeLabel: $count",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.semantics { testTag = "text_import_complete" }
        )
        Button(
            onClick = onClose,
            modifier = Modifier.semantics { testTag = "btn_close" }
        ) {
            Text(closeLabel)
        }
    }
}

@Composable
private fun ErrorStep(
    message: String,
    errorLabel: String,
    retryLabel: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            backgroundColor = MaterialTheme.colors.error,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = errorLabel,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onError
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onError,
                    modifier = Modifier.semantics { testTag = "text_import_error" }
                )
            }
        }
        Button(
            onClick = onRetry,
            modifier = Modifier.semantics { testTag = "btn_retry" }
        ) {
            Text(retryLabel)
        }
    }
}
