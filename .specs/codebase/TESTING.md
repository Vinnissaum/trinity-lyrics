# Testing Infrastructure

**Analyzed:** 2026-05-17
**Source:** `docs/TDD.md` §11 (Testing Strategy)
**Status:** Planned — no test files exist yet; conventions established here

## Test Frameworks

**Unit / Logic:** JUnit5 5.x + Kotest 6.x
**UI Testing:** Compose UI Testing Framework (same version as CMP 1.8.x)
**Integration:** JUnit5 + SQLDelight in-memory SQLite driver
**E2E:** Manual checklist (no automated E2E framework at MVP)
**Coverage:** Not yet configured (planned for Phase 1)

## Test Organization

**Location:** Mirror of source — `src/test/kotlin/` within each module
**Naming:**
- Test files: `<ClassName>Test.kt` — e.g., `SlideSplitterTest.kt`, `PresentationStateStoreTest.kt`
- No `Test` suffix on Kotest spec classes — use descriptive names: `SlideSplitterSpec`

**Style:** Kotest DSL exclusively — do not mix with JUnit5 `@Test` annotation style
**Preferred spec style:** `StringSpec` or `FunSpec` — pick one per module and be consistent

## Testing Patterns

### Unit Tests

**Approach:** Pure Kotlin — no DI, no DB, no Compose runtime
**Location:** `feature/<name>/src/test/kotlin/`
**Pattern:**
```kotlin
class SlideSplitterSpec : StringSpec({
  "splits section at maxLines boundary" {
    val section = SongSection(body = "Line1\nLine2\nLine3\nLine4\nLine5", ...)
    val config = SlideConfig(maxLinesPerSlide = 4, maxCharsPerLine = 60)
    val slides = SlideSplitter.split(section, config)
    slides shouldHaveSize 2
    slides[0].lines shouldHaveSize 4
    slides[1].lines shouldHaveSize 1
  }
})
```

**Priority unit test targets (from TDD.md):**
1. `SlideSplitter` — empty sections, single-word lines, exactly at maxLines, over limit, repeat sections
2. `PresentationStateStore` — all state transitions (advance past last slide, blank toggle, freeze, countdown tick)
3. `HolyricsSongParser` — all section type mappings, duplicate detection, malformed input
4. `CountdownTimer` — tick accuracy, end-action trigger, pause/resume

### Integration Tests

**Approach:** Real SQLDelight DAOs with in-memory SQLite driver
**Location:** `core/db/src/test/kotlin/` or per-feature `src/test/kotlin/`
**Pattern:** Create in-memory DB driver → run schema → execute DAO operations → assert

**Priority integration test targets:**
- SQLDelight DAOs — CRUD + FTS5 search against in-memory SQLite
- `MediaRepository` — import, thumbnail generation, deletion
- `SettingsStore` — get/set round-trip

### UI Tests (Compose Testing Framework)

**Approach:** `createComposeRule()` + semantic tree assertions
**Location:** `feature/<name>/src/test/kotlin/` (requires compose testing dep)
**Priority UI test targets:**
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

## Test Execution

**All tests:**
```
./gradlew test
```

**Specific module:**
```
./gradlew :feature:presentation:test
./gradlew :core:db:test
./gradlew :feature:library:test
```

**Generate SQLDelight before testing DB:**
```
./gradlew generateSqlDelightInterface
./gradlew :core:db:test
```

**Configuration:** No special test configuration files yet; JUnit5 platform configured via Gradle

## Coverage Targets

**Current:** No coverage measurement in place
**Goals (Phase 1):**
- `SlideSplitter`: 100% branch coverage (pure algorithm — must be airtight)
- `PresentationStateStore`: 90%+ state transition coverage
- Core parsers: 85%+ line coverage
**Enforcement:** Not yet automated; manual review during code review

## Test Coverage Matrix

| Code Layer | Required Test Type | Location Pattern | Run Command |
|---|---|---|---|
| `SlideSplitter` (pure algorithm) | Unit | `feature/presentation/src/test/kotlin/` | `./gradlew :feature:presentation:test` |
| `PresentationStateStore` (state transitions) | Unit | `feature/presentation/src/test/kotlin/` | `./gradlew :feature:presentation:test` |
| `HolyricsSongParser` | Unit | `feature/import/src/test/kotlin/` | `./gradlew :feature:import:test` |
| `CountdownTimer` | Unit | `feature/countdown/src/test/kotlin/` | `./gradlew :feature:countdown:test` |
| SQLDelight DAOs | Integration | `core/db/src/test/kotlin/` | `./gradlew :core:db:test` |
| `MediaRepository` | Integration | `feature/media/src/test/kotlin/` | `./gradlew :feature:media:test` |
| `SettingsStore` | Integration | `core/settings/src/test/kotlin/` | `./gradlew :core:settings:test` |
| Compose UI screens | UI (Compose Testing) | `feature/<name>/src/test/kotlin/` | `./gradlew :<feature>:test` |
| Dual-monitor / VLCJ / JCEF | Manual only | — | Manual checklist |

## Parallelism Assessment

| Test Type | Parallel-Safe? | Isolation Model | Evidence |
|---|---|---|---|
| Unit tests | Yes | No shared state — pure functions, no DI, no DB | `SlideSplitter` is a pure `object`; `PresentationStateStore` instantiated fresh per test |
| Integration (in-memory SQLite) | Yes | Each test creates its own in-memory SQLite driver — no shared DB | SQLDelight in-memory driver is per-connection |
| Compose UI tests | Unknown | `createComposeRule()` isolation — assess when first UI tests are written | — |
| Manual | N/A | Sequential by definition | — |

## Gate Check Commands

| Gate Level | When to Use | Command |
|---|---|---|
| Quick | After unit-only tasks (SlideSplitter, parsers, state transitions) | `./gradlew :feature:presentation:test` (or specific module) |
| Module | After completing a feature module | `./gradlew :<module>:test` |
| Full | After multi-module tasks or before PR | `./gradlew test` |
| Build | Before each phase completion / release | `./gradlew build` (compiles + all tests) |

## TDD Workflow (Project Convention)

**Test-First approach — enforced for all non-trivial logic:**

1. Write failing test(s) describing the expected behavior
2. Run test — confirm RED
3. Implement minimum code to pass
4. Run test — confirm GREEN
5. Refactor — run again to confirm GREEN
6. Commit with both test and implementation in same atomic commit

**TDD is mandatory for:**
- `SlideSplitter` (must be 100% tested before building the presenter UI)
- All parsers (`HolyricsSongParser`, `PlainTextSongParser`)
- All state machine transitions in `PresentationStateStore`
- `CountdownTimer` logic

**TDD is encouraged (not mandatory) for:**
- Repository methods (use integration tests)
- Compose UI (use UI tests where feasible)
