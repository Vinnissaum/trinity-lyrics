# Trinity Lyrics

Desktop application for church liturgical presentation. Replaces Holyrics with a simpler, volunteer-friendly experience — purpose-built so anyone can run a full Sunday service within 30 minutes of first opening the app.

**Platform:** Windows-first · **Stack:** Kotlin + Compose Multiplatform Desktop · **Build:** Gradle 8.x + Kotlin DSL

---

## Module Architecture

```
trinity-lyrics/
├── app/                    ← entry point, DI wiring, window setup
├── core/
│   ├── domain/             ← business logic, state, models (no UI, no DB)
│   ├── db/                 ← SQLDelight database, DAOs, migrations
│   └── ui/                 ← shared Compose components and theme
└── feature/
    ├── lyrics/             ← song library: browse, edit, search
    ├── presentation/       ← dual-window presentation engine (operator + projector)
    ├── media/              ← video/image playback via VLCJ
    └── import/             ← Holyrics import wizard
```

### Why is `feature/` a Gradle module (not just a package)?

Each `feature:*` module compiles independently. That means:

- **Isolation by contract** — a feature can only access what it declares in `dependencies {}`. It cannot accidentally reach into another feature's internals; it must go through `core:domain`.
- **Parallel compilation** — Gradle compiles unrelated features at the same time, keeping incremental builds fast as the codebase grows.
- **Clear ownership** — each feature is a self-contained vertical slice (its own UI, its own use-cases wired to domain interfaces). This maps directly to user-visible product areas, making it easy to find and change code without cross-feature side effects.
- **Testability** — each feature module has its own test source set and can be tested in isolation without spinning up the whole app.

The rule: `feature:*` modules depend on `core:*`, never on each other. `app` depends on everything and owns the Koin DI graph.

### Module dependency graph

```
app
├── core:domain
└── feature:*
      └── core:domain
            └── (pure Kotlin — no framework deps)

core:db       → sqldelight, coroutines
core:ui       → compose, material
feature:*     → core:domain, core:ui  (+ core:db where needed)
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17 (Temurin/Adoptium) | path configured in `gradle.properties` |
| VLC | 3.x or 4.x | required at runtime for media playback |
| Gradle | wrapper (no install needed) | use `./gradlew.bat` |

> `gradle.properties` pins the JDK path. If yours differs, update `org.gradle.java.home`.

---

## Development Mode

Run the app directly from source with hot-reload-friendly iteration:

```powershell
.\gradlew.bat :app:run
```

This launches **two windows simultaneously** (by design):

| Window | Title | Purpose |
|--------|-------|---------|
| Operator console | `Trinity Lyrics — Operator` | volunteer control panel (your monitor) |
| Projection output | `Trinity Lyrics — Projection` | fullscreen output (projector / second screen) |

Both windows share the same `PresentationStateStore` (via Koin singleton), so advancing a slide in the operator window updates the projection window in real time.

The entry point is `app/src/main/kotlin/dev/trinitychurch/lyrics/app/Main.kt` → `fun main()`.

### Run only the tests

```powershell
# all modules
.\gradlew.bat test

# single module
.\gradlew.bat :core:domain:test
```

### IDE (IntelliJ / Android Studio)

Open the repo root. The IDE picks up the Gradle project automatically. Run the `main` run configuration in `:app` or click the gutter icon next to `fun main()` in `Main.kt`.

---

## Generating the Windows Installer (.msi)

```powershell
.\gradlew.bat :app:packageMsi
```

Output: `app/build/compose/binaries/main/msi/Trinity Lyrics-0.1.0.msi`

The MSI is a standard Windows Installer package. It:
- installs per-machine (not per-user)
- creates a Start Menu entry under **Trinity Lyrics**
- creates a desktop shortcut
- is upgradeable via the fixed `upgradeUuid` in `app/build.gradle.kts` — **never change that UUID** after the first release or Windows will treat updates as a new product

### Other distribution targets

```powershell
# native executable only (no installer)
.\gradlew.bat :app:createDistributable

# zip of the distributable
.\gradlew.bat :app:packageDistributionForCurrentOS
```

---

## Tech Stack Reference

| Layer | Library | Version |
|-------|---------|---------|
| UI | Compose Multiplatform Desktop | 1.8.x |
| State | Kotlin StateFlow + coroutines | 1.9.x |
| Navigation | Decompose | 3.x |
| DI | Koin | 4.x |
| Database | SQLDelight (SQLite) | 2.x |
| Video | VLCJ | 4.x |
| WebView | compose-webview-multiplatform (JCEF) | 1.9.x |
| HTTP | Ktor Client | 3.x |
| Testing | Kotest + JUnit 5 | 5.x |

---

## Project Status

Phase 0 — scaffold complete (Gradle multi-module, DI wiring, dual-window proof-of-concept).  
See [`docs/TDD.md`](docs/TDD.md) for the full Technical Design Document.
