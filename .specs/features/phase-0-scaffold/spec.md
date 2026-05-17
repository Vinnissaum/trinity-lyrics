# Phase 0 Scaffold — Specification

**Feature:** Gradle Multi-Module Project Setup + Kotest Green
**Milestone:** Phase 0 — Skeleton
**Status:** Approved
**Last updated:** 2026-05-17

---

## Problem Statement

There is no compilable Gradle project yet. Every downstream feature (TDD for `SlideSplitter`,
`PresentationStateStore`, SQLDelight schema, Compose windows) is blocked until the multi-module
scaffold exists, compiles cleanly, and proves that the test runner is wired correctly. This
feature is the single prerequisite that unblocks all TDD work.

---

## Goals

- [ ] All 8 modules declared in `settings.gradle.kts` and compile with zero errors
- [ ] `./gradlew test` exits 0 with at least 1 passing Kotest `StringSpec` test in `:core:domain`
- [ ] `./gradlew run` opens a two-window Compose Desktop skeleton (operator + projection)
- [ ] Version catalog (`libs.versions.toml`) is the single source of truth for all dependency versions
- [ ] `:core:domain` has zero framework dependencies (pure Kotlin only)

---

## Out of Scope

| Feature | Reason |
|---|---|
| Any real business logic | Phase 1+; this scaffold is skeleton only |
| SQLDelight schema tables | Separate Phase 0 feature (DB Connection) |
| VLCJ integration | Separate Phase 0 feature |
| JCEF/WebView integration | Separate Phase 0 feature |
| Decompose navigation | Phase 1; skeleton uses direct Koin wiring only |
| Skia renderer performance validation | Separate todo; not a build task |
| Installer packaging | Phase 3 |

---

## User Stories

### P1: Version Catalog + Root Build ⭐ MVP

**User Story:** As a developer, I want a single `libs.versions.toml` version catalog so that all
8 modules share identical dependency versions with no duplication.

**Why P1:** Without a version catalog, each module declares its own versions, causing drift and
classpath conflicts across the project lifetime.

**Acceptance Criteria:**

1. WHEN `gradle/libs.versions.toml` exists THEN it SHALL declare versions for: Kotlin 2.x, CMP
   Desktop 1.8.x, SQLDelight 2.x, Koin 4.x, Kotest 6.x, JUnit5 5.x, Decompose 3.x, VLCJ 4.x,
   compose-webview 1.9.x, Ktor 3.x, Coroutines, Serialization
2. WHEN any module references a library THEN it SHALL use the type-safe catalog accessor
   (e.g. `libs.kotest.runner.junit5`) — no hardcoded version strings in any `build.gradle.kts`
3. WHEN `./gradlew dependencies` runs on any module THEN it SHALL resolve without version conflict
   warnings

**Independent Test:** Run `./gradlew :core:domain:dependencies` and confirm no "FAILED" or version
conflict lines appear.

---

### P1: All 8 Modules Scaffold + Compile ⭐ MVP

**User Story:** As a developer, I want all 8 modules declared and compilable so that I can start
feature work in any module without Gradle setup overhead.

**Why P1:** If any module fails to compile at scaffold time, feature work in that module is
blocked indefinitely.

**Modules:**
- `:app` — CMP Desktop entry point; `Main.kt`
- `:core:domain` — Pure Kotlin domain types; zero framework deps
- `:core:db` — SQLDelight plugin; DAO interfaces
- `:core:ui` — Shared Compose UI primitives
- `:feature:lyrics` — Song library UI + domain wiring
- `:feature:presentation` — Projection + operator console
- `:feature:media` — Media library (Phase 2 prep)
- `:feature:import` — Import wizard (plain-text + Holyrics)

**Acceptance Criteria:**

1. WHEN `settings.gradle.kts` is evaluated THEN all 8 module paths SHALL be declared via `include()`
2. WHEN `./gradlew assemble` runs THEN all 8 modules SHALL compile with exit code 0
3. WHEN `:core:domain` `build.gradle.kts` is inspected THEN it SHALL have NO dependency on
   `compose`, `koin`, `sqldelight`, `ktor`, `vlcj`, or any UI/framework artifact — pure Kotlin + coroutines only
4. WHEN `:app` `build.gradle.kts` is inspected THEN it SHALL apply the CMP Desktop plugin and
   declare `compose.desktop.currentOs` as an implementation dependency
5. WHEN `:core:db` `build.gradle.kts` is inspected THEN it SHALL apply the SQLDelight plugin with
   `database { packageName = "dev.trinitychurch.lyrics.db" }`

**Independent Test:** `./gradlew assemble` exits 0 — all module JARs produced.

---

### P1: Kotest Configured in Every Module — Green Gate ⭐ MVP

**User Story:** As a developer, I want Kotest configured in all modules and a passing StringSpec
test in `:core:domain` so that I can write TDD tests immediately without additional Gradle setup.

**Why P1:** D-007 (TDD workflow with Kotest) is a final architectural decision. No feature work
proceeds until the test runner is proven green. This is the TDD gate for the entire project.

**Acceptance Criteria:**

1. WHEN `./gradlew test` runs THEN it SHALL exit 0 across all modules
2. WHEN `:core:domain` tests run THEN the `StringSpec` test
   `"domain layer placeholder: 1 + 1 should equal 2"` SHALL pass
3. WHEN any module's test task runs THEN it SHALL use Kotest's JUnit5 runner
   (`useJUnitPlatform()` declared in every module's `build.gradle.kts`)
4. WHEN `./gradlew :core:domain:test` runs THEN the Kotest HTML report SHALL be generated at
   `core/domain/build/reports/tests/test/index.html`
5. WHEN a test is written in Kotest `FunSpec` or `BehaviorSpec` style in any module THEN it SHALL
   be discovered and executed by `./gradlew test` without additional configuration

**Independent Test:** `./gradlew :core:domain:test --tests "dev.trinitychurch.lyrics.domain.*"` — 1 test found, 1 passed, 0 failed.

---

### P1: Two-Window Compose Desktop Skeleton ⭐ MVP

**User Story:** As a developer, I want `./gradlew run` to open two Compose Desktop windows so that
the dual-monitor architecture is proven before any feature logic is built.

**Why P1:** The dual-window shared-state architecture (D-002) is the most novel part of the stack.
If it can't be proven at scaffold time, all feature work risks rearchitecting later.

**Acceptance Criteria:**

1. WHEN `./gradlew run` is executed THEN the JVM SHALL launch and two `Window {}` composables SHALL
   open on screen
2. WHEN the operator window opens THEN it SHALL display a "Click to advance" button showing the
   current slide index from `PresentationStateStore`
3. WHEN the projection window opens THEN it SHALL display the current slide index from
   `PresentationStateStore`
4. WHEN the operator clicks "Click to advance" THEN the slide index SHALL increment in both windows
   simultaneously (StateFlow propagation proven)
5. WHEN `PresentationStateStore` is resolved from Koin THEN it SHALL return the same singleton
   instance from both window composables (shared state proven)
6. WHEN the app starts THEN Koin SHALL initialize `AppModule` before any window is shown

**Independent Test:** `./gradlew run` — two windows appear, clicking the button increments the counter in both.

---

### P1: Koin AppModule Wired ⭐ MVP

**User Story:** As a developer, I want Koin 4.x bootstrapped in `:app` so that all future
singletons (stores, repositories, DAOs) have a DI container ready.

**Why P1:** `PresentationStateStore` (D-002) must be a Koin singleton resolvable from both
windows at startup. Without Koin, the two-window sync cannot be demonstrated.

**Acceptance Criteria:**

1. WHEN `Main.kt` runs THEN it SHALL call `startKoin { modules(AppModule) }` before opening windows
2. WHEN `AppModule` is defined THEN it SHALL declare `PresentationStateStore` as `single { ... }`
3. WHEN `PresentationStateStore` is retrieved via `get<PresentationStateStore>()` from two
   different call sites THEN both SHALL resolve to the identical object instance
4. WHEN Koin is initialized THEN it SHALL NOT throw any `NoBeanDefinitionException` for
   `PresentationStateStore`

**Independent Test:** `./gradlew run` — no Koin exception in stdout; both windows share identical store reference (confirmed by object identity check in debug log).

---

## Edge Cases

- WHEN `:core:domain` is added as a dependency of `:app` or any feature module THEN it SHALL NOT
  transitively pull framework JARs into `:core:domain`'s compile classpath
- WHEN `./gradlew clean build` runs (cold cache) THEN it SHALL succeed with exit 0 (no
  incremental-state dependencies)
- WHEN SQLDelight code generation runs (`./gradlew generateSqlDelightInterface`) THEN it SHALL
  produce Kotlin source files even if the `.sq` schema file is empty (stub only at Phase 0)
- WHEN both Compose windows are open and the user closes one THEN the app SHALL NOT crash
  (window close handled gracefully in the skeleton)
- WHEN Kotest version and JUnit5 version are declared THEN they SHALL be compatible (Kotest 6.x
  requires JUnit5 5.10+)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| SC-01 | P1: Version Catalog | Design | Pending |
| SC-02 | P1: Version Catalog | Design | Pending |
| SC-03 | P1: Version Catalog | Design | Pending |
| SC-04 | P1: All 8 Modules | Design | Pending |
| SC-05 | P1: All 8 Modules | Design | Pending |
| SC-06 | P1: All 8 Modules | Design | Pending |
| SC-07 | P1: All 8 Modules | Design | Pending |
| SC-08 | P1: All 8 Modules | Design | Pending |
| SC-09 | P1: Kotest Green | Design | Pending |
| SC-10 | P1: Kotest Green | Design | Pending |
| SC-11 | P1: Kotest Green | Design | Pending |
| SC-12 | P1: Kotest Green | Design | Pending |
| SC-13 | P1: Kotest Green | Design | Pending |
| SC-14 | P1: Two-Window Skeleton | Design | Pending |
| SC-15 | P1: Two-Window Skeleton | Design | Pending |
| SC-16 | P1: Two-Window Skeleton | Design | Pending |
| SC-17 | P1: Two-Window Skeleton | Design | Pending |
| SC-18 | P1: Two-Window Skeleton | Design | Pending |
| SC-19 | P1: Two-Window Skeleton | Design | Pending |
| SC-20 | P1: Koin AppModule | Design | Pending |
| SC-21 | P1: Koin AppModule | Design | Pending |
| SC-22 | P1: Koin AppModule | Design | Pending |
| SC-23 | P1: Koin AppModule | Design | Pending |

---

## Success Criteria (Definition of Done)

- [ ] **SC-BUILD:** `./gradlew clean build` exits 0 on a cold cache — all 8 modules compile
- [ ] **SC-TEST:** `./gradlew test` exits 0 — at least 1 Kotest test in `:core:domain` passes
- [ ] **SC-RUN:** `./gradlew run` opens two Compose windows with shared counter state
- [ ] **SC-SYNC:** Clicking "Click to advance" in the operator window increments the counter visible in both windows simultaneously
- [ ] **SC-DOMAIN:** `:core:domain` has zero compile-time dependencies on any UI/framework artifact (verified by `./gradlew :core:domain:dependencies`)
- [ ] **SC-CATALOG:** No hardcoded version strings in any `build.gradle.kts` — all versions via `libs.*`
