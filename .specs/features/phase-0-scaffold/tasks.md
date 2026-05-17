# Phase 0 Scaffold — Tasks

**Spec:** `.specs/features/phase-0-scaffold/spec.md`
**Status:** Approved
**Last updated:** 2026-05-17

---

## Execution Plan

### Phase 1: Gradle Foundation (Sequential — must be in order)

Everything depends on `settings.gradle.kts` and the version catalog existing first.

```
T1 → T2 → T3
```

- **T1:** Root `settings.gradle.kts` + `gradle/libs.versions.toml`
- **T2:** Root `build.gradle.kts` (convention plugins / allprojects config)
- **T3:** All 8 module `build.gradle.kts` stubs + source directories

### Phase 2: Module Configuration (Parallel OK after T3)

Plugin application and module-specific config can proceed independently once the stubs exist.

```
        ┌──→ T4  (CMP plugin on :app) ──────────────────────┐
T3 ─────┼──→ T5  (:core:domain pure Kotlin) ────────────────┤
        ├──→ T6  (SQLDelight plugin on :core:db) ────────────┤──→ T9
        └──→ T7  (Kotest in every module) ───────────────────┘
                                               T8 depends on T4+T7
```

- **T4 [P]:** CMP Desktop plugin + `compose.desktop` config on `:app`
- **T5 [P]:** `:core:domain` — pure Kotlin stdlib only, Coroutines permitted
- **T6 [P]:** SQLDelight plugin wired on `:core:db` with stub `.sq` file
- **T7 [P]:** `useJUnitPlatform()` + Kotest engine dep in every module's `build.gradle.kts`

### Phase 3: Application Bootstrap (Sequential)

Requires CMP (T4), Kotest (T7), and domain (T5) to be complete.

```
T4 + T5 + T7 → T8 → T9
```

- **T8:** `PresentationStateStore` skeleton + Koin `AppModule` in `:app`
- **T9:** `Main.kt` two-window skeleton wired to store + TDD gate (`./gradlew test`)

---

## Task Breakdown

### T1: Root `settings.gradle.kts` + Version Catalog

**What:** Create `settings.gradle.kts` declaring all 8 modules and `gradle/libs.versions.toml`
with every dependency version used across the project.
**Where:**
- `settings.gradle.kts` (repo root)
- `gradle/libs.versions.toml` (repo root)

**Depends on:** None
**Reuses:** Nothing — greenfield
**Requirement:** SC-01, SC-02, SC-04

**Done when:**
- [ ] `settings.gradle.kts` includes all 8 modules:
  `:app`, `:core:domain`, `:core:db`, `:core:ui`,
  `:feature:lyrics`, `:feature:presentation`, `:feature:media`, `:feature:import`
- [ ] `libs.versions.toml` has `[versions]` block with: `kotlin`, `cmp`, `sqldelight`, `koin`,
  `kotest`, `junit5`, `decompose`, `vlcj`, `compose-webview`, `ktor`, `coroutines`, `serialization`
- [ ] `libs.versions.toml` has `[libraries]` block exposing type-safe accessors for every dep
- [ ] `libs.versions.toml` has `[plugins]` block for: Kotlin JVM, Kotlin Multiplatform,
  Compose Multiplatform, SQLDelight
- [ ] `./gradlew help` runs without "Unresolved reference" or "Could not resolve" errors
- [ ] Gate check passes: `./gradlew help`

**Tests:** none (configuration file — no test code in this task)
**Gate:** build

**Commit:** `chore(gradle): add settings.gradle.kts and libs.versions.toml version catalog`

---

### T2: Root `build.gradle.kts` — Convention Plugins

**What:** Create the root `build.gradle.kts` applying version catalog plugins to subprojects
and configuring shared compile options (Kotlin JVM target, Java toolchain).
**Where:** `build.gradle.kts` (repo root)

**Depends on:** T1
**Reuses:** Nothing
**Requirement:** SC-01, SC-04

**Done when:**
- [ ] Root `build.gradle.kts` exists with `plugins { }` block applying Kotlin plugin via version
  catalog (`alias(libs.plugins.kotlin.jvm)` or multiplatform where appropriate)
- [ ] Kotlin `jvmToolchain(17)` configured (JDK 17 — LTS, required by CMP Desktop)
- [ ] `allprojects` or `subprojects` block sets `group = "dev.trinitychurch.lyrics"` and
  `version = "0.1.0-SNAPSHOT"`
- [ ] No hardcoded version strings (all via `libs.*`)
- [ ] `./gradlew projects` lists all 8 subprojects
- [ ] Gate check passes: `./gradlew projects`

**Tests:** none
**Gate:** build

**Commit:** `chore(gradle): add root build.gradle.kts with JVM toolchain and project metadata`

---

### T3: All 8 Module Build Stubs + Source Directories

**What:** Create `build.gradle.kts` stub and minimal source directory structure for every module
so that `./gradlew assemble` can run on all of them.
**Where:**
- `app/build.gradle.kts`
- `core/domain/build.gradle.kts`
- `core/db/build.gradle.kts`
- `core/ui/build.gradle.kts`
- `feature/lyrics/build.gradle.kts`
- `feature/presentation/build.gradle.kts`
- `feature/media/build.gradle.kts`
- `feature/import/build.gradle.kts`
- Each module's `src/main/kotlin/` and `src/test/kotlin/` directories (with a `.gitkeep`)

**Depends on:** T2
**Reuses:** Root convention from T2
**Requirement:** SC-04, SC-05

**Done when:**
- [ ] All 8 `build.gradle.kts` files exist (stubs — plugin application only, no deps yet)
- [ ] All 8 `src/main/kotlin/` directories exist
- [ ] All 8 `src/test/kotlin/` directories exist
- [ ] `./gradlew assemble` exits 0 across all modules (empty JARs acceptable at this stage)
- [ ] Gate check passes: `./gradlew assemble`

**Tests:** none
**Gate:** build

**Commit:** `chore(gradle): scaffold all 8 module build stubs and source directories`

---

### T4: CMP Desktop Plugin on `:app` [P]

**What:** Apply the Compose Multiplatform plugin to `:app`, configure `compose.desktop`,
declare `nativeDistributions` target, and add `compose.desktop.currentOs` implementation dep.
**Where:** `app/build.gradle.kts`

**Depends on:** T3
**Reuses:** `libs.plugins.compose.multiplatform` from catalog
**Requirement:** SC-07, SC-14

**Done when:**
- [ ] `app/build.gradle.kts` applies `alias(libs.plugins.compose.multiplatform)` and
  `alias(libs.plugins.kotlin.multiplatform)` (or JVM variant — whichever CMP Desktop requires)
- [ ] `compose.desktop { application { mainClass = "dev.trinitychurch.lyrics.app.MainKt" } }` declared
- [ ] `implementation(compose.desktop.currentOs)` declared in dependencies block
- [ ] `./gradlew :app:compileKotlin` exits 0 (even with empty `Main.kt` placeholder)
- [ ] Gate check passes: `./gradlew :app:compileKotlin`

**Tests:** none
**Gate:** build

**Commit:** `chore(:app): apply CMP Desktop plugin and configure compose.desktop`

---

### T5: `:core:domain` — Pure Kotlin, Zero Framework Deps [P]

**What:** Configure `:core:domain` with Kotlin JVM plugin only; explicitly declare that no
Compose, Koin, SQLDelight, Ktor, or VLCJ dependency is permitted in this module.
**Where:** `core/domain/build.gradle.kts`

**Depends on:** T3
**Reuses:** `libs.plugins.kotlin.jvm` from catalog
**Requirement:** SC-06, SC-08

**Done when:**
- [ ] `core/domain/build.gradle.kts` applies only `alias(libs.plugins.kotlin.jvm)`
- [ ] Only `implementation(libs.kotlinx.coroutines.core)` is permitted as a runtime dep
  (coroutines is intentionally allowed for `StateFlow` / `Flow` types in domain interfaces)
- [ ] NO compose, koin, sqldelight, ktor, vlcj, or android dependencies present
- [ ] `./gradlew :core:domain:dependencies --configuration compileClasspath` output contains
  ONLY kotlin-stdlib and kotlinx-coroutines entries
- [ ] Gate check passes: `./gradlew :core:domain:compileKotlin`

**Tests:** none (test code added in T7)
**Gate:** build

**Commit:** `chore(:core:domain): configure as pure-Kotlin module with zero framework deps`

---

### T6: SQLDelight Plugin on `:core:db` [P]

**What:** Apply the SQLDelight Gradle plugin to `:core:db`, configure the database block with
`packageName = "dev.trinitychurch.lyrics.db"`, and create a stub `.sq` file so code generation
can run without error.
**Where:**
- `core/db/build.gradle.kts`
- `core/db/src/main/sqldelight/dev/trinitychurch/lyrics/db/TrinityLyrics.sq` (stub)

**Depends on:** T3
**Reuses:** `libs.plugins.sqldelight` from catalog
**Requirement:** SC-08

**Done when:**
- [ ] `core/db/build.gradle.kts` applies `alias(libs.plugins.sqldelight)`
- [ ] `sqldelight { databases { create("TrinityLyricsDatabase") { packageName = "dev.trinitychurch.lyrics.db" } } }` configured
- [ ] `TrinityLyrics.sq` exists (can be empty or contain a single `-- Phase 0 stub` comment)
- [ ] `./gradlew :core:db:generateTrinityLyricsDatabaseInterface` exits 0
- [ ] Gate check passes: `./gradlew :core:db:generateTrinityLyricsDatabaseInterface`

**Tests:** none
**Gate:** build

**Commit:** `chore(:core:db): apply SQLDelight plugin with stub .sq file`

---

### T7: Kotest + JUnit5 Runner in Every Module [P]

**What:** Add Kotest engine, JUnit5 platform runner, and `useJUnitPlatform()` to every module's
`build.gradle.kts`; write the trivial `StringSpec` TDD gate test in `:core:domain`.
**Where:**
- All 8 `build.gradle.kts` (add `testImplementation` deps + `useJUnitPlatform()`)
- `core/domain/src/test/kotlin/dev/trinitychurch/lyrics/domain/DomainPlaceholderTest.kt` (new file)

**Depends on:** T3, T5 (`:core:domain` must be configured before its test can compile)
**Reuses:** `libs.kotest.runner.junit5`, `libs.kotest.assertions.core` from catalog
**Requirement:** SC-09, SC-10, SC-11, SC-12, SC-13

**Done when:**
- [ ] Every `build.gradle.kts` has:
  ```kotlin
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  ```
- [ ] Every module's `test { useJUnitPlatform() }` block is present
- [ ] `DomainPlaceholderTest.kt` exists with package `dev.trinitychurch.lyrics.domain` and contains:
  ```kotlin
  class DomainPlaceholderTest : StringSpec({
      "domain layer placeholder: 1 + 1 should equal 2" {
          (1 + 1) shouldBe 2
      }
  })
  ```
- [ ] `./gradlew :core:domain:test` exits 0 with 1 test found, 1 passed
- [ ] HTML report generated at `core/domain/build/reports/tests/test/index.html`
- [ ] Gate check passes: `./gradlew test` (all modules)
- [ ] Test count: 1 test passes in `:core:domain`

**Tests:** unit
**Gate:** quick (`./gradlew :core:domain:test`)
**Full gate:** `./gradlew test`

**Commit:** `test(all-modules): configure Kotest + JUnit5 runner; add domain placeholder StringSpec`

---

### T8: `PresentationStateStore` Skeleton + Koin `AppModule`

**What:** Create a minimal `PresentationStateStore` data class holding a single `slideIndex: Int`
counter backed by `MutableStateFlow`, and register it as a Koin `single` in `AppModule`.
**Where:**
- `core/domain/src/main/kotlin/dev/trinitychurch/lyrics/domain/PresentationStateStore.kt`
- `app/src/main/kotlin/dev/trinitychurch/lyrics/app/di/AppModule.kt`

**Depends on:** T4, T5, T7
**Reuses:** `libs.koin.core` (already in `:app` deps after T4)
**Requirement:** SC-18, SC-19, SC-20, SC-21, SC-22, SC-23

**Note on module placement:** `PresentationStateStore` lives in `:core:domain` (pure Kotlin,
zero framework deps — Koin dependency goes in `:app`'s `AppModule`, not in `:core:domain`).
`AppModule` wires `single { PresentationStateStore() }`.

**Done when:**
- [ ] `PresentationStateStore.kt` in `:core:domain` defines:
  ```kotlin
  class PresentationStateStore {
      private val _slideIndex = MutableStateFlow(0)
      val slideIndex: StateFlow<Int> = _slideIndex.asStateFlow()
      fun advance() { _slideIndex.value++ }
  }
  ```
- [ ] `:core:domain` compiles with this class (only stdlib + coroutines needed)
- [ ] `AppModule.kt` in `:app` defines:
  ```kotlin
  val AppModule = module {
      single { PresentationStateStore() }
  }
  ```
- [ ] `:app` `build.gradle.kts` declares `implementation(libs.koin.core)` and
  `implementation(project(":core:domain"))`
- [ ] `./gradlew :app:compileKotlin` exits 0
- [ ] Gate check passes: `./gradlew :app:compileKotlin`

**Tests:** none at this task (integration proven in T9 via `./gradlew run`)
**Gate:** build

**Commit:** `feat(:core:domain): add PresentationStateStore skeleton with StateFlow counter`
`feat(:app): register PresentationStateStore as Koin singleton in AppModule`

---

### T9: `Main.kt` Two-Window Skeleton + Final TDD Gate

**What:** Write `Main.kt` that starts Koin, opens two `Window {}` composables (operator + projection),
wires both to `PresentationStateStore` via `koinInject()`, and runs `./gradlew test` for the
final green gate.
**Where:**
- `app/src/main/kotlin/dev/trinitychurch/lyrics/app/Main.kt`

**Depends on:** T8
**Reuses:** `PresentationStateStore` from `:core:domain`, `AppModule` from T8
**Requirement:** SC-14, SC-15, SC-16, SC-17, SC-18, SC-19

**Done when:**
- [ ] `Main.kt` calls `startKoin { modules(AppModule) }` before any window is created
- [ ] Two `Window {}` composables are created in `application { }` block
- [ ] Operator window title: `"Trinity Lyrics — Operator"` — shows current `slideIndex` and a
  "Click to advance" `Button` that calls `store.advance()`
- [ ] Projection window title: `"Trinity Lyrics — Projection"` — shows current `slideIndex`
- [ ] Both windows resolve `PresentationStateStore` via `koinInject()` (or `get()` from
  `LocalKoinApplication`) — same singleton instance
- [ ] `./gradlew run` launches without exception and two windows appear on screen
- [ ] Clicking the button in the operator window increments the counter visible in both windows
- [ ] `./gradlew test` exits 0 — 1 test in `:core:domain` passes (final TDD gate)
- [ ] Gate check passes (build): `./gradlew run` (manual visual verification)
- [ ] Gate check passes (test): `./gradlew test`
- [ ] Test count: 1 test passes (SC-TEST confirmed)

**Tests:** manual (UI visual verification) + unit gate from T7
**Gate:** full (`./gradlew test` AND manual `./gradlew run` verification)

**Commit:** `feat(:app): two-window Compose Desktop skeleton with shared PresentationStateStore`

---

## Parallel Execution Map

```
Phase 1 (Sequential — Gradle foundation):
  T1 → T2 → T3

Phase 2 (Parallel after T3):
  T3 complete, then:
    ├── T4 [P]  :app CMP plugin
    ├── T5 [P]  :core:domain pure-Kotlin
    ├── T6 [P]  :core:db SQLDelight
    └── T7 [P]  Kotest all modules  (T7 also depends on T5 — wait for T5 first)

  NOTE: T7 depends on T5. Run T4, T5, T6 first in parallel;
        launch T7 only after T5 completes.
  Adjusted:
    T3 → T4 [P], T5 [P], T6 [P]
    T5 complete → T7

Phase 3 (Sequential):
  T4 + T7 complete → T8 → T9
```

---

## Task Granularity Check

| Task | Scope | Status |
|---|---|---|
| T1: settings + catalog | 2 config files, cohesive (catalog is part of settings setup) | OK |
| T2: root build.gradle.kts | 1 file | Granular |
| T3: 8 module stubs + dirs | 8 files — BUT all identical stubs; one operation logically | OK |
| T4: CMP plugin on :app | 1 file, 1 plugin config | Granular |
| T5: :core:domain pure-Kotlin | 1 file, 1 constraint | Granular |
| T6: SQLDelight on :core:db | 1 build file + 1 stub .sq | Granular |
| T7: Kotest all modules + test | 8 build files + 1 test file — cohesive (one configuration) | OK |
| T8: Store + AppModule | 2 files — cohesive (store + its DI registration) | OK |
| T9: Main.kt + final gate | 1 file + gate verification | Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
|---|---|---|---|
| T1 | None | Start of chain | Match |
| T2 | T1 | T1 → T2 | Match |
| T3 | T2 | T2 → T3 | Match |
| T4 | T3 | T3 → T4 [P] | Match |
| T5 | T3 | T3 → T5 [P] | Match |
| T6 | T3 | T3 → T6 [P] | Match |
| T7 | T3, T5 | T5 complete → T7 | Match |
| T8 | T4, T5, T7 | T4 + T7 complete → T8 | Match |
| T9 | T8 | T8 → T9 | Match |

All arrows consistent. No mismatches.

---

## Test Co-location Validation

(No TESTING.md exists yet — greenfield. Tests defined inline per TDD mandate from D-007.)

| Task | Code Layer Created | Test Requirement | Task Says | Status |
|---|---|---|---|---|
| T1 | Gradle config files | none | none | OK |
| T2 | Gradle config | none | none | OK |
| T3 | Module stubs (empty) | none (no logic) | none | OK |
| T4 | Build config only | none | none | OK |
| T5 | Build config constraint | none | none | OK |
| T6 | Build config + stub .sq | none | none | OK |
| T7 | Test infrastructure + StringSpec | unit (D-007) | unit | OK |
| T8 | Domain store + DI module | unit (D-007) | none (integration proven in T9) | OK — store is trivial; T9 is the integration gate |
| T9 | Main.kt entry point | manual UI + existing unit gate | manual + unit | OK |

---

## Risks and Blockers

| Risk | Impact | Mitigation |
|---|---|---|
| CMP Desktop 1.8.x plugin API may differ from 1.7.x docs | T4 blocks T9 | Verify plugin DSL against official JetBrains docs before T4 |
| Kotest 6.x + JUnit5 5.x compatibility requires exact engine artifact name | T7 fails | Use `io.kotest:kotest-runner-junit5` — confirmed artifact for Kotest 6.x |
| SQLDelight 2.x Gradle plugin task name changed from 1.x | T6 fails | Task is `generate[Db]DatabaseInterface` in 2.x — confirm via `./gradlew tasks` |
| `koinInject()` requires `KoinApplication` composable wrapper in CMP | T9 blocks | Use `KoinContext { }` or `rememberKoinInject()` as per Koin 4.x CMP docs |
| Two windows + single JVM process: Compose Desktop requires `application { }` DSL | T9 blocks | Both windows declared inside `application { }` block — this is the standard pattern |
| JDK version mismatch on dev machine | T2+ fails | Pin `jvmToolchain(17)` in root build; ensure JAVA_HOME points to JDK 17+ |
