package dev.trinitychurch.lyrics.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SongEditScreen(
    repository: SongRepository,
    songId: String?,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var sections by remember { mutableStateOf<List<SongSection>>(emptyList()) }
    var originalTitle by remember { mutableStateOf("") }
    var originalArtist by remember { mutableStateOf("") }
    var originalSections by remember { mutableStateOf<List<SongSection>>(emptyList()) }

    var loaded by remember { mutableStateOf(songId == null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var sectionsError by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(songId) {
        if (songId != null) {
            val song = repository.songById(songId).first()
            if (song != null) {
                title = song.title
                artist = song.artist
                sections = song.sections
                originalTitle = song.title
                originalArtist = song.artist
                originalSections = song.sections
            }
            loaded = true
        }
    }

    val isDirty = title != originalTitle || artist != originalArtist || sections != originalSections

    fun attemptBack() {
        if (isDirty) showDiscardDialog = true else onNavigateBack()
    }

    fun save() {
        titleError = if (title.isBlank()) strings.titleRequired else null
        sectionsError = if (sections.isEmpty()) strings.noSectionsInSong else null
        if (titleError != null || sectionsError != null) return

        scope.launch {
            val now = System.currentTimeMillis()
            val id = songId ?: UUID.randomUUID().toString()
            val song = Song(
                id = id,
                title = title.trim(),
                artist = artist.trim(),
                sections = sections.mapIndexed { i, s -> s.copy(songId = id, sortOrder = i) },
                createdAt = now,
                updatedAt = now
            )
            if (songId == null) repository.insert(song)
            else repository.update(song)
            onNavigateBack()
        }
    }

    if (!loaded) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (songId == null) strings.newSong else strings.edit) },
                navigationIcon = {
                    IconButton(
                        onClick = ::attemptBack,
                        modifier = Modifier.semantics { testTag = "btn_back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::save,
                        modifier = Modifier.semantics { testTag = "btn_save" }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = strings.save)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text(strings.songTitle) },
                isError = titleError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { testTag = "field_title" }
            )
            if (titleError != null) {
                Text(
                    text = titleError!!,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .semantics { testTag = "error_title" }
                )
            }

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(strings.artist) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics { testTag = "field_artist" }
            )

            if (sectionsError != null) {
                Text(
                    text = sectionsError!!,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .semantics { testTag = "error_sections" }
                )
            }

            SectionEditorComponent(
                sections = sections,
                onSectionsChanged = {
                    sections = it
                    sectionsError = null
                },
                songId = songId ?: "",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(strings.back) },
            text = { Text("Descartar alterações?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    modifier = Modifier.semantics { testTag = "confirm_discard" }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
