# Code Conventions

**Analyzed:** 2026-05-17
**Source:** `docs/TDD.md` (design doc; conventions established here as project has no source files yet)
**Status:** Established conventions — enforce from first file written

## Naming Conventions

**Files / Classes:**
- PascalCase for all Kotlin files and classes
- Examples: `PresentationStateStore.kt`, `SlideSplitter.kt`, `HolyricsSongParser.kt`, `OperatorConsoleApp.kt`
- Composable functions: PascalCase (Compose convention) — `LyricsSlide`, `VlcVideoSurface`, `IpCameraView`
- Object singletons: PascalCase — `SlideSplitter` (pure algorithm, no state)

**Functions / Methods:**
- camelCase for all functions
- Action methods on stores: verb-noun — `loadSong()`, `advance()`, `toggleBlank()`, `toggleFreeze()`, `startCountdown()`, `pauseCountdown()`, `resetCountdown()`, `showMedia()`, `showWebView()`, `clear()`
- Repository methods: CRUD verbs — `insert()`, `insertAll()`, `update()`, `delete()`, `findById()`, `findAll()`

**Variables:**
- camelCase — `currentSlideIndex`, `presentationState`, `mediaPlayer`
- Backing StateFlow fields: prefix with underscore — `_state`, then expose as `state`

**Constants / Enum values:**
- SCREAMING_SNAKE_CASE for enum values: `VERSE`, `CHORUS`, `BRIDGE`, `PRE_CHORUS`, `OUTRO`, `INTERLUDE`, `TAG`
- Settings keys: `dot.notation` strings — `"presentation.monitor_index"`, `"ui.theme"`, `"media.library_paths"`

**Resource / config files:**
- kebab-case for resource files (Gradle convention)

**Modules:**
- Gradle module names: `kebab-case` — `:core:domain`, `:feature:library`, `:feature:presentation`

## Code Organization

**Import Declaration:**
- Kotlin standard library imports first
- External libraries second
- Internal project imports last
- No wildcard imports except for Compose (`import androidx.compose.material3.*` acceptable)

**File Structure (Kotlin class):**
```kotlin
// 1. Package declaration
package com.trinitylyrics.feature.presentation

// 2. Imports

// 3. Data classes / sealed classes (if file is primarily a model)
// 4. Class/object declaration
// 5. Properties (vals before vars)
// 6. init block (if needed)
// 7. Public functions
// 8. Private functions
```

**Composable file structure:**
```kotlin
// 1. Screen-level composable (entry point)
// 2. Subcomponents (internal composables)
// 3. Preview functions at bottom
```

## Architecture Conventions

**Stores (state holders):**
- Plain Kotlin classes — NOT Android ViewModels
- Register as Koin singletons (application lifetime)
- Expose `StateFlow` for read; expose action methods for writes
- Private `MutableStateFlow`, public `StateFlow` — always use `asStateFlow()`

**No ViewModel pattern:**
- Compose Desktop does not use Android ViewModels
- All state in Koin-injected classes with StateFlow
- UI state that only one composable needs: `remember { mutableStateOf(...) }`

**Dependency Injection:**
- Koin 4.x — `koinInject<T>()` in composables, constructor injection elsewhere
- Modules defined in `:app/di/AppModule.kt`
- All stores registered as `single { ... }`

**Domain types:**
- Defined in `:core:domain` — pure Kotlin, zero framework deps
- Data classes for all entities: `Song`, `SongSection`, `Tag`, `Slide`, `MediaItem`, `SetItem`
- Sealed classes for state: `PresentationState`, `Background`, `PlaybackState`

## Type Safety

**Nullability:**
- Prefer non-null types; express optionality explicitly with `?`
- `id` fields: always non-null `String` (UUID)
- Database foreign keys reflected as nullable in domain types where appropriate

**Enums over stringly-typed values:**
- `SectionType` enum — never raw strings in domain logic
- `ItemType` enum for set items — `SONG`, `MEDIA`, `COUNTDOWN`, `WEBVIEW`, `BLANK`

## Error Handling

**Pattern:** Result-based for operations that can fail; exceptions for programming errors
- Repository operations: wrap in `try/catch`; surface errors via `StateFlow<Result<T>>` or callback
- Parser operations: return nullable or `Result<T>` — never throw to caller
- VLC/JCEF init failures: surface as UI banners (non-fatal), not crashes

## Comments / Documentation

**Style:** Comments explain *why*, not *what*
- KDoc on public APIs of stores and repositories
- Inline comments for non-obvious algorithms (e.g., SlideSplitter word-wrap logic)
- Mark deferred work: `// TODO(V2): PPTX rendering`
- Mark open questions from TDD: `// OQ-1: Holyrics format not yet confirmed`

## Testing Conventions

- Test files mirror source structure in `src/test/kotlin/`
- Kotest style: `StringSpec` or `FunSpec` — choose one and be consistent per module
- Test names: plain English descriptions — `"SlideSplitter splits section at maxLines boundary"`
- No `@Test`-annotated methods style — use Kotest DSL exclusively
- Unit tests: pure logic, no DI, no DB — inject dependencies via constructor
- Integration tests: use in-memory SQLite driver from SQLDelight

## Database Conventions (SQLDelight)

- Table names: `snake_case` plural — `songs`, `song_sections`, `set_items`
- Column names: `snake_case` — `song_id`, `sort_order`, `created_at`
- Primary keys: always `TEXT NOT NULL` UUIDs
- Timestamps: `INTEGER NOT NULL` (epoch milliseconds)
- Soft delete: `deleted_at INTEGER` (null = active, epoch ms = deleted)
- JSON blobs: use sparingly for config — `slide_config TEXT`, `countdown_config TEXT`

## Gradle Conventions

- All version numbers in Gradle Version Catalog (`libs.versions.toml`)
- Module `build.gradle.kts` uses `plugins { }` block only; no imperative config
- Run tests: `./gradlew test`
- Run specific module: `./gradlew :feature:presentation:test`
- Generate SQLDelight: `./gradlew generateSqlDelightInterface`
