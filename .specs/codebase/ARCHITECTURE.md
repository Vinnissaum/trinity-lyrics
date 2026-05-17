# Architecture

**Pattern:** Multi-module modular monolith — single JVM process, feature modules, shared core modules
**Analyzed:** 2026-05-17
**Source:** `docs/TDD.md`

## High-Level Structure

```
Single JVM Process
│
├── Window 1: Operator Console (primary monitor)
│     ├── Compose UI: library, set builder, presentation controls
│     ├── Observes: PresentationStateStore (StateFlow)
│     ├── Mutates: PresentationStateStore via action methods
│     └── Manages: lifecycle of Window 2
│
└── Window 2: Presentation Window (secondary monitor / projector)
      ├── Compose UI: slides, media, countdown, webview
      ├── Observes: PresentationStateStore (same Koin singleton)
      └── Read-only: never writes to state
```

Both windows inject the **same** `PresentationStateStore` singleton via Koin. No IPC, no serialization — pure StateFlow emission.

## Module Boundaries

```
:app                    — entry point, Compose application setup, window lifecycle
:feature:library        — song CRUD, tag management, FTS5 search
:feature:setbuilder     — service set creation and item ordering
:feature:presentation   — presentation state machine, slide engine, keyboard handling
:feature:media          — media library, video playback (VLCJ), image display
:feature:countdown      — countdown timer logic and rendering
:feature:webviewer      — embedded JCEF WebView for IP camera / URL
:feature:import         — Holyrics import wizard, plain-text parser
:core:db                — SQLDelight schema, DAOs, migrations
:core:design            — shared Compose components, theme, typography
:core:domain            — shared domain types (Song, Set, PresentationState, etc.)
:core:settings          — app settings store (key-value over SQLDelight)
```

## Identified Patterns

### Koin Singleton State Store

**Location:** `:feature:presentation` — `PresentationStateStore.kt`
**Purpose:** Single source of truth for what is currently being presented; shared across both Compose windows
**Implementation:** Plain Kotlin class holding `MutableStateFlow<PresentationState>`; registered as Koin singleton; both windows `koinInject<PresentationStateStore>()`
**Example:**
```kotlin
class PresentationStateStore {
  private val _state = MutableStateFlow<PresentationState>(PresentationState.Idle)
  val state: StateFlow<PresentationState> = _state.asStateFlow()
  fun advance() { ... }
  fun toggleBlank() { ... }
}
```

### Sealed Class State Machine

**Location:** `:core:domain` — `PresentationState.kt`
**Purpose:** Model all mutually exclusive presentation modes as a sealed hierarchy; Compose windows `when`-switch on state
**Implementation:** `sealed class PresentationState` with subclasses `Idle`, `Blank`, `Lyrics`, `Media`, `Countdown`, `WebView`

### SQLDelight DAO + Flow

**Location:** `:core:db`
**Purpose:** Reactive data access — DB queries expose `Flow<List<T>>` consumed by feature modules
**Implementation:** SQLDelight `asFlow().mapToList()` pattern; migrations via `.sqm` files

### SwingPanel / VLCJ Interop

**Location:** `:feature:media` / `:feature:presentation`
**Purpose:** Embed VLC video surface inside a Compose layout
**Implementation:** `SwingPanel { }` composable wrapping a VLCJ `EmbeddedMediaPlayerComponent`; must update on EDT

### JCEF WebView (Lazy Init)

**Location:** `:feature:webviewer`
**Purpose:** Full Chromium WebView for IP cameras and arbitrary URLs
**Implementation:** compose-webview-multiplatform wrapping JCEF; lazy-initialized on first use (2–5 s init time acceptable at that point)

### SlideSplitter Algorithm

**Location:** `:feature:presentation` — `SlideSplitter.kt`
**Purpose:** Convert `SongSection.body` text into a list of `Slide` objects respecting max-lines and max-chars-per-line rules
**Implementation:** Pure `object` (no state, no DI); section boundaries always force new slide; fully unit-tested before building the presenter

## Data Flow

### Operator Advances Slide

```
Operator presses Space
  → OperatorConsoleApp keyEvent handler
  → PresentationStateStore.advance()
  → _state.value = copy(currentSlideIndex = current + 1)
  → StateFlow emits new PresentationState.Lyrics
  → PresentationWindowApp recomposes
  → New slide rendered — no IPC, no serialization
```

### Song Added to Set and Presented

```
User picks song in LibraryScreen
  → SetBuilderViewModel adds SetItem(type=song, songId=...)
  → Persisted to SQLDelight set_items table
  → PresentationStateStore.loadSong(song, config)
  → SlideSplitter.split(sections, config) → List<Slide>
  → StateFlow emits PresentationState.Lyrics(song, allSlides, currentIndex=0)
  → PresentationWindowApp renders LyricsSlide
```

### Holyrics Import

```
User selects Holyrics export file
  → HolyricsSongParser.parse(file) → List<Song>
  → Duplicate detection (title + artist match)
  → User confirms in wizard
  → SongRepository.insertAll(songs, source="holyrics")
  → SQLDelight emits updated song list
  → LibraryScreen recomposes
```

## Code Organization

**Approach:** Feature-based (vertical slices) with shared core modules (horizontal)

**Planned Directory Structure:**
```
app/src/main/kotlin/
  Main.kt
  di/AppModule.kt

core/
  domain/src/main/kotlin/   ← shared types, no Android/Compose deps
  db/src/main/sqldelight/   ← .sq schema + .sqm migrations
  design/src/main/kotlin/   ← shared Compose components + theme
  settings/src/main/kotlin/ ← SettingsStore

feature/
  library/src/main/kotlin/
  setbuilder/src/main/kotlin/
  presentation/src/main/kotlin/
    PresentationStateStore.kt
    SlideSplitter.kt
    OperatorConsoleApp.kt
    PresentationWindowApp.kt
    slides/
  media/src/main/kotlin/
  countdown/src/main/kotlin/
  webviewer/src/main/kotlin/
  import/src/main/kotlin/
    HolyricsSongParser.kt
    PlainTextSongParser.kt
```

**Module boundaries:**
- `:core:domain` has zero Compose/DI deps — pure Kotlin data classes and sealed classes
- `:core:db` depends on `:core:domain`; no feature modules depend on each other
- `:app` depends on all modules (composition root)
- Feature modules depend on `:core:domain`, `:core:design`, `:core:db`

## State Management Layers

| Layer | Technology | Scope |
|---|---|---|
| Presentation state | `StateFlow<PresentationState>` Koin singleton | Both windows, in-memory |
| Library data | SQLDelight queries + `asFlow()` | Persisted, observed reactively |
| App settings | SQLDelight key-value table | Persisted |
| UI-only state | Compose `remember` / `mutableStateOf` | Single composable scope |
