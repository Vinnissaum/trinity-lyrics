# Phase 1 — Lyrics MVP Design

**Spec**: `.specs/features/phase-1-lyrics-mvp/spec.md`  
**Status**: Approved  
**Last updated:** 2026-05-17

---

## Architecture Overview

Phase 1 extends the Phase 0 two-window skeleton with a full lyrics pipeline. The skeleton (`Main.kt`, `PresentationStateStore`) already exists and is wired via Koin. Phase 1 replaces stub types with real domain models, adds SQLDelight persistence, and builds all operator UI screens.

```
Operator Window (Main Monitor)
├── LibraryScreen       — song list, search, CRUD
├── SetBuilderScreen    — set items, drag-to-reorder
├── ImportWizardScreen  — Holyrics JSON + plain-text
├── SettingsScreen      — monitor, font, language
└── PresentationControls
    ├── CurrentSlidePanel    — large current slide
    ├── NextSlidePreview     — small next slide
    └── SlideThumbnailGrid  — all slides, click-to-jump

Projection Window (Second Monitor — undecorated, fullscreen)
└── LyricsSlideView     — centered text, black BG

Shared (Koin singleton)
└── PresentationStateStore  — StateFlow<PresentationState>
```

**State flow for slide advance:**
```
Operator: Right arrow
  → KeyboardHandler.onKeyEvent
  → PresentationStateStore.advance()
  → _state.value = Lyrics(currentSlideIndex++, displaySlideIndex++)
  → StateFlow emits
  → Projection window recomposes — LyricsSlideView shows new text
  → Operator console recomposes — SlideThumbnailGrid highlights new index
```

**Freeze state flow:**
```
Operator: F key
  → toggleFreeze()
  → if not frozen: frozenDisplayIndex = currentSlideIndex
  → if frozen: frozenDisplayIndex = null (snap to current)
  → Projection observes displaySlideIndex = frozenDisplayIndex ?: currentSlideIndex
  → Operator navigates freely; projection stays on frozenDisplayIndex
```

---

## Code Reuse Analysis

### Existing to Reuse

| Component | Location | How |
|---|---|---|
| `PresentationStateStore` (stub) | `core/domain/src/main/kotlin/.../PresentationStateStore.kt` | Replace stub with full implementation in `feature:presentation` |
| `AppModule` | `app/src/main/kotlin/.../di/AppModule.kt` | Add all new Koin bindings here |
| `Main.kt` | `app/src/main/kotlin/.../Main.kt` | Extend with navigation, monitor selection, first-run gate |
| SQLDelight setup | `core/db/build.gradle.kts` | Schema already pointed at `sqldelight/TrinityLyricsDatabase/` |
| `StringResources` architecture | `.specs/features/i18n-language-selection/spec.md` | Implement fully as designed |

### Integration Points

| System | Integration |
|---|---|
| SQLDelight 2.x | All repositories use `TrinityLyricsDatabase` generated interface |
| Koin 4.x | Every store and repository registered as `single {}` in `AppModule` |
| Kotlin Coroutines | All DB flows run on `Dispatchers.IO`; UI collects on `Dispatchers.Main` |
| kotlinx.serialization | Holyrics JSON parsing — `@Serializable` DTOs |

---

## Components

### 1. Domain Types — `:core:domain`

**Purpose**: Shared pure-Kotlin types with no Compose/DI/DB dependencies.  
**Location**: `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/`

New files needed:

| File | Contents |
|---|---|
| `Song.kt` | `data class Song(id, title, artist, sections, tags, slideConfig, backgroundId, source)` |
| `SongSection.kt` | `data class SongSection(id, songId, label, type, body, sortOrder, repeatCount)` |
| `SectionType.kt` | `enum class SectionType { VERSE, CHORUS, BRIDGE, PRE_CHORUS, OUTRO, INTERLUDE, TAG }` |
| `Slide.kt` | `data class Slide(sectionId, sectionLabel, sectionType, lines, slideIndexInSection, totalSlidesInSection)` |
| `SlideConfig.kt` | `data class SlideConfig(maxLinesPerSlide: Int = 4, maxCharsPerLine: Int = 60, fontSizeSp: Int = 48)` |
| `PresentationState.kt` | Sealed class (see below) |
| `Background.kt` | `sealed class Background { object Black; data class SolidColor(color); data class Image(mediaId) }` |
| `ServiceSet.kt` | `data class ServiceSet(id, name, serviceDate, items: List<SetItem>)` |
| `SetItem.kt` | `data class SetItem(id, setId, songId, sortOrder)` |
| `Tag.kt` | `data class Tag(id, name, color)` |
| `SettingsRepository.kt` | Interface: `getString`, `putString`, `getInt`, `putInt` |
| `AppLocale.kt` | `enum class AppLocale(code, displayName) { PT_BR, EN }` |
| `LocaleStore.kt` | StateFlow-based locale manager |

**`PresentationState` sealed class:**

```kotlin
sealed class PresentationState {
    object Idle : PresentationState()
    object Blank : PresentationState()

    data class Lyrics(
        val set: ServiceSet,
        val allSlides: List<Slide>,           // flat list across all songs in set
        val currentSlideIndex: Int,           // operator's position
        val frozenDisplayIndex: Int? = null,  // null = not frozen
        val background: Background = Background.Black
    ) : PresentationState() {
        // derived — what the projection window actually shows
        val displaySlideIndex: Int get() = frozenDisplayIndex ?: currentSlideIndex
        val frozen: Boolean get() = frozenDisplayIndex != null
    }
}
```

**Why `frozenDisplayIndex: Int?` instead of `frozen: Boolean`:** Modeling the frozen slide index directly prevents the race condition where freeze and navigate happen in the same frame.

---

### 2. SQLDelight Schema — `:core:db`

**Purpose**: Single source of truth for all persisted data.  
**Location**: `core/db/src/main/sqldelight/dev/trinitychurch/lyrics/db/TrinityLyrics.sq`

Tables: `songs`, `song_sections`, `tags`, `song_tags`, `sets`, `set_items`, `settings`  
FTS5: `songs_fts` (content table, mirrors `songs.title + artist + body`)

Named queries (generated as Kotlin methods):
- `SongQueries`: `selectAll`, `selectById`, `search(query)`, `insert`, `update`, `softDelete`
- `SongSectionQueries`: `selectBySongId`, `insert`, `update`, `delete`, `reorder`
- `SetQueries`: `selectAll`, `selectById`, `insert`, `update`, `delete`
- `SetItemQueries`: `selectBySetId`, `insert`, `delete`, `updateSortOrder`
- `SettingsQueries`: `get(key)`, `upsert(key, value)`

---

### 3. SlideSplitter — `:feature:presentation`

**Purpose**: Pure stateless algorithm converting a `SongSection` body into `List<Slide>`.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/SlideSplitter.kt`

```kotlin
object SlideSplitter {
    fun split(section: SongSection, config: SlideConfig): List<Slide>
}
```

**Algorithm:**
1. Split `body` by `\n` → raw lines
2. Word-wrap each raw line at `config.maxCharsPerLine` → display lines
3. Accumulate display lines; when count hits `config.maxLinesPerSlide`, flush into a `Slide`
4. Trailing lines → final `Slide`
5. Empty body → `emptyList()`

**Hard rule**: TDD first. No `SlideSplitter` implementation without passing tests. Tests live in `feature/presentation/src/test/kotlin/.../SlideSplitterSpec.kt`.

---

### 4. SongRepository — `:core:db`

**Purpose**: Reactive data access for songs and sections.  
**Location**: `core/db/src/main/kotlin/.../db/SongRepository.kt`

```kotlin
class SongRepository(private val db: TrinityLyricsDatabase) {
    fun allSongs(): Flow<List<Song>>
    fun songById(id: String): Flow<Song?>
    fun search(query: String): Flow<List<Song>>
    suspend fun insert(song: Song)
    suspend fun update(song: Song)
    suspend fun softDelete(id: String, deletedAt: Long)
}
```

---

### 5. SetRepository — `:core:db`

**Purpose**: Reactive data access for service sets and their items.  
**Location**: `core/db/src/main/kotlin/.../db/SetRepository.kt`

```kotlin
class SetRepository(private val db: TrinityLyricsDatabase) {
    fun allSets(): Flow<List<ServiceSet>>
    fun setById(id: String): Flow<ServiceSet?>
    suspend fun createSet(set: ServiceSet)
    suspend fun addItem(item: SetItem)
    suspend fun removeItem(id: String)
    suspend fun reorderItems(setId: String, orderedIds: List<String>)
}
```

---

### 6. SettingsRepositoryImpl — `:core:db`

**Purpose**: Persists key-value settings to the SQLDelight `settings` table.  
**Location**: `core/db/src/main/kotlin/.../db/SettingsRepositoryImpl.kt`  
**Implements**: `SettingsRepository` interface from `:core:domain`

---

### 7. PresentationStateStore — `:feature:presentation`

**Purpose**: Single source of truth for what is currently presented. Koin singleton shared across both windows.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/PresentationStateStore.kt`

**Replaces** the stub in `:core:domain` — the stub must be deleted after this is wired.

```kotlin
class PresentationStateStore(private val slideSplitter: SlideSplitter) {
    private val _state = MutableStateFlow<PresentationState>(PresentationState.Idle)
    val state: StateFlow<PresentationState> = _state.asStateFlow()

    fun loadSet(set: ServiceSet, songs: Map<String, Song>, config: SlideConfig)
    fun advance()
    fun previous()
    fun jumpToSlide(index: Int)
    fun toggleBlank()
    fun toggleFreeze()
    fun clear()
}
```

**`loadSet` logic**: For each `SetItem`, get the `Song`, call `SlideSplitter.split()` on each section → flat `List<Slide>` across the whole set.

**`toggleBlank` logic**: If current state is `Blank`, restore saved pre-blank state. Otherwise, save current state to `_preBlankState` and emit `Blank`.

---

### 8. HolyricsSongParser — `:feature:import`

**Purpose**: Parse Holyrics JSON export into `List<Song>`.  
**Location**: `feature/import/src/main/kotlin/.../import/HolyricsSongParser.kt`

**Input format** (from real export):
```json
[{ "id": 1754779801229, "title": "23", "artist": "(Projeto Sola, 2017)",
   "lyrics": { "paragraphs": [{"number": 1, "description": "", "text": "O SENHOR..."}] } }]
```

**Mapping rules**:
- `song.title` → `Song.title`
- `song.artist` → `Song.artist`
- `paragraph.number` → `SongSection.sortOrder`
- `paragraph.description.ifBlank { "Estrofe ${paragraph.number}" }` → `SongSection.label`
- `paragraph.text` → `SongSection.body`
- All sections: `SectionType.VERSE` (no type metadata in export)
- `Song.id` = `UUID.randomUUID().toString()` (Holyrics IDs are longs, not UUIDs)

**DTOs** (`@Serializable`):
```kotlin
@Serializable data class HolyricsSong(val id: Long, val title: String, val artist: String = "",
    val lyrics: HolyricsLyrics)
@Serializable data class HolyricsLyrics(val paragraphs: List<HolyricsParagraph>)
@Serializable data class HolyricsParagraph(val number: Int, val description: String = "",
    val text: String)
```

---

### 9. PlainTextSongParser — `:feature:import`

**Purpose**: Parse bracketed plain-text lyrics into `List<SongSection>`.  
**Location**: `feature/import/src/main/kotlin/.../import/PlainTextSongParser.kt`

**Header mapping (case-insensitive)**:

| Header | SectionType |
|---|---|
| `Verse`, `Verso`, `Estrofe`, `V` | VERSE |
| `Chorus`, `Refrão`, `Refrao`, `C` | CHORUS |
| `Bridge`, `Ponte`, `B` | BRIDGE |
| `Pre-Chorus`, `Pre-Refrão` | PRE_CHORUS |
| `Outro`, `Final` | OUTRO |
| `Interlude`, `Interlúdio` | INTERLUDE |
| `Tag`, `Coda` | TAG |

**Algorithm**: Scan lines for `[...]` pattern → new section. Lines after a header are body. If no headers, one VERSE section with all text.

---

### 10. LocaleStore — `:core:domain`

**Purpose**: Loads + persists language selection; exposes `StateFlow<AppLocale>`.  
**Location**: `core/domain/src/main/kotlin/.../domain/LocaleStore.kt`  
**Design**: Per `.specs/features/i18n-language-selection/spec.md`

---

### 11. StringResources — `:core:ui`

**Purpose**: All visible UI strings via `LocalStrings.current`.  
**Location**: `core/ui/src/main/kotlin/.../ui/strings/`

Files:
- `StringResources.kt` — interface with all string properties
- `PtBrStrings.kt` — Portuguese implementation
- `EnStrings.kt` — English implementation
- `LocalStrings.kt` — `CompositionLocal<StringResources>` default = `PtBrStrings`

**Phase 1 string categories** (add properties incrementally as screens are built):
- Common: appName, ok, cancel, save, delete, edit, search, settings, back, add, confirm, close
- Language picker: chooseLanguage, chooseLanguageSubtitle, continueButton, portuguese, english
- Library: songs, noSongsFound, searchHint, newSong, importSongs
- Editor: songTitle, artist, sections, addSection, sectionType, sectionLabel, sectionBody, livePreview, save, discardChanges
- Set builder: serviceSets, newSet, setName, serviceDate, addSong, removeSong, startPresentation
- Import: importHolyrics, selectFile, foundSongs, importing, importComplete, importError, duplicateFound, skip, overwrite, plainTextImport, pasteLyrics
- Presentation: currentSlide, nextSlide, blank, freeze, exitPresentation, slideOf
- Settings: targetMonitor, fontSize, maxLinesPerSlide, language, monitor (with index + resolution)
- Errors: noSongsInSet, noSectionsInSong, fileMalformed

---

### 12. LibraryScreen — `:feature:lyrics`

**Purpose**: Song list with search and navigation to editor/import.  
**Location**: `feature/lyrics/src/main/kotlin/.../lyrics/LibraryScreen.kt`

UI:
- `TextField` search (debounced 150ms → FTS5 query)
- `LazyColumn` of `SongCard` items (title + artist + section count)
- FAB: "Nova Música" → SongEditScreen
- Action: "Importar" → ImportWizardScreen
- On song tap: SongEditScreen

---

### 13. SectionEditorComponent — `:feature:lyrics`

**Purpose**: Reusable section list with add/edit/delete and drag-to-reorder.  
**Location**: `feature/lyrics/src/main/kotlin/.../lyrics/SectionEditorComponent.kt`

- `LazyColumn` with drag handles using `ReorderableItem` (use `sh.calvin.reorderable:reorderable` library — must be added to build.gradle.kts)
- Each row: section type chip, label field, body `TextField`
- Live slide preview panel on the right (calls `SlideSplitter.split()` on body change, debounced)
- "Add section" button at bottom

---

### 14. SongEditScreen — `:feature:lyrics`

**Purpose**: Create/edit screen combining title/artist header + SectionEditorComponent.  
**Location**: `feature/lyrics/src/main/kotlin/.../lyrics/SongEditScreen.kt`

- Unsaved changes guard: prompt on back navigation
- Save: validate (title required, min 1 section) → `SongRepository.insert/update`

---

### 15. SetBuilderScreen — `:feature:lyrics`

**Purpose**: Create/edit service sets with song ordering.  
**Location**: `feature/lyrics/src/main/kotlin/.../lyrics/SetBuilderScreen.kt`

- Set name + date fields
- Reorderable list of set items (same `ReorderableItem` pattern as SectionEditor)
- "Add Song" opens search sheet → song added to bottom of list
- On "Start Presentation": validates set non-empty → `PresentationStateStore.loadSet()` → shows PresentationControls

---

### 16. ImportWizardScreen — `:feature:import`

**Purpose**: Step-by-step import of Holyrics JSON or plain-text songs.  
**Location**: `feature/import/src/main/kotlin/.../import/ImportWizardScreen.kt`

Steps:
1. Choose source: "Holyrics JSON" or "Texto puro / Plain text"
2. File picker (Holyrics) or text paste area (plain text)
3. Preview: songs found, duplicates listed
4. Confirm → import → success message with count

---

### 17. SettingsScreen

**Purpose**: Configure monitor, font, language.  
**Location**: `app/src/main/kotlin/.../app/SettingsScreen.kt` (app-level since it touches window config)

---

### 18. OperatorConsoleApp — `:feature:presentation`

**Purpose**: Operator-facing UI during an active presentation.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/OperatorConsoleApp.kt`

Layout:
```
┌──────────────────────────────────┬─────────────┐
│  CurrentSlidePanel (large)       │ NextSlide   │
│                                  │ Preview     │
├──────────────────────────────────┴─────────────┤
│  SlideThumbnailGrid (scrollable, click-to-jump)│
├────────────────────────────────────────────────┤
│  Controls: [Blank] [Freeze] [Exit]  3/15 slides│
└────────────────────────────────────────────────┘
```

- Keyboard shortcuts handled by `onKeyEvent` on the root `Box` with `focusRequester`
- Thumbnails: `LazyVerticalGrid(columns = Fixed(6))` of `SlideThumbnailCard`
- Highlighted thumbnail: `displaySlideIndex` (not `currentSlideIndex` when frozen)

---

### 19. SlideThumbnailCard — `:feature:presentation`

**Purpose**: Single clickable slide thumbnail in the grid.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/SlideThumbnailCard.kt`

- Renders slide text at small scale
- `isCurrentDisplay: Boolean` → highlighted border
- `isCurrent: Boolean` (operator position, may differ from display when frozen) → subtle indicator
- `onClick: () -> Unit` → `PresentationStateStore.jumpToSlide(index)`

---

### 20. PresentationWindowApp — `:feature:presentation`

**Purpose**: Read-only projection window observing `PresentationStateStore.state`.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/PresentationWindowApp.kt`

```kotlin
@Composable
fun PresentationWindowApp(store: PresentationStateStore) {
    val state by store.state.collectAsState()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            is PresentationState.Idle -> {} // black screen
            is PresentationState.Blank -> {} // black screen
            is PresentationState.Lyrics -> LyricsSlideView(s.allSlides[s.displaySlideIndex])
        }
    }
}
```

---

### 21. LyricsSlideView — `:feature:presentation`

**Purpose**: Renders a single `Slide`'s text lines centered on a black screen.  
**Location**: `feature/presentation/src/main/kotlin/.../presentation/LyricsSlideView.kt`

- `Column(horizontalAlignment = Center, verticalArrangement = Center)` inside `Box(fillMaxSize, black bg)`
- Each line: `Text(line, fontSize = config.fontSizeSp.sp, color = White, textAlign = Center)`
- Font size: `config.fontSizeSp` (from `SlideConfig`)

---

### 22. KeyboardShortcutHandler

**Purpose**: Maps key events to `PresentationStateStore` actions.  
**Location**: Inline in `OperatorConsoleApp.kt` (not a separate file — it's a `Modifier.onKeyEvent {}` lambda)

| Key | Action |
|---|---|
| Right, Space | `store.advance()` |
| Left | `store.previous()` |
| B | `store.toggleBlank()` |
| F | `store.toggleFreeze()` |
| Escape | `store.clear()` → navigate back to SetBuilder |

`onKeyEvent` consumes `KEY_DOWN` events only to avoid double-firing.

---

## Data Models

### SQLDelight Schema (full)

```sql
-- songs
CREATE TABLE songs (
  id            TEXT NOT NULL PRIMARY KEY,
  title         TEXT NOT NULL,
  artist        TEXT,
  source        TEXT,          -- 'holyrics' | 'plain_text' | NULL (manual)
  slide_config  TEXT,          -- JSON: SlideConfig override
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER        -- soft delete epoch ms
);

-- song_sections
CREATE TABLE song_sections (
  id           TEXT NOT NULL PRIMARY KEY,
  song_id      TEXT NOT NULL REFERENCES songs(id),
  label        TEXT NOT NULL,
  type         TEXT NOT NULL,  -- VERSE|CHORUS|BRIDGE|PRE_CHORUS|OUTRO|INTERLUDE|TAG
  body         TEXT NOT NULL,
  sort_order   INTEGER NOT NULL
);

-- tags (schema only; UI deferred to Phase 2)
CREATE TABLE tags (
  id    TEXT NOT NULL PRIMARY KEY,
  name  TEXT NOT NULL,
  color TEXT
);

CREATE TABLE song_tags (
  song_id TEXT NOT NULL REFERENCES songs(id),
  tag_id  TEXT NOT NULL REFERENCES tags(id),
  PRIMARY KEY (song_id, tag_id)
);

-- sets
CREATE TABLE sets (
  id           TEXT NOT NULL PRIMARY KEY,
  name         TEXT NOT NULL,
  service_date TEXT,           -- ISO date string, nullable
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL
);

-- set_items (Phase 1: only song items)
CREATE TABLE set_items (
  id         TEXT NOT NULL PRIMARY KEY,
  set_id     TEXT NOT NULL REFERENCES sets(id),
  song_id    TEXT REFERENCES songs(id),
  sort_order INTEGER NOT NULL
);

-- settings (key-value)
CREATE TABLE settings (
  key   TEXT NOT NULL PRIMARY KEY,
  value TEXT NOT NULL
);

-- FTS5 full-text search
CREATE VIRTUAL TABLE songs_fts USING fts5(
  title, artist, body,
  content='songs',
  content_rowid='rowid'
);
```

**Migrations**: Initial schema is migration 1 (`1.sqm`). Phase 1 does not require migration 2.

---

## Error Handling Strategy

| Scenario | Handling | User Impact |
|---|---|---|
| Holyrics JSON malformed | `try/catch SerializationException` → error dialog | "Arquivo inválido" dialog, no crash |
| Song save with empty title | Inline validation in editor | Red border + error text |
| Song save with no sections | Inline validation | "Adicione pelo menos uma estrofe" |
| DB write fails | Log + toast/snackbar | "Erro ao salvar. Tente novamente." |
| Projection monitor unplugged | Compose window auto-closes; operator window unaffected | Projection closes; service can continue on operator screen |
| Empty set started | Guard in SetBuilderScreen | Inline message, no navigation |

---

## Tech Decisions

| Decision | Choice | Rationale |
|---|---|---|
| `frozenDisplayIndex: Int?` in state | Nullable Int over Boolean flag | Avoids race condition between freeze toggle and advance; single field tracks both frozen state and frozen slide |
| Flat slide list across set | All songs in set pre-split into one `List<Slide>` | Enables single index for entire set navigation; cross-song advance is a free `index++` |
| `SlideSplitter` as `object` | Pure stateless singleton | Enables inline calling from editor preview without DI setup; trivially testable |
| Section drag-to-reorder | `sh.calvin.reorderable` library | Best-maintained CMP-compatible drag library as of 2026; avoids writing drag physics from scratch |
| Monitor selection | `GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices` | Standard JVM API; persisted index to `settings` table |
| `SettingsRepository` in `:core:domain` (interface) + `:core:db` (impl) | No `:core:settings` module | Avoids a new module for a thin wrapper; impl stays close to the DB module it uses |
| Keyboard handling | `Modifier.onKeyEvent` on root composable with explicit `FocusRequester` | CMP Desktop does not auto-focus windows; must `requestFocus()` on presentation start |
