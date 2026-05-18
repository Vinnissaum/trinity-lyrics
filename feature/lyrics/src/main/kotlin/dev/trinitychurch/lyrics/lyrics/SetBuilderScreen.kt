package dev.trinitychurch.lyrics.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import dev.trinitychurch.lyrics.db.SetRepository
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.presentation.PresentationStateStore
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

@Composable
fun SetBuilderScreen(
    setRepository: SetRepository,
    songRepository: SongRepository,
    presentationStore: PresentationStateStore,
    setId: String?,
    config: SlideConfig = SlideConfig(),
    onNavigateBack: () -> Unit,
    onStartPresentation: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var currentSetId by remember { mutableStateOf(setId) }
    var setName by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<SetItem>>(emptyList()) }
    var allSongs by remember { mutableStateOf<Map<String, Song>>(emptyMap()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSetEmptyGuard by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (setId != null) {
            val set = setRepository.setById(setId).first()
            if (set != null) {
                setName = set.name
                serviceDate = set.serviceDate ?: ""
                items = set.items.sortedBy { it.sortOrder }
            }
        } else {
            val newId = UUID.randomUUID().toString()
            setRepository.createSet(ServiceSet(id = newId, name = ""))
            currentSetId = newId
        }
        loaded = true
    }

    LaunchedEffect(Unit) {
        songRepository.allSongs().collect { songs ->
            allSongs = songs.associateBy { it.id }
        }
    }

    if (!loaded) return

    fun addSong(song: Song) {
        val sid = currentSetId ?: return
        val newItem = SetItem(
            id = UUID.randomUUID().toString(),
            setId = sid,
            songId = song.id,
            sortOrder = items.size
        )
        items = items + newItem
        scope.launch { setRepository.addItem(newItem) }
    }

    fun removeItem(item: SetItem) {
        items = items.filter { it.id != item.id }
            .mapIndexed { i, s -> s.copy(sortOrder = i) }
        scope.launch { setRepository.removeItem(item.id) }
    }

    fun reorder(newItems: List<SetItem>) {
        val sid = currentSetId ?: return
        items = newItems
        scope.launch { setRepository.reorderItems(sid, newItems.map { it.id }) }
    }

    fun startPresentation() {
        if (items.isEmpty()) {
            showSetEmptyGuard = true
            return
        }
        val sid = currentSetId ?: return
        val set = ServiceSet(
            id = sid,
            name = setName,
            serviceDate = serviceDate.ifBlank { null },
            items = items
        )
        presentationStore.loadSet(set, allSongs, config)
        onStartPresentation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.serviceSets) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val sid = currentSetId
                            if (sid != null) {
                                scope.launch {
                                    setRepository.updateSet(
                                        ServiceSet(
                                            id = sid,
                                            name = setName,
                                            serviceDate = serviceDate.ifBlank { null }
                                        )
                                    )
                                }
                            }
                            onNavigateBack()
                        },
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
        ) {
            OutlinedTextField(
                value = setName,
                onValueChange = { setName = it },
                label = { Text(strings.setName) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { testTag = "field_set_name" }
            )

            OutlinedTextField(
                value = serviceDate,
                onValueChange = { serviceDate = it },
                label = { Text(strings.serviceDate) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics { testTag = "field_service_date" }
            )

            if (showSetEmptyGuard) {
                Text(
                    text = strings.setEmpty,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics { testTag = "set_empty_guard" }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.semantics { testTag = "btn_add_song" }
                ) {
                    Text(strings.addSong)
                }

                Button(
                    onClick = ::startPresentation,
                    modifier = Modifier.semantics { testTag = "btn_start_presentation" }
                ) {
                    Text(strings.startPresentation)
                }
            }

            SetItemList(
                items = items,
                songs = allSongs,
                onReorder = ::reorder,
                onDelete = ::removeItem,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showAddDialog) {
        val alreadyAdded = items.map { it.songId }.toSet()
        val available = allSongs.values.filter { it.id !in alreadyAdded }.sortedBy { it.title }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addSong) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    if (available.isEmpty()) {
                        item {
                            Text(
                                text = strings.noSongsFound,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(available, key = { it.id }) { song ->
                            TextButton(
                                onClick = {
                                    addSong(song)
                                    showAddDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { testTag = "pick_song_${song.id}" }
                            ) {
                                Text(
                                    text = song.title,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun SetItemList(
    items: List<SetItem>,
    songs: Map<String, Song>,
    onReorder: (List<SetItem>) -> Unit,
    onDelete: (SetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val reordered = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }.mapIndexed { index, item -> item.copy(sortOrder = index) }
        onReorder(reordered)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(items, key = { it.id }) { item ->
            val index = items.indexOf(item)
            ReorderableItem(reorderState, key = item.id) { isDragging ->
                SetItemCard(
                    item = item,
                    song = songs[item.songId],
                    index = index,
                    isDragging = isDragging,
                    onDelete = { onDelete(item) },
                    dragHandleModifier = Modifier.draggableHandle()
                )
            }
        }
    }
}

@Composable
private fun SetItemCard(
    item: SetItem,
    song: Song?,
    index: Int,
    isDragging: Boolean,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier
) {
    val strings = LocalStrings.current
    Card(
        elevation = if (isDragging) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { testTag = "set_item_card_$index" }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Reorder",
                modifier = dragHandleModifier
                    .semantics { testTag = "set_item_drag_$index" }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = song?.title ?: item.songId,
                    style = MaterialTheme.typography.subtitle1
                )
                if (song?.artist?.isNotBlank() == true) {
                    Text(text = song.artist, style = MaterialTheme.typography.body2)
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { testTag = "set_item_delete_$index" }
            ) {
                Icon(Icons.Default.Delete, contentDescription = strings.removeSong)
            }
        }
    }
}
