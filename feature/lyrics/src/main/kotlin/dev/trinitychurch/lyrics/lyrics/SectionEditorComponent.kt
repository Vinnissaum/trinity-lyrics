package dev.trinitychurch.lyrics.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Slide
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.SongSection
import dev.trinitychurch.lyrics.presentation.SlideSplitter
import dev.trinitychurch.lyrics.ui.strings.LocalStrings
import dev.trinitychurch.lyrics.ui.strings.StringResources
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

@Composable
fun SectionEditorComponent(
    sections: List<SongSection>,
    onSectionsChanged: (List<SongSection>) -> Unit,
    songId: String = "",
    config: SlideConfig = SlideConfig(),
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var previewSlides by remember { mutableStateOf(emptyList<Slide>()) }
    val sectionsRef = rememberUpdatedState(sections)

    LaunchedEffect(sections) {
        delay(300L)
        previewSlides = sectionsRef.value.flatMap { SlideSplitter.split(it, config) }
    }

    Row(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            SectionList(
                sections = sections,
                onSectionsChanged = onSectionsChanged,
                strings = strings,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = {
                    val newSection = SongSection(
                        id = UUID.randomUUID().toString(),
                        songId = songId.ifBlank { sections.firstOrNull()?.songId ?: "" },
                        label = "${strings.verse} ${sections.size + 1}",
                        type = SectionType.VERSE,
                        body = "",
                        sortOrder = sections.size
                    )
                    onSectionsChanged(sections + newSection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "add_section_button" }
            ) {
                Text(strings.addSection)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp)
                .semantics { testTag = "preview_panel" }
        ) {
            Text(
                strings.livePreview,
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyColumn {
                items(previewSlides, key = { "${it.sectionId}_${it.slideIndexInSection}" }) { slide ->
                    SlidePreviewCard(slide)
                }
            }
        }
    }
}

@Composable
private fun SectionList(
    sections: List<SongSection>,
    onSectionsChanged: (List<SongSection>) -> Unit,
    strings: StringResources,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val reordered = sections.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }.mapIndexed { index, sec -> sec.copy(sortOrder = index) }
        onSectionsChanged(reordered)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(sections, key = { it.id }) { section ->
            val index = sections.indexOf(section)
            ReorderableItem(reorderState, key = section.id) { isDragging ->
                SectionRow(
                    section = section,
                    index = index,
                    isDragging = isDragging,
                    onChanged = { updated ->
                        onSectionsChanged(sections.map { if (it.id == updated.id) updated else it })
                    },
                    onDelete = {
                        onSectionsChanged(
                            sections.filter { it.id != section.id }
                                .mapIndexed { i, s -> s.copy(sortOrder = i) }
                        )
                    },
                    strings = strings,
                    dragHandleModifier = Modifier.draggableHandle()
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: SongSection,
    index: Int,
    isDragging: Boolean,
    onChanged: (SongSection) -> Unit,
    onDelete: () -> Unit,
    strings: StringResources,
    dragHandleModifier: Modifier
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    Card(
        elevation = if (isDragging) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { testTag = "section_card_$index" }
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Reorder",
                    modifier = dragHandleModifier
                        .semantics { testTag = "drag_handle_$index" }
                )

                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.semantics { testTag = "section_type_$index" }
                    ) {
                        Text(sectionTypeName(section.type, strings))
                    }
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        SectionType.values().forEach { type ->
                            DropdownMenuItem(onClick = {
                                onChanged(section.copy(type = type))
                                showTypeMenu = false
                            }) {
                                Text(sectionTypeName(type, strings))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = section.label,
                    onValueChange = { onChanged(section.copy(label = it)) },
                    label = { Text(strings.sectionLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { testTag = "section_label_$index" }
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.semantics { testTag = "delete_section_$index" }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                }
            }

            OutlinedTextField(
                value = section.body,
                onValueChange = { onChanged(section.copy(body = it)) },
                label = { Text(strings.sectionBody) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { testTag = "section_body_$index" },
                minLines = 3
            )
        }
    }
}

@Composable
private fun SlidePreviewCard(slide: Slide) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .semantics { testTag = "preview_slide" }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            slide.lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

private fun sectionTypeName(type: SectionType, strings: StringResources): String = when (type) {
    SectionType.VERSE -> strings.verse
    SectionType.CHORUS -> strings.chorus
    SectionType.BRIDGE -> strings.bridge
    SectionType.PRE_CHORUS -> strings.preChorus
    SectionType.OUTRO -> strings.outro
    SectionType.INTERLUDE -> strings.interlude
    SectionType.TAG -> strings.tag
}
