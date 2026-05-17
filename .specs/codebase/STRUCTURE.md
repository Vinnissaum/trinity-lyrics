# Project Structure

**Root:** `C:\git\trinity-lyrics`
**Analyzed:** 2026-05-17
**Status:** Pre-scaffold — structure is planned (from `docs/TDD.md`); only `docs/TDD.md` exists currently

## Current State (actual files on disk)

```
trinity-lyrics/
├── docs/
│   └── TDD.md              ← Comprehensive technical design document
├── .claude/                ← Claude Code skills (tlc-spec-driven, mermaid-studio, etc.)
├── .agents/                ← Skill lock registry
└── .specs/                 ← Planning artifacts (this directory — being created now)
```

## Planned Directory Tree (from TDD.md — to be created in Phase 0)

```
trinity-lyrics/
├── build.gradle.kts                    ← Root build file
├── settings.gradle.kts                 ← Module declarations
├── gradle.properties                   ← JVM args, Kotlin opts
├── gradle/libs.versions.toml           ← Version catalog
├── CLAUDE.md                           ← Dev guide (to be created)
├── docs/
│   └── TDD.md
│
├── app/
│   └── src/main/kotlin/
│       ├── Main.kt                     ← Application entry point, two-window setup
│       └── di/AppModule.kt             ← Koin module definitions
│
├── core/
│   ├── domain/src/main/kotlin/
│   │   ├── Song.kt
│   │   ├── SongSection.kt
│   │   ├── Tag.kt
│   │   ├── PresentationState.kt        ← CRITICAL sealed class
│   │   ├── Slide.kt
│   │   ├── MediaItem.kt
│   │   └── SetItem.kt
│   ├── db/src/
│   │   ├── main/sqldelight/
│   │   │   ├── TrinityLyrics.sq        ← Full SQLDelight schema
│   │   │   └── migrations/1.sqm
│   │   └── main/kotlin/               ← DB driver setup
│   ├── design/src/main/kotlin/        ← Shared Compose components + theme
│   └── settings/src/main/kotlin/      ← SettingsStore
│
└── feature/
    ├── library/src/main/kotlin/        ← Song CRUD, search, tags
    ├── setbuilder/src/main/kotlin/     ← Service set creation, ordering
    ├── presentation/src/main/kotlin/
    │   ├── PresentationStateStore.kt   ← CRITICAL state coordinator
    │   ├── SlideSplitter.kt            ← CRITICAL slide algorithm
    │   ├── OperatorConsoleApp.kt
    │   ├── PresentationWindowApp.kt
    │   └── slides/
    │       ├── LyricsSlide.kt
    │       ├── MediaSlide.kt
    │       ├── CountdownSlide.kt
    │       ├── WebViewSlide.kt
    │       └── BlankSlide.kt
    ├── media/src/main/kotlin/          ← Media library, VLCJ, thumbnails
    ├── countdown/src/main/kotlin/      ← Countdown timer logic + rendering
    ├── webviewer/src/main/kotlin/      ← JCEF WebView composable
    └── import/src/main/kotlin/
        ├── HolyricsSongParser.kt       ← Requires real Holyrics export first
        └── PlainTextSongParser.kt
```

## Module Organization

### :app
**Purpose:** Application entry point; wires all modules via Koin; manages two Compose windows and monitor selection
**Location:** `app/src/main/kotlin/`
**Key files:** `Main.kt`, `di/AppModule.kt`

### :core:domain
**Purpose:** Shared domain types with zero framework dependencies — used by all modules
**Location:** `core/domain/src/main/kotlin/`
**Key files:** `PresentationState.kt` (sealed class), `Song.kt`, `Slide.kt`, `SetItem.kt`

### :core:db
**Purpose:** SQLDelight schema, DAOs, migration files, database driver initialization
**Location:** `core/db/src/main/sqldelight/`
**Key files:** `TrinityLyrics.sq` (full schema), `migrations/1.sqm`

### :core:design
**Purpose:** Shared Compose components, color scheme, typography, spacing constants
**Location:** `core/design/src/main/kotlin/`

### :core:settings
**Purpose:** `SettingsStore` — typed key-value wrapper over the `settings` SQLDelight table
**Location:** `core/settings/src/main/kotlin/`

### :feature:presentation
**Purpose:** Central feature — state machine, slide splitter, both Compose window roots
**Location:** `feature/presentation/src/main/kotlin/`
**Key files:** `PresentationStateStore.kt`, `SlideSplitter.kt`

### :feature:library
**Purpose:** Song CRUD UI, tag management, FTS5 search screen
**Location:** `feature/library/src/main/kotlin/`

### :feature:import
**Purpose:** Holyrics import wizard + plain-text importer
**Location:** `feature/import/src/main/kotlin/`
**Critical constraint:** Do not code `HolyricsSongParser` without a real Holyrics export file

## Where Things Live

**Presentation state:**
- Domain types: `core/domain/src/main/kotlin/PresentationState.kt`
- State store: `feature/presentation/src/main/kotlin/PresentationStateStore.kt`
- Operator UI: `feature/presentation/src/main/kotlin/OperatorConsoleApp.kt`
- Projection UI: `feature/presentation/src/main/kotlin/PresentationWindowApp.kt`

**Database:**
- Schema: `core/db/src/main/sqldelight/TrinityLyrics.sq`
- Migrations: `core/db/src/main/sqldelight/migrations/`
- DAOs (generated): `core/db/build/generated/`

**DI wiring:**
- All Koin modules: `app/src/main/kotlin/di/AppModule.kt`

**Persisted data at runtime:**
- Database: `%APPDATA%\TrinityLyrics\database.db`
- Media files: `%APPDATA%\TrinityLyrics\media\`
- Thumbnails: `%APPDATA%\TrinityLyrics\thumbnails\`

## Special Directories

**`.specs/`:**
**Purpose:** Planning and specification artifacts for this project
**Examples:** `project/PROJECT.md`, `project/ROADMAP.md`, `codebase/STACK.md`, `features/<name>/spec.md`

**`docs/`:**
**Purpose:** Design and reference documents
**Examples:** `TDD.md` (Technical Design Document — authoritative reference)

**`core/db/src/main/sqldelight/migrations/`:**
**Purpose:** SQLDelight migration scripts run automatically at app startup
**Examples:** `1.sqm`, `2.sqm` (incremental schema changes)
