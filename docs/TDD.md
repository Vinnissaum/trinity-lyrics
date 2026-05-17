# Technical Design Document: Trinity Lyrics

**Version:** 1.0  
**Date:** 2026-05-17  
**Status:** Approved for implementation  
**Stack confirmed by user:** Kotlin + Compose Multiplatform Desktop

---

## Context

Church AV teams currently use Holyrics, a feature-rich but UX-heavy presentation tool. The primary complaint is that new volunteers take too long to learn it. Trinity Lyrics replaces Holyrics with a purpose-built desktop app where a volunteer can run a full Sunday service within 30 minutes of first opening the app. The app is named Trinity Lyrics, scoped to Windows-first, and built AI-first using Claude Code.

The user confirmed:
- **Stack:** Kotlin + Compose Multiplatform Desktop
- **Data migration:** Holyrics import wizard required at MVP
- **PPTX:** Deferred to V2

---

## 1. Executive Summary

Trinity Lyrics is a Windows-first desktop application for church liturgical presentation. It manages a local library of song lyrics, organizes them into service sets, and drives a second monitor (projector/TV) with a fullscreen presentation window. The operator sees a control console on their laptop/PC; the congregation sees the output on the projector.

Core design principle: **operator confidence first.** Every interaction must be discoverable by a non-technical volunteer without training.

---

## 2. Goals and Non-Goals

### Goals
- Simple, confidence-inspiring UX for non-technical church volunteers
- Reliable dual-monitor presentation (control screen + fullscreen output)
- Full offline operation — no internet required during service
- Fast startup: < 3 seconds on 5-year-old Windows PC (i5, 8 GB RAM, HDD)
- Persistent local library (songs, sets, media)
- Keyboard-first operation for all presentation actions
- Holyrics import wizard at MVP
- AI-first development (Claude Code as primary dev tool)

### Non-Goals (MVP)
- Cloud storage or multi-device sync
- Multi-user collaboration
- Live streaming integration
- PPTX/slide rendering (deferred to V2)
- RTSP camera streaming (HTTP cameras only)
- CCLI license reporting

### Non-Goals (All Versions)
- Mobile/web deployment
- Audio mixing
- Video editing

---

## 3. Technology Stack

### Decision: Kotlin + Compose Multiplatform Desktop

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.x |
| UI Framework | Compose Multiplatform (Desktop) | 1.8.x |
| Build System | Gradle + Kotlin DSL | 8.x |
| Database | SQLDelight | 2.x |
| DI | Koin Multiplatform | 4.x |
| State | Kotlin StateFlow + Compose state | coroutines 1.9.x |
| Navigation | Decompose (JetBrains) | 3.x |
| Video Playback | VLCJ (VLC Java bindings) | 4.x |
| WebView | compose-webview-multiplatform (JCEF) | 1.9.x |
| HTTP Client | Ktor Client | 3.x |
| Testing (unit) | JUnit5 + Kotest | 5.x / 6.x |
| Testing (UI) | Compose UI Testing framework | same as CMP |
| Icons | Material Icons Extended | same as CMP |
| PPTX (V2) | Apache POI | 5.x |

**Rationale for VLCJ over JavaFX MediaView:** VLCJ supports H.264, H.265, and virtually every codec without additional Windows Media Feature Pack dependencies. VLC is free and widely installed; the app should detect VLC at startup and prompt for install if absent.

**Rationale for compose-webview-multiplatform:** It wraps JCEF (Java Chromium Embedded Framework), providing a full Chromium WebView embedded in a Compose layout. This handles IP camera HTTP UIs, MJPEG streams, and any URL-based content with full browser fidelity.

**Dual-monitor architecture advantage:** Both windows run in the same JVM process. State is shared via Kotlin `StateFlow` singletons — no IPC, no serialization, no event bus. This is simpler and more performant than a two-process model.

---

## 4. Architecture Overview

### 4.1 Two-Window Architecture

```
Single JVM Process
│
├── Window 1: Operator Console (primary monitor)
│     ├── Compose UI: library, set builder, presentation controls
│     ├── Observes: PresentationStateStore (StateFlow)
│     ├── Mutates: PresentationStateStore via actions
│     └── Manages: lifecycle of Window 2
│
└── Window 2: Presentation Window (secondary monitor / projector)
      ├── Compose UI: slides, media, countdown, webview
      ├── Observes: PresentationStateStore (same instance, no copy)
      └── Read-only: never writes to state
```

Both windows observe the **same** `PresentationStateStore` singleton via Koin. When the operator advances a slide, the StateFlow emits the new state and the presentation window recomposes instantly — no serialization or IPC latency.

### 4.2 Module Boundaries

```
:app                    — entry point, Compose application setup, window lifecycle
:feature:library        — song CRUD, tag management, search
:feature:setbuilder     — set creation and ordering
:feature:presentation   — presentation state machine, slide engine
:feature:media          — media library, video playback, image display
:feature:countdown      — countdown timer logic and rendering
:feature:webviewer      — embedded JCEF WebView for IP camera / URL
:feature:import         — Holyrics import wizard
:core:db                — SQLDelight schema, DAOs, migrations
:core:design            — shared Compose components, theme, typography
:core:domain            — shared domain types (Song, Set, PresentationState, etc.)
:core:settings          — app settings store (key-value over SQLDelight)
```

---

## 5. Feature Specifications

### 5.1 Lyrics Management

**Song Data Structure:**
A song has: title, artist, key, language, tags, and an ordered list of sections. Each section has a type (verse / chorus / bridge / pre-chorus / outro / interlude / tag), a label ("Verse 1", "Chorus"), and body text.

**CRUD:**
- Create/edit: section editor with drag-to-reorder, live slide preview panel
- Delete: soft-delete, 30-day recovery
- Import: plain text with `[Verse 1]` / `[Chorus]` markers (MVP), Holyrics format (MVP)

**Organization:**
- Free-form tags (multi-value, e.g. `worship`, `portuguese`, `advent`)
- Full-text search across title, artist, tags, and lyrics body (SQLite FTS5)
- Favorites flag for quick access

**Set Builder:**
A "Set" is an ordered list of items for a specific service: songs, media, countdowns, web views, or blank slides. Sets are saved and reusable. Drag-to-reorder. Quick-add from song library.

### 5.2 Lyrics Presentation

**Slide Generation — core algorithm (`SlideSplitter.kt`):**
Converts a section's body text into a list of slides based on rules:
- Max lines per slide (default: 4, configurable)
- Max characters per line (default: 60, configurable)
- Section boundaries always force a new slide

**Presentation Flow:**
- Manual advance only (spacebar / arrow keys)
- Jump to any section from navigator panel
- `B` key: blank (black) screen without losing position
- `F` key: freeze presentation window while operator navigates freely

**Operator Console During Presentation:**
- Current slide (large)
- Next slide preview
- Section navigator (all slides as scrollable list)
- Set queue (upcoming items)
- Song progress indicator

**Typography:**
- Font family, size (auto-fit with manual override), color configurable globally and per-song
- Text alignment: center (default) or left
- Text shadow/outline for legibility over backgrounds

**Background Options:**
- Solid color (black default)
- Static image from media library
- Looping video (via VLCJ surface embedded in Compose)

### 5.3 Media Presentation

**Supported Formats:**
- Images: JPEG, PNG, WebP, GIF (static and animated)
- Videos: MP4, WebM, MKV, AVI (via VLCJ — inherits VLC codec support)

**Media Library:**
- Operator designates watched folder(s); changes auto-sync
- Thumbnails generated at import time, cached in `%APPDATA%\TrinityLyrics\thumbnails\`
- Files stored by absolute path (with relative-to-media-root fallback)

**Video Presentation:**
- VLCJ `EmbeddedMediaPlayer` renders into a `SwingPanel` wrapped in Compose
- Operator controls: play/pause/seek/volume
- Can run as background layer behind lyrics simultaneously

**Image Presentation:**
- Fullscreen with fit mode: contain, cover, stretch
- Transition: fade or cut (configurable per-item and globally)
- Slideshow mode with configurable interval

**PPTX:** Deferred to V2. In V1: display a "Export your presentation to images" helper dialog with step-by-step instructions.

### 5.4 Countdown Timer

**Configuration:**
- Duration: hours/minutes/seconds
- Display message (e.g. "Service begins in")
- Background: solid color, static image, or looping video
- End action: blank screen or auto-advance to next set item

**Rendering (in presentation window):**
- Format: `MM:SS` or `H:MM:SS`
- Color transition: yellow at 60s, red at 10s (configurable thresholds)
- Optional progress bar

**Operator Controls:** Start / Pause / Resume / Reset / Edit duration

### 5.5 Web / IP Camera Viewer

**URL Entry:**
- Operator enters any URL; presentation window loads it in a JCEF WebView
- URL presets saved per set-item (e.g. "Camera 1" → `http://192.168.1.10/cgi-bin/video`)

**IP Camera Compatibility:**
- HTTP MJPEG streams: rendered via `<img src="...">` in the WebView
- HTTP/HTTPS camera web UIs (DaHua, Hikvision): full JCEF Chromium rendering
- RTSP: not supported in V1; documented limitation — advise use of camera's HTTP UI

**Security:**
- JCEF WebView is sandboxed from the host Compose application
- Camera credentials handled by URL (e.g. `http://admin:pass@192.168.1.10`)

### 5.6 Holyrics Import (MVP)

**Pre-condition:** Obtain a real Holyrics export (`.db` or `.lyr` file) before coding this module. Do not implement the parser blind.

**Import Wizard Flow:**
1. User selects the Holyrics database file or an export folder
2. App parses songs, sections, and tags
3. Preview: "Found 142 songs. 3 duplicates detected."
4. User confirms
5. Songs created in TrinityLyrics DB with `source = "holyrics"` marker

**Field Mapping:**
- Holyrics section types → Trinity `SectionType` enum
- Holyrics categories → Trinity tags
- Holyrics song backgrounds → linked by file path if media exists

---

## 6. Data Model

### 6.1 SQLDelight Schema

```sql
CREATE TABLE songs (
  id             TEXT NOT NULL PRIMARY KEY,
  title          TEXT NOT NULL,
  artist         TEXT,
  ccli_number    TEXT,
  key_signature  TEXT,
  language       TEXT NOT NULL DEFAULT 'pt',
  notes          TEXT,
  background_id  TEXT REFERENCES media(id),
  slide_config   TEXT,  -- JSON: {maxLines, fontSize, fontFamily, textAlign, textColor}
  source         TEXT,  -- 'holyrics' for imported songs
  created_at     INTEGER NOT NULL,
  updated_at     INTEGER NOT NULL,
  deleted_at     INTEGER        -- soft delete (epoch ms)
);

CREATE TABLE song_sections (
  id           TEXT NOT NULL PRIMARY KEY,
  song_id      TEXT NOT NULL REFERENCES songs(id),
  label        TEXT NOT NULL,   -- "Verse 1", "Chorus"
  type         TEXT NOT NULL,   -- verse|chorus|bridge|pre_chorus|outro|interlude|tag
  body         TEXT NOT NULL,
  sort_order   INTEGER NOT NULL,
  repeat_count INTEGER NOT NULL DEFAULT 1
);

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

CREATE TABLE media (
  id             TEXT NOT NULL PRIMARY KEY,
  file_path      TEXT NOT NULL,
  file_name      TEXT NOT NULL,
  media_type     TEXT NOT NULL,  -- image|video|url
  url            TEXT,
  mime_type      TEXT,
  duration_ms    INTEGER,
  width          INTEGER,
  height         INTEGER,
  thumbnail_path TEXT,
  created_at     INTEGER NOT NULL
);

CREATE TABLE sets (
  id           TEXT NOT NULL PRIMARY KEY,
  name         TEXT NOT NULL,
  service_date TEXT,  -- ISO date string
  notes        TEXT,
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL
);

CREATE TABLE set_items (
  id                TEXT NOT NULL PRIMARY KEY,
  set_id            TEXT NOT NULL REFERENCES sets(id),
  item_type         TEXT NOT NULL,  -- song|media|countdown|webview|blank
  song_id           TEXT REFERENCES songs(id),
  media_id          TEXT REFERENCES media(id),
  countdown_config  TEXT,  -- JSON blob
  web_url           TEXT,
  sort_order        INTEGER NOT NULL,
  notes             TEXT
);

CREATE TABLE settings (
  key   TEXT NOT NULL PRIMARY KEY,
  value TEXT NOT NULL
);

-- Full-text search
CREATE VIRTUAL TABLE songs_fts USING fts5(
  title, artist, body,
  content='songs',
  content_rowid='rowid'
);
```

### 6.2 Key Settings (settings table)

```
presentation.monitor_index         -- integer
presentation.default_background    -- "black" | media_id
presentation.font_family           -- string
presentation.font_size_base        -- integer
presentation.slide_max_lines       -- integer
presentation.transition_type       -- "cut" | "fade"
presentation.transition_ms         -- integer
media.library_paths                -- JSON array
ui.theme                           -- "dark" | "light"
ui.language                        -- "pt" | "en"
```

---

## 7. Module and Component Design

### 7.1 Core Domain Types (`:core:domain`)

```kotlin
data class Song(
  val id: String,
  val title: String,
  val artist: String?,
  val sections: List<SongSection>,
  val tags: List<Tag>,
  val slideConfig: SlideConfig?,
  val backgroundId: String?
)

data class SongSection(
  val id: String,
  val label: String,
  val type: SectionType,
  val body: String,
  val sortOrder: Int,
  val repeatCount: Int
)

enum class SectionType { VERSE, CHORUS, BRIDGE, PRE_CHORUS, OUTRO, INTERLUDE, TAG }

sealed class PresentationState {
  object Idle : PresentationState()
  object Blank : PresentationState()

  data class Lyrics(
    val song: Song,
    val allSlides: List<Slide>,
    val currentSlideIndex: Int,
    val frozen: Boolean = false,
    val background: Background = Background.Black
  ) : PresentationState()

  data class Media(
    val media: MediaItem,
    val playbackState: PlaybackState = PlaybackState.Playing
  ) : PresentationState()

  data class Countdown(
    val totalMs: Long,
    val remainingMs: Long,
    val running: Boolean,
    val message: String,
    val background: Background = Background.Black,
    val endAction: CountdownEndAction = CountdownEndAction.Blank
  ) : PresentationState()

  data class WebView(val url: String) : PresentationState()
}

data class Slide(
  val sectionId: String,
  val sectionLabel: String,
  val sectionType: SectionType,
  val lines: List<String>,
  val slideIndexInSection: Int,
  val totalSlidesInSection: Int
)
```

### 7.2 PresentationStateStore (`:feature:presentation`)

```kotlin
class PresentationStateStore {
  private val _state = MutableStateFlow<PresentationState>(PresentationState.Idle)
  val state: StateFlow<PresentationState> = _state.asStateFlow()

  fun loadSong(song: Song, config: SlideConfig)
  fun advance()
  fun previous()
  fun jumpToSlide(index: Int)
  fun toggleBlank()
  fun toggleFreeze()
  fun startCountdown(config: CountdownConfig)
  fun pauseCountdown()
  fun resetCountdown()
  fun showMedia(media: MediaItem)
  fun showWebView(url: String)
  fun clear()
}
```

Both windows inject the **same Koin singleton** of this store. No serialization, no IPC.

### 7.3 Dual-Window Setup (`:app`)

```kotlin
fun main() = application {
  val store = koinInject<PresentationStateStore>()
  val settings = koinInject<SettingsStore>()

  Window(
    title = "Trinity Lyrics",
    state = rememberWindowState(width = 1200.dp, height = 800.dp),
    onCloseRequest = ::exitApplication
  ) {
    OperatorConsoleApp(store, settings)
  }

  val presentationOpen by store.presentationWindowOpen.collectAsState()
  if (presentationOpen) {
    val monitors = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    val targetMonitor = monitors[settings.monitorIndex]
    val bounds = targetMonitor.defaultConfiguration.bounds

    Window(
      undecorated = true,
      resizable = false,
      alwaysOnTop = true,
      state = WindowState(
        position = WindowPosition(bounds.x.dp, bounds.y.dp),
        width = bounds.width.dp,
        height = bounds.height.dp
      ),
      onCloseRequest = { store.closePresentationWindow() }
    ) {
      PresentationWindowApp(store)
    }
  }
}
```

### 7.4 SlideSplitter Algorithm (`:feature:presentation`)

```kotlin
object SlideSplitter {
  fun split(section: SongSection, config: SlideConfig): List<Slide> {
    // 1. Split body by \n into raw lines
    // 2. Word-wrap each raw line at config.maxCharsPerLine
    // 3. Accumulate display lines into slides; flush at config.maxLinesPerSlide
    // 4. Return Slide list with sectionId, label, type, lines[], position metadata
  }
}
```

Full unit test coverage required on this function before building the presenter.

### 7.5 Video Integration (VLCJ)

VLCJ requires VLC to be installed. Detection strategy:
1. At startup, search PATH and common install locations (`C:\Program Files\VideoLAN\VLC`)
2. If absent: show one-time banner "VLC required for video. [Install VLC]"
3. If present: initialize VLCJ `NativeLibraryLocator` with detected path

Video renders via `SwingPanel {}` (Compose Desktop supports Swing interop):

```kotlin
@Composable
fun VlcVideoSurface(mediaPlayer: EmbeddedMediaPlayer) {
  val videoSurface = remember { SwingVideoSurface(...) }
  SwingPanel(
    modifier = Modifier.fillMaxSize(),
    factory = { videoSurface.component() }
  )
}
```

### 7.6 WebView Integration (JCEF)

```kotlin
@Composable
fun IpCameraView(url: String) {
  WebView(
    state = rememberWebViewState(url),
    modifier = Modifier.fillMaxSize(),
    captureBackPresses = false
  )
}
```

JCEF bundles Chromium — no external browser installation required. Binary size ~80 MB, included in app bundle. Lazy-initialize to avoid impacting startup time.

---

## 8. State Management

| Layer | Technology | Scope |
|---|---|---|
| Presentation state | `StateFlow<PresentationState>` Koin singleton | Both windows, in-memory |
| Library data | SQLDelight queries + `asFlow()` | Persisted, observed reactively |
| App settings | SQLDelight key-value table | Persisted |
| UI-only state | Compose `remember` / `mutableStateOf` | Single composable scope |

Compose Desktop does not use Android ViewModels. Use plain Koin-injected classes with StateFlow. Lifecycle scope = application lifetime.

---

## 9. Media Pipeline

### 9.1 Import Flow

```
User selects file via file dialog
  → MediaRepository.importMedia(path)
  → Validate extension and MIME type
  → Copy to %APPDATA%\TrinityLyrics\media\ (configurable)
  → Generate thumbnail:
      - Images: resize to 320×180 via ImageIO
      - Videos: extract frame at 2s via VLCJ
  → Save Media record to SQLDelight
  → Emit new media via Flow to UI
```

### 9.2 Asset Access

Compose `Image` and VLCJ both accept `File` objects directly — no URL protocol conversion needed.

### 9.3 PPTX (V2 Only)

Deferred. V1 shows a helper dialog: "Export your presentation to images" with step-by-step PowerPoint/LibreOffice instructions.

---

## 10. Persistence Strategy

**Paths:**
```
%APPDATA%\TrinityLyrics\database.db    (SQLDelight SQLite)
%APPDATA%\TrinityLyrics\media\         (imported media files)
%APPDATA%\TrinityLyrics\thumbnails\    (generated thumbnails)
```

**Migrations:** SQLDelight 2.x migration files (`1.sqm`, `2.sqm`, ...) run automatically at startup.

**Backup/Export:** ZIP archive of `database.db` + `media/`. Import merges by UUID with title+artist dedup fallback. Soft-delete with 30-day "Recently Deleted" view.

---

## 11. Testing Strategy

### Unit Tests (JUnit5 + Kotest)
- `SlideSplitter` — empty sections, single-word lines, exactly at maxLines, over limit, repeat sections
- `PresentationStateStore` — all state transitions (advance past last slide, blank toggle, freeze, countdown tick)
- `HolyricsSongParser` — all section type mappings, duplicate detection, malformed input
- `CountdownTimer` — tick accuracy, end-action trigger, pause/resume

### Integration Tests
- SQLDelight DAOs — CRUD + FTS5 search against in-memory SQLite
- `MediaRepository` — import, thumbnail generation, deletion
- `SettingsStore` — get/set round-trip

### UI Tests (Compose Testing Framework)
- Song library: search filters correctly
- Set builder: drag-to-reorder changes `sort_order`
- Presentation control: advance button updates slide index
- Countdown display: correct format and color at thresholds

### Manual Test Checklist (before each release)
- Dual-monitor on physical hardware
- IP camera stream with a real DaHua or Hikvision device
- Fullscreen stability after monitor sleep/wake
- VLC detection when VLC is absent
- Holyrics import with a real export file
- 2-hour service simulation: 20 songs, 10 media items — no memory growth

---

## 12. Non-Functional Requirements

| Metric | Target |
|---|---|
| App cold startup | < 3 s on 5-year-old Windows PC (i5, 8 GB RAM, HDD) |
| Slide advance latency | < 16 ms (1 frame at 60 fps — pure StateFlow emit) |
| Song search response | < 100 ms for up to 5,000 songs (FTS5) |
| Memory (idle) | < 300 MB (JVM overhead included) |
| Memory (video background active) | < 600 MB |
| Installer size | < 120 MB (JCEF ~80 MB) |
| Offline operation | 100% — zero network for core features |
| Windows support | Windows 10 (1803+) and Windows 11 |

---

## 13. Open Questions and Risks

### Open Questions

**OQ-1: Holyrics export format**
Need a real export file before coding the parser. Action: obtain from church team before starting `:feature:import`.

**OQ-2: VLC bundle strategy**
Require VLC (simpler) vs bundle VLC installer (better UX). Recommended: require VLC, detect at startup, provide install link. Revisit bundling if adoption is low.

**OQ-3: JCEF initialization time**
JCEF takes 2-5 s to initialize. Mitigation: lazy-initialize only when a webview set item is first loaded.

**OQ-4: Compose rendering on older Intel GPU**
Skia renderer may fail on old integrated graphics. Mitigation: test on target hardware in Phase 0; fallback via `-Dskiko.renderApi=SOFTWARE`.

### Risks

**RISK-1: Dual-monitor window positioning (High probability, Medium impact)**
`GraphicsEnvironment.getScreenDevices()` ordering is OS-dependent and can change on reconnect. Mitigation: show monitor preview with resolution labels; allow manual drag as fallback; persist last-used index.

**RISK-2: VLCJ version mismatch (Medium probability, High impact)**
VLCJ 4.x requires VLC 3.0+. Old VLC causes silent init failure. Mitigation: check VLC version at startup and warn if < 3.0.

**RISK-3: JCEF camera compatibility (Medium probability, Medium impact)**
ActiveX-only camera UIs won't render in Chromium. Mitigation: document known-working cameras; provide MJPEG URL field as fallback.

**RISK-4: JVM startup time on HDD (Medium probability, Medium impact)**
JVM cold start on spinning disk may exceed 3 s. Mitigation: profile early; investigate AppCDS or GraalVM native image if needed.

---

## 14. Phased Roadmap

### Phase 0: Skeleton (Week 1-2)

Goal: Prove the hardest problems before any feature work.

- Gradle multi-module project with Compose Desktop + Koin
- Two-window setup syncing a StateFlow value between operator and presentation windows
- SQLDelight connected with empty schema and migration running
- VLCJ playing a video in a `SwingPanel`
- JCEF loading a URL in a Compose layout
- Kotest runner green on a hello-world test

**Deliverable:** Two-window skeleton that syncs state, plays video, and loads a WebView.

### Phase 1: MVP — Lyrics + Holyrics Import (Week 3-8)

Goal: Replace Holyrics for Sunday morning lyrics presentation.

- Full song CRUD with section editor and drag-to-reorder
- `SlideSplitter` fully unit-tested
- Set builder (songs only)
- Lyrics presentation: load set → advance/previous/blank/freeze
- Keyboard shortcuts: Space, arrows, B, F, Esc, 1-9
- Solid-color and static-image backgrounds
- Plain-text import (`[Verse 1]` format)
- Holyrics import wizard
- Settings: font, slide layout, target monitor
- Portuguese UI strings

**Deliverable:** App used in one real Sunday service. Collect feedback.

### Phase 2: V1 — Media + Countdown + WebView (Week 9-14)

Goal: Full Holyrics feature parity minus PPTX.

- Media library (images and videos via VLCJ)
- Image and video presentation with transitions
- Video backgrounds for lyrics
- Countdown timer with video background support
- Web/IP camera viewer (JCEF WebView)
- Set items: all types (song, media, countdown, webview, blank)
- Library export/import (ZIP backup)
- Fade transitions
- Portuguese UI + English option

**Deliverable:** Full V1 used weekly. 4-week feedback period.

### Phase 3: V2 — Polish + Power Features (Week 15-22)

- PPTX rendering (Apache POI → slide images)
- Per-section background overrides
- Presenter notes (operator-only per-slide text)
- Keyboard shortcut customization
- Service report / CCLI prep export
- Print set list (PDF)
- Dark/light UI theme
- Multiple set templates
- Auto-update mechanism

---

## 15. Critical Files for Implementation

Files that gate all downstream work — build these first:

| File | Module | Why Critical |
|---|---|---|
| `app/src/main/kotlin/Main.kt` | `:app` | Koin setup, two-window lifecycle, monitor selection |
| `core/db/src/sqldelight/TrinityLyrics.sq` | `:core:db` | Full schema; all modules derive types from this |
| `core/domain/PresentationState.kt` | `:core:domain` | Sealed class shared by both windows |
| `feature/presentation/PresentationStateStore.kt` | `:feature:presentation` | Central state coordinator |
| `feature/presentation/SlideSplitter.kt` | `:feature:presentation` | Core domain algorithm; unit-test before building presenter |
| `feature/import/HolyricsSongParser.kt` | `:feature:import` | Requires real Holyrics export — do not code blind |

---

## 16. Verification (MVP Sign-off Checklist)

1. **Import:** Holyrics export with 10+ songs → all appear with correct sections
2. **Set building:** Set with 3 songs + 1 countdown → correct order
3. **Dual monitor:** Presentation window opens fullscreen on second monitor
4. **Lyrics:** Advance through song 1 → jump to song 2 via navigator → blank → un-blank → reach end of set
5. **Countdown:** 2-minute countdown → yellow at 60 s, red at 10 s → end action triggers
6. **Keyboard:** Space, B, F, Esc, arrows — all work without mouse
7. **State sync:** No perceptible latency between operator action and presentation update

---

## Appendix A: Project Directory Structure

```
c:\git\trinity-lyrics\
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  CLAUDE.md
  TDD.md                              ← this file

  app\
    src\main\kotlin\
      Main.kt
      di\AppModule.kt
    build.gradle.kts

  core\
    domain\src\main\kotlin\
      Song.kt
      SongSection.kt
      Tag.kt
      PresentationState.kt
      Slide.kt
      MediaItem.kt
      SetItem.kt
    db\src\main\sqldelight\
      TrinityLyrics.sq
      migrations\1.sqm
    design\src\main\kotlin\
    settings\src\main\kotlin\

  feature\
    library\src\main\kotlin\
    setbuilder\src\main\kotlin\
    presentation\src\main\kotlin\
      PresentationStateStore.kt
      SlideSplitter.kt
      OperatorConsoleApp.kt
      PresentationWindowApp.kt
      slides\
        LyricsSlide.kt
        MediaSlide.kt
        CountdownSlide.kt
        WebViewSlide.kt
        BlankSlide.kt
    media\src\main\kotlin\
    countdown\src\main\kotlin\
    webviewer\src\main\kotlin\
    import\src\main\kotlin\
      HolyricsSongParser.kt
      PlainTextSongParser.kt
```

## Appendix B: CLAUDE.md Starter Content

Create `CLAUDE.md` at the repo root with:
- Stack: Kotlin 2.x, Compose Multiplatform Desktop 1.8.x, SQLDelight 2.x, Koin 4.x, VLCJ, JCEF
- Module dependency graph
- Naming conventions: `PascalCase` classes, `camelCase` functions, `kebab-case` resource files
- How to add a new feature store: plain class with StateFlow, registered as Koin singleton
- How to add a new `SectionType`: enum value + parser mapping + UI label
- SQLDelight: run `./gradlew generateSqlDelightInterface` after schema changes
- Test commands: `./gradlew test`, `./gradlew :feature:presentation:test`
- Common gotchas: JCEF lazy init, VLCJ `SwingPanel` must update on EDT, `GraphicsEnvironment` monitor index ordering is OS-dependent
