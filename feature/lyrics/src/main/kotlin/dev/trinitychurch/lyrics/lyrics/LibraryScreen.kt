package dev.trinitychurch.lyrics.lyrics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun LibraryScreen(
    repository: SongRepository,
    onNavigateToEditor: (songId: String?) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSets: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .debounce(150L)
            .flatMapLatest { query ->
                if (query.isBlank()) repository.allSongs()
                else repository.search(query)
            }
            .collect { songs = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.songs) },
                actions = {
                    TextButton(onClick = onNavigateToSets) {
                        Text(strings.serviceSets)
                    }
                    TextButton(onClick = onNavigateToImport) {
                        Text(strings.importSongs)
                    }
                    TextButton(onClick = onNavigateToSettings) {
                        Text(strings.settings)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                modifier = Modifier.semantics { testTag = "fab_new_song" }
            ) {
                Text("+")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(strings.searchHint) },
                label = { Text(strings.search) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { testTag = "search_field" },
                singleLine = true
            )

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noSongsFound,
                        modifier = Modifier.semantics { testTag = "empty_state" }
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(songs, key = { it.id }) { song ->
                        SongCard(
                            song = song,
                            onClick = { onNavigateToEditor(song.id) },
                            onLongClick = { songToDelete = song }
                        )
                    }
                }
            }
        }

        songToDelete?.let { song ->
            AlertDialog(
                onDismissRequest = { songToDelete = null },
                title = { Text(strings.deleteConfirm) },
                text = { Text(song.title) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.softDelete(song.id)
                                songToDelete = null
                            }
                        },
                        modifier = Modifier.semantics { testTag = "confirm_delete" }
                    ) {
                        Text(strings.delete)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { songToDelete = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics { testTag = "song_card_${song.id}" }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(song.title, style = MaterialTheme.typography.subtitle1)
            if (song.artist.isNotBlank()) {
                Text(song.artist, style = MaterialTheme.typography.body2)
            }
            Text(
                text = "${song.sections.size} seções",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
