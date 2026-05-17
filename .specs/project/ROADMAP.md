# Roadmap

**Current Milestone:** Phase 0 — Skeleton
**Status:** Planning

---

## Phase 0 — Skeleton

**Goal:** Prove the hardest technical problems before any feature work; establish that the full stack compiles, runs, and integrates
**Target:** 2 weeks — deliverable is a two-window skeleton that syncs state, plays video, and loads a WebView

### Features

**Gradle Multi-Module Project Setup** - DONE → `.specs/features/phase-0-scaffold/`
- Root `build.gradle.kts` + `settings.gradle.kts` with all module declarations
- `gradle/libs.versions.toml` version catalog with all dependencies
- All modules compile: `:app`, `:core:domain`, `:core:db`, `:core:ui`, all `:feature:*`
- `./gradlew packageMsi` produces Windows installer via CMP Desktop `nativeDistributions`

**Two-Window Compose Desktop Skeleton** - PLANNED
- `Main.kt` with two `Window {}` composables
- Monitor selection via `GraphicsEnvironment.getScreenDevices()`
- `PresentationStateStore` Koin singleton wired to both windows
- Operator window shows a "Click to advance" button; presentation window shows current slide index
- State syncs between windows instantly via StateFlow

**SQLDelight + Database Connection** - PLANNED
- Full schema in `TrinityLyrics.sq` (songs, sections, tags, sets, set_items, media, settings, FTS5)
- Migration runner connected
- DAOs generated and at least one DAO used (e.g., insert + query a test song)

**VLCJ Video in SwingPanel** - PLANNED
- VLC detection at startup (PATH + common install locations)
- Missing VLC banner shown if absent
- `VlcVideoSurface` composable playing a local video file on SwingPanel

**JCEF WebView Loading a URL** - PLANNED
- `IpCameraView` composable loading a URL
- Lazy initialization confirmed (no startup delay when WebView not used)

**Kotest Runner Green** - PLANNED
- JUnit5 + Kotest configured in at least one module
- One passing `StringSpec` test (e.g., a trivial `SlideSplitter` stub test)
- `./gradlew test` exits 0

---

## Phase 1 — MVP: Lyrics + Holyrics Import

**Goal:** Replace Holyrics for Sunday morning lyrics presentation; used in one real Sunday service
**Target:** 6 weeks (weeks 3–8)
**Prerequisite:** Phase 0 complete; real Holyrics export file obtained from church team

### Features

**Core Domain Types** - PLANNED
- `Song`, `SongSection`, `SectionType`, `Tag` data classes in `:core:domain`
- `PresentationState` sealed class with all subtypes: `Idle`, `Blank`, `Lyrics`, `Media`, `Countdown`, `WebView`
- `Slide`, `SlideConfig`, `MediaItem`, `SetItem` data classes

**SlideSplitter (TDD-first)** - PLANNED
- `SlideSplitter.split(section, config): List<Slide>` pure algorithm
- 100% unit test coverage BEFORE building any presenter UI
- Test cases: empty section, single line, exactly at maxLines, over maxLines, word-wrap at maxCharsPerLine, repeat sections, section boundary forces new slide

**PresentationStateStore — Lyrics State Machine (TDD-first)** - PLANNED
- All action methods: `loadSong()`, `advance()`, `previous()`, `jumpToSlide()`, `toggleBlank()`, `toggleFreeze()`, `clear()`
- Unit tests for all state transitions: advance past last slide (no-op), blank toggle, freeze toggle, load while presenting
- Koin singleton registered in AppModule

**Song Library — CRUD** - PLANNED
- Create / edit song: section editor with drag-to-reorder sections
- Live slide preview panel in editor
- Delete: soft-delete with 30-day recovery (`deleted_at` timestamp)
- Free-form tags (multi-value)
- Favorites flag

**Song Library — Search** - PLANNED
- FTS5 full-text search: title, artist, tags, lyrics body
- < 100 ms response for up to 5,000 songs
- Filter by tag
- Filter favorites

**Set Builder — Songs Only** - PLANNED
- Create / rename / delete sets
- Add songs from library to set
- Drag-to-reorder set items
- Persist to SQLDelight `sets` + `set_items` tables
- Load set into presentation

**Lyrics Presentation — Operator Console** - PLANNED
- Current slide display (large)
- Next slide preview
- Section navigator (scrollable all-slides list)
- Set queue (upcoming items)
- Song progress indicator
- Keyboard shortcuts: Space (advance), Left/Right arrows, B (blank), F (freeze), Esc (clear), 1-9 (jump to section)

**Lyrics Presentation — Projection Window** - PLANNED
- Fullscreen lyrics slide rendering
- Solid-color background (default: black)
- Static-image background from media library
- Font family, size (auto-fit + manual override), color, alignment, text shadow
- Blank screen (B key) — black without losing position
- Freeze (F key) — operator navigates freely while projection is frozen

**Plain-Text Import** - PLANNED
- Parser for `[Verse 1]` / `[Chorus]` format
- Handles common section type names
- Import from `.txt` file via file dialog

**Holyrics Import Wizard** - PLANNED
- 5-step wizard: select file → parse → preview → confirm → done
- Duplicate detection (title + artist match)
- `source = "holyrics"` marker on imported songs
- **Blocked by:** Real Holyrics export file (OQ-1)

**App Settings — Phase 1 Scope** - PLANNED
- Target monitor selection (with preview showing resolution labels)
- Default font family + size + color
- Max lines per slide (default: 4)
- Max chars per line (default: 60)
- Transition type: cut (MVP)
- UI language: Portuguese-BR (default), English

**Language Selection (i18n — PT-BR + EN)** - SPECIFIED → `.specs/features/i18n-language-selection/`
- First-run full-screen language picker (PT-BR pre-selected)
- `StringResources` interface in `:core:ui` with PT-BR and EN implementations
- `LocaleStore` in `:core:domain` — `StateFlow<AppLocale>` persisted in SQLDelight `settings`
- Language changeable at any time from app settings
- No external i18n library — plain Kotlin objects

---

## Phase 2 — V1: Media + Countdown + WebView

**Goal:** Full Holyrics feature parity minus PPTX; used weekly
**Target:** Weeks 9–14

### Features

**Media Library** - PLANNED
- Operator designates watched folder(s); auto-sync on changes
- Thumbnail generation at import (images: 320×180 via ImageIO; videos: frame at 2s via VLCJ)
- Supported: JPEG, PNG, WebP, GIF, MP4, WebM, MKV, AVI
- Files stored by absolute path + relative fallback

**Image Presentation** - PLANNED
- Fullscreen with fit modes: contain, cover, stretch
- Cut transition (MVP for Phase 2)
- Slideshow mode with configurable interval

**Video Presentation (VLCJ)** - PLANNED
- `VlcVideoSurface` composable with VLCJ `EmbeddedMediaPlayer`
- Operator controls: play / pause / seek / volume
- Video backgrounds behind lyrics simultaneously

**Countdown Timer** - PLANNED
- Duration: hours/minutes/seconds
- Display message (e.g. "Service begins in")
- Background: solid color, static image, or looping video
- Rendering: `MM:SS` or `H:MM:SS` format
- Color transitions: yellow at 60 s, red at 10 s (configurable thresholds)
- Optional progress bar
- End action: blank screen or auto-advance to next set item
- Operator controls: Start / Pause / Resume / Reset / Edit duration

**Web/IP Camera Viewer** - PLANNED
- URL entry with presets saved per set-item
- JCEF WebView rendering HTTP camera UIs and MJPEG streams
- Lazy JCEF initialization

**Set Builder — All Item Types** - PLANNED
- Set items: song, media, countdown, webview, blank
- All item types integrated into presentation flow

**Fade Transitions** - PLANNED
- Configurable per-item and globally
- Applied to slide advances and item transitions

**Library Export / Import (ZIP Backup)** - PLANNED
- ZIP archive of `database.db` + `media/` folder
- Import merges by UUID; title+artist dedup fallback
- Soft-delete with "Recently Deleted" view (30-day recovery)

**Additional language polish** - PLANNED
- Review all Phase 2 strings in both PT-BR and EN after real usage feedback

**PPTX Helper Dialog** - PLANNED
- "Export your presentation to images" helper dialog
- Step-by-step PowerPoint/LibreOffice instructions
- Makes PPTX deferral visible and actionable for users

---

## Phase 3 — V2: Polish + Power Features

**Goal:** Long-term polish and power-user features after weekly usage feedback
**Target:** Weeks 15–22

### Features

**PPTX Rendering** - PLANNED
- Apache POI 5.x → slide images
- PPTX set items with slide-by-slide navigation

**Per-Section Background Overrides** - PLANNED
**Presenter Notes (Operator-Only)** - PLANNED
**Keyboard Shortcut Customization** - PLANNED
**Service Report / CCLI Prep Export** - PLANNED
**Print Set List (PDF)** - PLANNED
**Dark / Light UI Theme** - PLANNED
**Multiple Set Templates** - PLANNED
**Auto-Update Mechanism** - PLANNED

---

## Future Considerations

- GraalVM native image for faster startup (if JVM startup proves problematic)
- AppCDS (Application Class Data Sharing) as lighter startup optimization
- VLC bundling in installer (vs. requiring separate VLC install)
- RTSP camera support (would require a separate media server component)
- Multi-language lyric variants per song
- Cloud sync / multi-device (explicitly deferred; would require significant architecture change)
