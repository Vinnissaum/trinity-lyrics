# Phase 1 — Lyrics MVP Tasks

**Spec**: `.specs/features/phase-1-lyrics-mvp/spec.md`  
**Design**: `.specs/features/phase-1-lyrics-mvp/design.md`  
**Testing**: `.specs/codebase/TESTING.md`  
**Status**: Approved

---

## Execution Plan

### Phase A — Domain Foundation (Sequential)

```
T01
 ├──→ T02 [P]
 └──→ T03 [P]   (also: T08 [P], T09 [P] start from T01)
```

T01 must complete before anything else. T02 and T03 run in parallel after T01. T08 and T09 also start from T01, concurrent with T02/T03.

### Phase B — Logic Layer (Parallel after T02 / T01+T03)

```
T02 ──→ T04 [P] ─┐
T02 ──→ T05 [P] ─┤
T02 ──→ T06 [P] ─┤──→ T10 ──→ T11
T01+T03 → T07 [P]─┘
T01 ──→ T08 [P] ─┐
T01 ──→ T09 [P] ─┘
```

T04, T05, T06, T07, T08, T09 all run in parallel (different modules, no shared mutable state).  
T10 starts after T06. T11 starts after T10.

### Phase C — UI Screens (Parallel after deps)

```
T04+T11 ──→ T12 [P] ─┐
T01+T03+T11 → T13 [P]─┤──→ T14
T05+T11+T12 ──────────┤──→ T15 [P with T14]
T08+T09+T04+T11 ──────┤──→ T16 [P with T14, T15]
T06+T11 ──────────────┘──→ T17 [P with T14, T15, T16]
```

### Phase D — Presentation UI (Parallel after T07+T11)

```
T07+T11 ──→ T18 [P] ──→ T19
T07+T11 ──→ T20 [P]
```

### Phase E — Full Wiring (Sequential)

```
ALL (T14, T15, T16, T17, T19, T20) ──→ T21
```

---

## Task Breakdown

### T01: Define Core Domain Types

**What**: Create all pure-Kotlin domain data classes and sealed classes in `:core:domain`.  
**Where**:
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/Song.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/SongSection.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/SectionType.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/Slide.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/SlideConfig.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/PresentationState.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/Background.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/ServiceSet.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/SetItem.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/Tag.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/SettingsRepository.kt`

**Depends on**: None  
**Reuses**: Existing `PresentationStateStore` stub in `:core:domain` — **delete** the stub after this task; real store goes in `:feature:presentation` (T07)  
**Requirement**: P1-20..P1-34 (all presentation state), P1-01..P1-07 (song model), P1-08..P1-12 (section model)

**Key types to implement** (verbatim from design.md):

```kotlin
// PresentationState.kt
sealed class PresentationState {
    object Idle : PresentationState()
    object Blank : PresentationState()
    data class Lyrics(
        val set: ServiceSet,
        val allSlides: List<Slide>,
        val currentSlideIndex: Int,
        val frozenDisplayIndex: Int? = null,
        val background: Background = Background.Black
    ) : PresentationState() {
        val displaySlideIndex: Int get() = frozenDisplayIndex ?: currentSlideIndex
        val frozen: Boolean get() = frozenDisplayIndex != null
    }
}

// SlideConfig.kt
data class SlideConfig(
    val maxLinesPerSlide: Int = 4,
    val maxCharsPerLine: Int = 60,
    val fontSizeSp: Int = 48
)
```

**Done when**:
- [x] All 11 files compile with `./gradlew :core:domain:build`
- [x] `PresentationState.Lyrics.displaySlideIndex` returns `frozenDisplayIndex` when set, else `currentSlideIndex`
- [x] `PresentationState.Lyrics.frozen` returns `true` iff `frozenDisplayIndex != null`
- [x] Stub `PresentationStateStore` deleted from `:core:domain`
- [x] Gate: `./gradlew :core:domain:build`

**Tests**: None (pure data classes — no logic to test)  
**Gate**: build

---

### T02: Define SQLDelight Schema

**What**: Create the full SQLDelight `.sq` schema file with all Phase 1 tables and named queries.  
**Where**: `core/db/src/main/sqldelight/dev/trinitychurch/lyrics/db/TrinityLyrics.sq`  
**Depends on**: T01  
**Reuses**: SQLDelight config already present in `core/db/build.gradle.kts`  
**Requirement**: P1-01..P1-07 (song persistence), P1-08..P1-12 (section persistence), P1-16..P1-19 (set persistence)

**Tables**: `songs`, `song_sections`, `tags`, `song_tags`, `sets`, `set_items`, `settings`  
**FTS5**: `songs_fts` virtual table on title + artist + body  
**Named queries** to define:
- `SongQueries`: `selectAll`, `selectById`, `selectNonDeleted`, `search(query: String)`, `insert`, `update`, `softDelete(id, deletedAt)`
- `SongSectionQueries`: `selectBySongId`, `insert`, `update`, `delete`, `updateSortOrder`
- `SetQueries`: `selectAll`, `selectById`, `insert`, `update`, `delete`
- `SetItemQueries`: `selectBySetId`, `insert`, `delete`, `updateSortOrder`
- `SettingsQueries`: `get(key: String)`, `upsert(key: String, value: String)`

**Done when**:
- [x] `./gradlew generateSqlDelightInterface` succeeds with no errors
- [x] Generated `TrinityLyricsDatabase.kt` and all query interfaces are present in `core/db/build/generated/`
- [x] All 7 tables + FTS5 virtual table present in schema
- [x] Gate: `./gradlew :core:db:generateSqlDelightInterface`

**Tests**: None at this step — DAO integration tests are co-located with T04/T05/T06  
**Gate**: build

---

### T03: Implement SlideSplitter with TDD [P with T02]

**What**: Implement `SlideSplitter.split()` using TDD — write failing tests first, then implement.  
**Where**:
- `feature/presentation/src/test/kotlin/dev/trinitychurch/lyrics/presentation/SlideSplitterSpec.kt` (write FIRST)
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/SlideSplitter.kt`

**Depends on**: T01  
**Reuses**: `SongSection`, `Slide`, `SlideConfig` from T01  
**Requirement**: P1-20, P1-24 (slide generation is the foundation for all navigation)

**TDD test cases (write these before implementing)**:
1. Empty body → `emptyList()`
2. Single line under maxChars → one Slide with one line
3. Exactly `maxLinesPerSlide` lines → one Slide
4. `maxLinesPerSlide + 1` lines → two Slides (1 + N remaining)
5. Line over `maxCharsPerLine` → word-wrapped to multiple display lines
6. Line with no spaces over limit → hard-break at `maxCharsPerLine`
7. Multiple sections via `split()` called per section → independent slide lists
8. Body with blank lines (from Holyrics paragraphs) → blank lines discarded or treated as empty

**Done when**:
- [x] All 8+ test cases written before implementation (RED phase confirmed)
- [x] `SlideSplitter.split()` passes all tests (GREEN phase)
- [x] Gate passes: `./gradlew :feature:presentation:test`
- [x] Test count: ≥ 8 tests pass in `SlideSplitterSpec`
- [x] No `SlideSplitter` implementation exists before all tests are written

**Tests**: Unit  
**Gate**: `./gradlew :feature:presentation:test`

---

### T04: Implement SongRepository [P]

**What**: Implement `SongRepository` with full CRUD, reactive flows, and FTS5 search.  
**Where**: `core/db/src/main/kotlin/dev/trinitychurch/lyrics/db/SongRepository.kt`  
**Depends on**: T02  
**Reuses**: SQLDelight generated `TrinityLyricsDatabase` interface  
**Requirement**: P1-07, P1-08, P1-10, P1-13, P1-14, P1-15

**Methods**:
```kotlin
class SongRepository(private val db: TrinityLyricsDatabase) {
    fun allSongs(): Flow<List<Song>>              // non-deleted only
    fun songById(id: String): Flow<Song?>
    fun search(query: String): Flow<List<Song>>   // FTS5 query
    suspend fun insert(song: Song)
    suspend fun update(song: Song)
    suspend fun softDelete(id: String)
    suspend fun insertAll(songs: List<Song>, source: String)  // for import
}
```

**Note**: All DB operations run on `Dispatchers.IO`; `.asFlow()` uses SQLDelight's coroutine extension.

**Done when**:
- [x] All methods implemented
- [x] Integration test: insert song → `allSongs()` emits it
- [x] Integration test: search by body text → song appears in results
- [x] Integration test: softDelete → song absent from `allSongs()`, present with `deleted_at` set
- [x] Gate: `./gradlew :core:db:test`
- [x] Test count: ≥ 5 integration tests pass

**Tests**: Integration  
**Gate**: `./gradlew :core:db:test`

---

### T05: Implement SetRepository [P]

**What**: Implement `SetRepository` with set CRUD and item reordering.  
**Where**: `core/db/src/main/kotlin/dev/trinitychurch/lyrics/db/SetRepository.kt`  
**Depends on**: T02  
**Reuses**: SQLDelight generated query interfaces  
**Requirement**: P1-16, P1-17, P1-18, P1-19

**Methods**:
```kotlin
class SetRepository(private val db: TrinityLyricsDatabase) {
    fun allSets(): Flow<List<ServiceSet>>
    fun setById(id: String): Flow<ServiceSet?>
    suspend fun createSet(set: ServiceSet)
    suspend fun updateSet(set: ServiceSet)
    suspend fun deleteSet(id: String)
    suspend fun addItem(item: SetItem)
    suspend fun removeItem(id: String)
    suspend fun reorderItems(setId: String, orderedIds: List<String>)
}
```

**Done when**:
- [x] Integration test: create set + add 3 items → `setById()` emits correct order
- [x] Integration test: reorder items → emitted set reflects new sort_order
- [x] Integration test: remove item → song remains in songs table
- [x] Gate: `./gradlew :core:db:test`
- [x] Test count: ≥ 4 integration tests pass

**Tests**: Integration  
**Gate**: `./gradlew :core:db:test`

---

### T06: Implement SettingsRepository [P]

**What**: Define `SettingsRepository` interface in `:core:domain` and implement `SettingsRepositoryImpl` in `:core:db`.  
**Where**:
- `core/domain/src/main/kotlin/.../domain/SettingsRepository.kt` (interface — already declared in T01)
- `core/db/src/main/kotlin/dev/trinitychurch/lyrics/db/SettingsRepositoryImpl.kt`

**Depends on**: T01, T02  
**Reuses**: `SettingsQueries` from T02 schema  
**Requirement**: P1-37, P1-38, P1-39 (settings persistence for monitor, font, language)

**Interface methods**: `getString(key, default)`, `putString(key, value)`, `getInt(key, default)`, `putInt(key, value)`

**Done when**:
- [x] Integration test: `putString("k","v")` → `getString("k", "")` returns "v"
- [x] Integration test: `getString("missing", "default")` returns "default"
- [x] Integration test: `putInt` → `getInt` round-trip
- [x] Gate: `./gradlew :core:db:test`
- [x] Test count: ≥ 3 integration tests pass

**Tests**: Integration  
**Gate**: `./gradlew :core:db:test`

---

### T07: Implement PresentationStateStore [P]

**What**: Implement full `PresentationStateStore` with all state transitions, TDD.  
**Where**:
- `feature/presentation/src/test/kotlin/dev/trinitychurch/lyrics/presentation/PresentationStateStoreSpec.kt` (write FIRST)
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/PresentationStateStore.kt`

**Depends on**: T01, T03  
**Reuses**: `SlideSplitter` from T03, all domain types from T01  
**Requirement**: P1-20..P1-31 (all navigation + blank + freeze)

**Methods to implement** (see design.md §7):
```kotlin
fun loadSet(set: ServiceSet, songs: Map<String, Song>, config: SlideConfig)
fun advance()      // → currentSlideIndex++, cap at allSlides.lastIndex
fun previous()     // → currentSlideIndex--, floor at 0
fun jumpToSlide(index: Int)
fun toggleBlank()  // Idle/Lyrics → Blank; Blank → restore
fun toggleFreeze() // null → set frozenDisplayIndex; not null → clear
fun clear()        // → Idle
```

**TDD test cases (write before implementing)**:
1. `loadSet` → state is `Lyrics`, `currentSlideIndex = 0`
2. `advance()` → index increments
3. `advance()` at last slide → index unchanged (P1-22)
4. `previous()` at first slide → index unchanged (P1-23)
5. `jumpToSlide(5)` → `currentSlideIndex = 5`
6. `toggleBlank()` from Lyrics → Blank
7. `toggleBlank()` from Blank → restores Lyrics at same index (P1-25)
8. `toggleFreeze()` from not-frozen → `frozenDisplayIndex = currentSlideIndex` (P1-26)
9. `toggleFreeze()` from frozen → `frozenDisplayIndex = null`
10. Frozen: `advance()` increments `currentSlideIndex` but `displaySlideIndex` unchanged
11. Frozen: `jumpToSlide()` updates `currentSlideIndex` but `displaySlideIndex` unchanged
12. `clear()` → Idle
13. Multi-song set: advance past last slide of song 1 → first slide of song 2

**Done when**:
- [x] All 13+ test cases written before implementation
- [x] All tests pass
- [x] Gate: `./gradlew :feature:presentation:test`
- [x] Test count: ≥ 13 tests in `PresentationStateStoreSpec`
- [x] Koin module in `AppModule` registers `PresentationStateStore` as `single {}`

**Tests**: Unit  
**Gate**: `./gradlew :feature:presentation:test`

---

### T08: Implement HolyricsSongParser [P]

**What**: TDD implementation of `HolyricsSongParser` that converts Holyrics JSON → `List<Song>`.  
**Where**:
- `feature/import/src/test/kotlin/dev/trinitychurch/lyrics/import/HolyricsSongParserSpec.kt` (write FIRST)
- `feature/import/src/main/kotlin/dev/trinitychurch/lyrics/import/HolyricsSongParser.kt`
- `feature/import/src/main/kotlin/dev/trinitychurch/lyrics/import/HolyricsDto.kt` (`@Serializable` DTOs)

**Depends on**: T01  
**Reuses**: `Song`, `SongSection`, `SectionType` from T01; `kotlinx.serialization` (already in build)  
**Requirement**: P1-01..P1-07

**DTOs to define** (from design.md §8):
```kotlin
@Serializable data class HolyricsSong(val id: Long, val title: String,
    val artist: String = "", val lyrics: HolyricsLyrics)
@Serializable data class HolyricsLyrics(val paragraphs: List<HolyricsParagraph>)
@Serializable data class HolyricsParagraph(val number: Int,
    val description: String = "", val text: String)
```

**TDD test cases**:
1. Valid JSON with 3 songs → returns 3 `Song` objects
2. Empty `description` → section label is `"Estrofe 1"` (PT-BR locale default)
3. Non-empty `description` → section label uses it
4. Each paragraph's `text` becomes `SongSection.body`
5. `paragraph.number` → `SongSection.sortOrder`
6. All sections have `SectionType.VERSE`
7. Generated `Song.id` is a valid UUID (not the Holyrics Long id)
8. Empty paragraphs array → song imported with 0 sections
9. Malformed JSON → throws a typed exception (not `SerializationException` leaking to caller)

**Done when**:
- [x] All 9+ tests written before implementation
- [x] Parser correctly handles the 3-song sample from the spec
- [x] Gate: `./gradlew :feature:import:test`
- [x] Test count: ≥ 9 tests pass

**Tests**: Unit  
**Gate**: `./gradlew :feature:import:test`

---

### T09: Implement PlainTextSongParser [P]

**What**: TDD implementation of `PlainTextSongParser` that converts bracketed plain text → `List<SongSection>`.  
**Where**:
- `feature/import/src/test/kotlin/dev/trinitychurch/lyrics/import/PlainTextSongParserSpec.kt` (write FIRST)
- `feature/import/src/main/kotlin/dev/trinitychurch/lyrics/import/PlainTextSongParser.kt`

**Depends on**: T01  
**Reuses**: `SongSection`, `SectionType` from T01  
**Requirement**: P1-35, P1-36, P1-37

**Header mapping** (case-insensitive, from design.md §9):

| Headers | SectionType |
|---|---|
| Verse, Verso, Estrofe, V | VERSE |
| Chorus, Refrão, Refrao, C | CHORUS |
| Bridge, Ponte, B | BRIDGE |
| Pre-Chorus, Pre-Refrão | PRE_CHORUS |
| Outro, Final | OUTRO |
| Tag, Coda | TAG |

**TDD test cases**:
1. `"[Verse 1]\nLine1\nLine2"` → one VERSE section, body = "Line1\nLine2"
2. `"[Refrão]\nHaleluia"` → one CHORUS section
3. Multiple headers → multiple sections with correct types
4. No headers → one VERSE section with all text
5. Empty input → empty list
6. Unknown header `[Intro]` → VERSE (default fallback)
7. Section body lines preserve newlines

**Done when**:
- [x] All 7+ tests written before implementation
- [x] Gate: `./gradlew :feature:import:test`
- [x] Test count: ≥ 7 tests pass in `PlainTextSongParserSpec`

**Tests**: Unit  
**Gate**: `./gradlew :feature:import:test`

---

### T10: Implement AppLocale + LocaleStore

**What**: Implement `AppLocale` enum and `LocaleStore` as specified in the i18n spec.  
**Where**:
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/AppLocale.kt`
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/LocaleStore.kt`

**Depends on**: T06 (needs `SettingsRepository` implementation wired)  
**Reuses**: `SettingsRepository` from T01/T06  
**Requirement**: P1-39 (i18n spec L-01..L-09)

**Implementation** (verbatim from i18n spec):
```kotlin
enum class AppLocale(val code: String, val displayName: String) {
    PT_BR("pt-BR", "Português (Brasil)"),
    EN("en", "English")
}

class LocaleStore(private val settings: SettingsRepository) {
    private val _locale = MutableStateFlow(AppLocale.PT_BR)
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()
    suspend fun load() { /* load from settings */ }
    suspend fun setLocale(locale: AppLocale)
    fun isFirstRun(): Boolean  // settings key "app.locale" not yet written
}
```

**Done when**:
- [x] Unit test: `load()` with no saved value → `locale.value == PT_BR`
- [x] Unit test: `load()` with saved "en" → `locale.value == EN`
- [x] Unit test: `setLocale(EN)` → persisted to settings
- [x] Unit test: `isFirstRun()` true when key absent, false when present
- [x] Gate: `./gradlew :core:domain:test`
- [x] Test count: ≥ 4 tests pass

**Tests**: Unit  
**Gate**: `./gradlew :core:domain:test`

---

### T11: Implement StringResources Infrastructure

**What**: Define `StringResources` interface, `PtBrStrings`, `EnStrings`, and `LocalStrings` CompositionLocal with all Phase 1 strings.  
**Where**:
- `core/ui/src/main/kotlin/dev/trinitychurch/lyrics/ui/strings/StringResources.kt`
- `core/ui/src/main/kotlin/dev/trinitychurch/lyrics/ui/strings/PtBrStrings.kt`
- `core/ui/src/main/kotlin/dev/trinitychurch/lyrics/ui/strings/EnStrings.kt`
- `core/ui/src/main/kotlin/dev/trinitychurch/lyrics/ui/strings/LocalStrings.kt`

**Depends on**: T10  
**Reuses**: `AppLocale` from T10, pattern from `.specs/features/i18n-language-selection/spec.md`  
**Requirement**: P1-39

**String categories to cover** (all Phase 1 visible strings):
- Common: `appName, ok, cancel, save, delete, edit, search, settings, back, add, confirm, close, error`
- Language picker: `chooseLanguage, chooseLanguageSubtitle, continueButton, portuguese, english`
- Library: `songs, noSongsFound, searchHint, newSong, importSongs, deleteConfirm`
- Editor: `songTitle, artist, sections, addSection, sectionType, sectionLabel, sectionBody, livePreview`
- Section types: `verse, chorus, bridge, preChorus, outro, interlude, tag`
- Set builder: `serviceSets, newSet, setName, serviceDate, addSong, removeSong, startPresentation, setEmpty`
- Import: `importHolyrics, selectFile, foundSongs, importing, importComplete, importError, duplicateFound, skip, overwrite, plainTextImport, pasteLyrics`
- Presentation: `currentSlide, nextSlide, blank, freeze, exitPresentation, slideOf, songOf`
- Settings: `targetMonitor, fontSize, maxLinesPerSlide, language`
- Errors: `fileMalformed, noSectionsInSong, titleRequired`

**Done when**:
- [x] `StringResources` interface has properties for all categories above
- [x] `PtBrStrings` and `EnStrings` both implement every property
- [x] `LocalStrings = compositionLocalOf<StringResources> { PtBrStrings }`
- [x] `./gradlew :core:ui:build` passes with no errors
- [x] Gate: `./gradlew :core:ui:build`

**Tests**: None (pure string constants — no logic)  
**Gate**: build

---

### T12: Implement LibraryScreen [P]

**What**: Song library Composable with live FTS5 search and navigation to editor.  
**Where**: `feature/lyrics/src/main/kotlin/dev/trinitychurch/lyrics/lyrics/LibraryScreen.kt`  
**Depends on**: T04, T11  
**Reuses**: `SongRepository.allSongs()`, `SongRepository.search()`, `LocalStrings` from T11  
**Requirement**: P1-13, P1-14, P1-15

**UI elements**:
- `OutlinedTextField` for search (debounced 150ms via `flow.debounce`)
- `LazyColumn` of `SongCard` (title, artist, section count)
- Empty state composable when list empty
- FAB or top bar button: "Nova Música"
- Top bar action: "Importar"
- Each card: tap → navigate to SongEditScreen; long-press → delete confirm dialog

**Done when**:
- [x] UI test: insert 3 songs via repo → library shows all 3
- [x] UI test: type search term → list filters to matching songs
- [x] UI test: clear search → all songs shown
- [x] UI test: soft-deleted song does NOT appear
- [x] Gate: `./gradlew :feature:lyrics:test`
- [x] Test count: ≥ 4 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:lyrics:test`

---

### T13: Implement SectionEditorComponent [P]

**What**: Reusable Composable for editing an ordered list of song sections with live slide preview.  
**Where**: `feature/lyrics/src/main/kotlin/dev/trinitychurch/lyrics/lyrics/SectionEditorComponent.kt`  
**Depends on**: T01, T03, T11  
**Reuses**: `SlideSplitter` from T03, `SectionType`, `SongSection` from T01, `LocalStrings` from T11  
**Requirement**: P1-09, P1-11, P1-12

**UI elements**:
- `LazyColumn` with `ReorderableItem` (add `sh.calvin.reorderable:reorderable` dependency to `feature/lyrics/build.gradle.kts`)
- Each row: type `DropdownMenu`, label `TextField`, body `TextField` (multiline)
- "Add section" button
- Delete icon per section
- Right panel: live slide preview (`LazyColumn` of `SlidePreviewCard`) — calls `SlideSplitter.split()` on body change, debounced 300ms

**Done when**:
- [x] UI test: add 2 sections → both visible in list
- [x] UI test: change body text → preview panel shows updated slides
- [x] UI test: drag section 1 below section 2 → order swapped in state
- [x] Gate: `./gradlew :feature:lyrics:test`
- [x] Test count: ≥ 3 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:lyrics:test`

---

### T14: Implement SongEditScreen

**What**: Create/edit screen for a song — title + artist header + embedded SectionEditorComponent.  
**Where**: `feature/lyrics/src/main/kotlin/dev/trinitychurch/lyrics/lyrics/SongEditScreen.kt`  
**Depends on**: T12, T13  
**Reuses**: `SectionEditorComponent` from T13, `SongRepository` from T04, `LocalStrings` from T11  
**Requirement**: P1-08, P1-09, P1-10, P1-11, P1-12

**UI elements**:
- Title `TextField` (required — shows error if blank on save)
- Artist `TextField` (optional)
- `SectionEditorComponent` filling remaining space
- Top bar: Save button + back arrow (with "Descartar alterações?" confirm dialog if dirty)
- Save: validates title non-blank AND at least 1 section → `SongRepository.insert/update`

**Done when**:
- [x] UI test: fill title + add section → save → appears in library
- [x] UI test: save with empty title → inline error visible, no navigation
- [x] UI test: save with no sections → inline error visible
- [x] UI test: back with unsaved changes → confirm dialog shown
- [x] Gate: `./gradlew :feature:lyrics:test`
- [x] Test count: ≥ 4 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:lyrics:test`

---

### T15: Implement SetBuilderScreen [P]

**What**: Service set editor — name/date header, reorderable song list, start presentation button.  
**Where**: `feature/lyrics/src/main/kotlin/dev/trinitychurch/lyrics/lyrics/SetBuilderScreen.kt`  
**Depends on**: T05, T11, T12  
**Reuses**: `SetRepository` from T05, `LibraryScreen` song card pattern from T12, `LocalStrings` from T11  
**Requirement**: P1-16..P1-19

**UI elements**:
- Set name `TextField` + service date `DatePicker` (or plain text field)
- `LazyColumn` of `SetItemCard` (reorderable, same library as T13)
- "Adicionar Música" → opens song search bottom sheet (reuses LibraryScreen search)
- Remove per item: swipe-to-delete or delete icon
- "Iniciar Apresentação" button → validates set non-empty → `PresentationStateStore.loadSet()` → navigate to presentation mode
- Guard: "Adicione músicas ao culto primeiro" if set empty when start pressed

**Done when**:
- [x] UI test: add 3 songs → reorder → set items in new order
- [x] UI test: remove song → song still in library (verify via SongRepository)
- [x] UI test: start with empty set → guard message shown, no navigation
- [x] Gate: `./gradlew :feature:lyrics:test`
- [x] Test count: ≥ 3 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:lyrics:test`

---

### T16: Implement ImportWizardScreen [P]

**What**: Step-by-step import wizard for Holyrics JSON and plain-text songs.  
**Where**: `feature/import/src/main/kotlin/dev/trinitychurch/lyrics/import/ImportWizardScreen.kt`  
**Depends on**: T08, T09, T04, T11  
**Reuses**: `HolyricsSongParser` from T08, `PlainTextSongParser` from T09, `SongRepository.insertAll()` from T04  
**Requirement**: P1-01..P1-07, P1-35..P1-36

**Steps**:
1. Source chooser: "Holyrics JSON" button | "Texto Puro / Plain Text" button
2a. Holyrics: file picker dialog (`FileDialog` from AWT — standard on Desktop) → parse → preview count
2b. Plain text: `TextField` (multiline paste area) → "Analisar / Analyze" button → show section preview
3. Preview: "Encontradas N músicas" + duplicate warning list
4. "Confirmar Importação" → `SongRepository.insertAll()` → success snackbar with count
5. Error state: malformed file → error card with message, "Tentar Novamente" button

**Done when**:
- [x] UI test (Holyrics path): mock parser returns 3 songs → preview shows "3 músicas encontradas"
- [x] UI test: confirm → `SongRepository.insertAll()` called with 3 songs
- [x] UI test: parser throws exception → error UI shown (no crash)
- [x] Gate: `./gradlew :feature:import:test`
- [x] Test count: ≥ 3 UI tests pass (4 tests: includes P1-05 duplicate skip/overwrite)

**Tests**: UI  
**Gate**: `./gradlew :feature:import:test`

---

### T17: Implement SettingsScreen [P]

**What**: Settings screen for monitor selection, font size, max lines per slide, and language.  
**Where**: `app/src/main/kotlin/dev/trinitychurch/lyrics/app/SettingsScreen.kt`  
**Depends on**: T06, T10, T11  
**Reuses**: `SettingsRepositoryImpl` from T06, `LocaleStore` from T10, `LocalStrings` from T11  
**Requirement**: P1-37, P1-38, P1-39

**UI elements**:
- Monitor list: `GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices` → `RadioButton` list showing "Monitor N — WIDTHxHEIGHT"
- Font size: `Slider` (24..96 sp) + current value label
- Max lines per slide: `Slider` (2..8) + current value label
- Language: `RadioButton` group for PT-BR / EN → `LocaleStore.setLocale()` on change → UI recomposes immediately
- Changes auto-saved on each interaction (no separate Save button for settings)

**Done when**:
- [x] `./gradlew :app:build` passes
- [ ] Manual: open settings → monitor list shows all connected monitors with resolution
- [ ] Manual: change font size → slide preview (visible in editor) reflects new size on next open
- [ ] Manual: change language to EN → all visible strings switch to English immediately
- [x] Gate: `./gradlew :app:build`

**Tests**: None (manual verification; no pure logic to unit-test; UI test skipped — hardware-dependent monitor enumeration)  
**Gate**: build

---

### T18: Implement SlideThumbnailCard + SlideThumbnailGrid [P]

**What**: Composables for the scrollable slide thumbnail grid with click-to-jump.  
**Where**:
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/SlideThumbnailCard.kt`
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/SlideThumbnailGrid.kt`

**Depends on**: T07, T11  
**Reuses**: `PresentationStateStore.state`, `Slide` from T01, `LocalStrings` from T11  
**Requirement**: P1-28..P1-31

**`SlideThumbnailCard`**:
- `slide: Slide`, `isCurrentDisplay: Boolean`, `isCurrentOperator: Boolean`, `onClick: () -> Unit`
- `isCurrentDisplay = true` → highlighted border (e.g. `2.dp` accent-colored border)
- `isCurrentOperator = true && !isCurrentDisplay` → subtle dimmed indicator (operator is ahead/behind display)
- Shows slide text lines at small scale (e.g. 10.sp)

**`SlideThumbnailGrid`**:
- `LazyVerticalGrid(columns = Fixed(6))` of `SlideThumbnailCard`
- Auto-scrolls to keep current display slide visible (`LazyGridState.animateScrollToItem`)
- `onClick` → `store.jumpToSlide(index)`

**Done when**:
- [x] UI test: grid with 10 slides → clicking slide 7 calls `jumpToSlide(7)`
- [x] UI test: `displaySlideIndex = 3` → thumbnail 3 has highlighted border
- [x] UI test: frozen state (`frozenDisplayIndex = 2`, `currentSlideIndex = 5`) → thumbnail 2 highlighted, thumbnail 5 has operator indicator
- [x] Gate: `./gradlew :feature:presentation:test`
- [x] Test count: ≥ 3 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:presentation:test`

---

### T19: Implement OperatorConsoleApp

**What**: Full operator console layout combining current/next slide panels, thumbnail grid, controls bar, and keyboard shortcut handler.  
**Where**: `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/OperatorConsoleApp.kt`  
**Depends on**: T07, T11, T18  
**Reuses**: `SlideThumbnailGrid` from T18, `PresentationStateStore` from T07, `LocalStrings` from T11  
**Requirement**: P1-20..P1-31

**Layout** (see design.md §18):
```
Row {
  Column(weight 0.7) {
    CurrentSlidePanel(large — renders Slide.lines)
    NextSlidePreview(small — renders next slide if exists)
  }
  SlideThumbnailGrid(weight 0.3, scrollable)
}
ControlsBar { BlankButton, FreezeButton, ExitButton, "3/15 slides" progress }
```

**Keyboard handler** (`Modifier.onKeyEvent` on root `Box`, `focusRequester.requestFocus()` on load):

| Key | Action |
|---|---|
| Right, Space | `store.advance()` |
| Left | `store.previous()` |
| B | `store.toggleBlank()` |
| F | `store.toggleFreeze()` |
| Escape | `store.clear()` then navigate back |

Key events: consume `KEY_DOWN` only.

**Done when**:
- [x] UI test: `PresentationState.Lyrics` with 5 slides → current slide panel shows slide 0 text
- [x] UI test: `advance()` → current panel updates, previous slide shown at correct index
- [x] UI test: Blank button click → `store.toggleBlank()` called
- [x] Gate: `./gradlew :feature:presentation:test`
- [x] Test count: ≥ 3 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:presentation:test`

---

### T20: Implement PresentationWindowApp + LyricsSlideView [P]

**What**: Read-only projection window composable and the lyrics slide renderer.  
**Where**:
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/PresentationWindowApp.kt`
- `feature/presentation/src/main/kotlin/dev/trinitychurch/lyrics/presentation/LyricsSlideView.kt`

**Depends on**: T07, T11  
**Reuses**: `PresentationStateStore.state`, `PresentationState` from T01  
**Requirement**: P1-32..P1-34

**`PresentationWindowApp`**:
```kotlin
val state by store.state.collectAsState()
Box(Modifier.fillMaxSize().background(Color.Black)) {
    when (val s = state) {
        is Idle, is Blank -> { /* black */ }
        is Lyrics -> LyricsSlideView(s.allSlides[s.displaySlideIndex], config)
    }
}
```

**`LyricsSlideView`**:
- `Column(horizontalAlignment = CenterHorizontally, verticalArrangement = Center)` inside `Box(fillMaxSize, black)`
- Each line: `Text(fontSize = config.fontSizeSp.sp, color = Color.White, textAlign = TextAlign.Center)`
- No transitions at MVP (cut — just recompose)

**Done when**:
- [x] UI test: `Idle` state → screen fully black
- [x] UI test: `Lyrics` state with slide having 2 lines → both lines rendered centered
- [x] UI test: `Blank` state → screen fully black (same as Idle visually)
- [x] Gate: `./gradlew :feature:presentation:test`
- [x] Test count: ≥ 3 UI tests pass

**Tests**: UI  
**Gate**: `./gradlew :feature:presentation:test`

---

### T21: Full App Wiring — Main.kt + AppModule + Navigation

**What**: Wire all modules into a running app: Koin DI, navigation, two-window lifecycle, monitor positioning, first-run language gate.  
**Where**:
- `app/src/main/kotlin/dev/trinitychurch/lyrics/app/Main.kt` (rewrite)
- `app/src/main/kotlin/dev/trinitychurch/lyrics/app/di/AppModule.kt` (extend)

**Depends on**: T10, T11, T14, T15, T16, T17, T19, T20  
**Reuses**: Phase 0 `Main.kt` two-window skeleton; all screen composables from T14..T20  
**Requirement**: P1-32, P1-37, P1-39

**AppModule bindings** (Koin `single {}`):
- `TrinityLyricsDatabase` (SQLDelight — in-memory for tests, file-based in production)
- `SongRepository`, `SetRepository`, `SettingsRepositoryImpl`
- `PresentationStateStore`
- `LocaleStore`

**Main.kt structure**:
```kotlin
fun main() {
    startKoin { modules(AppModule) }
    application {
        val localeStore = koinGet<LocaleStore>()
        LaunchedEffect(Unit) { localeStore.load() }
        val locale by localeStore.locale.collectAsState()
        val strings = when (locale) { AppLocale.PT_BR -> PtBrStrings; AppLocale.EN -> EnStrings }
        CompositionLocalProvider(LocalStrings provides strings) {
            // First-run gate
            if (localeStore.isFirstRun()) {
                Window("Trinity Lyrics") { LanguagePickerScreen(localeStore) }
            } else {
                // Operator window with nav host
                Window("Trinity Lyrics") { OperatorNavHost(...) }
                // Projection window — conditional on presentation state
                val state by store.state.collectAsState()
                if (state !is PresentationState.Idle) {
                    val monitor = settings.getInt("presentation.monitor_index", 1)
                    val bounds = GraphicsEnvironment...screenDevices[monitor].bounds
                    Window(undecorated=true, alwaysOnTop=true, state=WindowState(position=bounds...)) {
                        PresentationWindowApp(store)
                    }
                }
            }
        }
    }
}
```

**Navigation** (`OperatorNavHost` — simple state-based nav, no Decompose required at this scale):
```
LibraryScreen  ←→  SongEditScreen
     ↓
SetBuilderScreen  →  OperatorConsoleApp
     ↓
ImportWizardScreen
SettingsScreen
```

**Done when**:
- [x] `./gradlew :app:run` launches without crash
- [ ] First run: language picker visible; selecting EN → UI in English; selecting PT-BR → UI in Portuguese
- [ ] Second run (locale persisted): main UI opens directly
- [ ] LibraryScreen → tap song → SongEditScreen → back → LibraryScreen
- [ ] SetBuilderScreen → Start Presentation → projection window opens (if second monitor available) OR operator console shows in single-monitor mode
- [x] Gate: `./gradlew build`

**Tests**: None (manual integration checklist)  
**Gate**: `./gradlew build`

---

## Task Granularity Check

| Task | Scope | Status |
|---|---|---|
| T01: Domain types | 11 files, all pure data/sealed — zero logic | ✅ DONE |
| T02: SQLDelight schema | 1 .sq file | ✅ DONE |
| T03: SlideSplitter | 1 pure function + test file | ✅ DONE |
| T04: SongRepository | 1 class + integration tests | ✅ DONE |
| T05: SetRepository | 1 class + integration tests | ✅ DONE |
| T06: SettingsRepository | 2 files (interface + impl) + tests | ✅ DONE |
| T07: PresentationStateStore | 1 class + test file | ✅ DONE |
| T08: HolyricsSongParser | 1 parser + DTOs + test | ✅ DONE |
| T09: PlainTextSongParser | 1 parser + test | ✅ DONE |
| T10: AppLocale + LocaleStore | 2 files, tightly coupled domain pair | ✅ DONE |
| T11: StringResources | 4 files, 1 conceptual unit (strings infra) | ✅ DONE |
| T12: LibraryScreen | 1 composable + UI test | ✅ DONE |
| T13: SectionEditorComponent | 1 composable + UI test | ✅ DONE |
| T14: SongEditScreen | 1 composable + UI test | ✅ DONE |
| T15: SetBuilderScreen | 1 composable + UI test | ✅ DONE |
| T16: ImportWizardScreen | 1 composable + UI test | ✅ DONE — 4 UI tests in ImportWizardScreenTest.kt; P1-05 duplicate detection implemented |
| T17: SettingsScreen | 1 composable + manual verification | ✅ DONE (build-only gate; manual checklist pending human verification) |
| T18: ThumbnailCard + Grid | 2 closely coupled composables | ✅ DONE |
| T19: OperatorConsoleApp | 1 composable + inline keyboard handler | ✅ DONE |
| T20: PresentationWindowApp + LyricsSlideView | 2 composables (window + slide renderer) | ✅ DONE |
| T21: Main.kt wiring | 2 files (entry + DI), composition root | ✅ DONE (build gate; manual integration checklist pending human verification) |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
|---|---|---|---|
| T01 | None | Start of graph | ✅ Match |
| T02 | T01 | T01 → T02 | ✅ Match |
| T03 | T01 | T01 → T03 [P with T02] | ✅ Match |
| T04 | T02 | T02 → T04 [P] | ✅ Match |
| T05 | T02 | T02 → T05 [P] | ✅ Match |
| T06 | T01, T02 | T02 → T06 [P] | ✅ Match |
| T07 | T01, T03 | T01+T03 → T07 [P] | ✅ Match |
| T08 | T01 | T01 → T08 [P] | ✅ Match |
| T09 | T01 | T01 → T09 [P] | ✅ Match |
| T10 | T06 | T06 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T04, T11 | T04+T11 → T12 [P] | ✅ Match |
| T13 | T01, T03, T11 | T01+T03+T11 → T13 [P] | ✅ Match |
| T14 | T12, T13 | T12+T13 → T14 | ✅ Match |
| T15 | T05, T11, T12 | T05+T11+T12 → T15 [P with T14] | ✅ Match |
| T16 | T08, T09, T04, T11 | T08+T09+T04+T11 → T16 [P with T14,T15] | ✅ Match |
| T17 | T06, T10, T11 | T06+T11 → T17 [P with T14,T15,T16] | ✅ Match |
| T18 | T07, T11 | T07+T11 → T18 [P] | ✅ Match |
| T19 | T07, T11, T18 | T18 → T19 | ✅ Match |
| T20 | T07, T11 | T07+T11 → T20 [P with T18] | ✅ Match |
| T21 | T10, T14, T15, T16, T17, T19, T20 | ALL → T21 | ✅ Match |

All arrows consistent. No mismatches.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
|---|---|---|---|---|
| T01 | Pure data classes | None (not in matrix) | None | ✅ OK |
| T02 | SQLDelight schema | None (tests in T04-T06) | None (note: DAO tests co-located with repos) | ✅ OK |
| T03 | `SlideSplitter` | Unit | Unit | ✅ OK |
| T04 | `SongRepository` (DAO) | Integration | Integration | ✅ OK |
| T05 | `SetRepository` (DAO) | Integration | Integration | ✅ OK |
| T06 | `SettingsRepository` (DAO) | Integration | Integration | ✅ OK |
| T07 | `PresentationStateStore` | Unit | Unit | ✅ OK |
| T08 | `HolyricsSongParser` | Unit (core parsers) | Unit | ✅ OK |
| T09 | `PlainTextSongParser` | Unit (core parsers) | Unit | ✅ OK |
| T10 | `LocaleStore` | Unit | Unit | ✅ OK |
| T11 | `StringResources` (constants) | None (no logic) | None | ✅ OK |
| T12 | `LibraryScreen` (Compose UI) | UI (Compose Testing) | UI | ✅ OK |
| T13 | `SectionEditorComponent` (Compose UI) | UI | UI | ✅ OK |
| T14 | `SongEditScreen` (Compose UI) | UI | UI | ✅ OK |
| T15 | `SetBuilderScreen` (Compose UI) | UI | UI | ✅ OK |
| T16 | `ImportWizardScreen` (Compose UI) | UI | UI | ✅ OK |
| T17 | `SettingsScreen` (Compose UI, monitor HW) | Manual | None (manual) | ✅ OK — hardware-dependent; manual checklist |
| T18 | `SlideThumbnailCard/Grid` (Compose UI) | UI | UI | ✅ OK |
| T19 | `OperatorConsoleApp` (Compose UI) | UI | UI | ✅ OK |
| T20 | `PresentationWindowApp/LyricsSlideView` | UI | UI | ✅ OK |
| T21 | `Main.kt` wiring | Manual | None (manual integration) | ✅ OK — wiring; manual checklist covers it |

No violations. All ❌ checks passed.

---

## Parallel Execution Map

```
Phase A (Sequential):
  T01

Phase B (Parallel — all start after T01/T02 as noted):
  T01 done →
    ├── T02 [P]
    ├── T03 [P]
    ├── T08 [P]   (parsers start from T01, no DB needed)
    └── T09 [P]

  T02 done →
    ├── T04 [P]
    ├── T05 [P]
    └── T06 [P]

  T01 + T03 done →
    └── T07 [P]   (concurrent with T04-T06)

  T06 done →
    └── T10 ──→ T11

Phase C (UI Screens — parallel after their specific deps):
  T04 + T11 →  T12 [P]
  T01+T03+T11→ T13 [P]   ← can start as soon as T11 is done (T01,T03 already done)
  T12 + T13 →  T14
  T05+T11+T12→ T15 [P]   ← parallel with T14
  T08+T09+T04+T11 → T16 [P]   ← parallel with T14, T15
  T06+T10+T11 → T17 [P]  ← parallel with T14, T15, T16

Phase D (Presentation UI):
  T07 + T11 →
    ├── T18 [P]
    └── T20 [P]
  T18 → T19

Phase E (Wiring):
  T14,T15,T16,T17,T19,T20 → T21
```

**Parallelism constraint note**: All `[P]` tasks use unit or integration tests confirmed parallel-safe in TESTING.md (unit: no shared state; integration: each test creates own in-memory SQLite driver). Compose UI test parallelism is marked "Unknown" — run UI-test tasks sequentially until confirmed safe.

---

## Requirement Coverage

| Req ID | Covered by Task(s) | Status |
|---|---|---|
| P1-01..P1-07 | T08, T16, T04 | Done |
| P1-08..P1-12 | T01, T13, T14 | Done |
| P1-13..P1-15 | T04, T12 | Done |
| P1-16..P1-19 | T05, T15 | Done |
| P1-20..P1-27 | T07, T19 | Done |
| P1-28..P1-31 | T18, T19 | Done |
| P1-32..P1-34 | T20 | Done |
| P1-35..P1-36 | T09, T16 | Done |
| P1-37..P1-38 | T06, T17 | Done |
| P1-39 (i18n) | T10, T11, T21 | Done |

**Coverage**: 39 requirements, 21 tasks, 0 unmapped ✅
