# Codebase Concerns

**Analysis Date:** 2026-05-17
**Source:** `docs/TDD.md` §13 (Open Questions and Risks)
**Note:** Project is pre-scaffold — no source files yet. Concerns below are architectural risks identified in the design phase.

---

## Dependencies at Risk

**VLCJ / VLC version mismatch:**
- Risk: VLCJ 4.x requires VLC 3.0+; older VLC causes **silent init failure** with no error message
- Impact: Video playback and video backgrounds silently broken for users with old VLC
- Files: `feature/media/` (not yet created)
- Fix approach: Check VLC version at startup; warn user with actionable message if < 3.0; document minimum VLC version in installer

**JCEF initialization time:**
- Risk: JCEF (Chromium) takes 2–5 seconds to initialize; if done eagerly, violates < 3 s startup target
- Impact: App startup exceeds NFR target on first run
- Files: `feature/webviewer/` (not yet created)
- Fix approach: Lazy-initialize JCEF only when a WebView set item is first loaded — established convention in TDD.md

**Compose / Skia renderer on older Intel GPU:**
- Risk: Skia renderer may fail on old integrated graphics (target hardware: 5-year-old PC)
- Impact: Blank window or crash on operator's machine during Sunday service
- Fix approach: Test on target hardware in Phase 0; fallback via `-Dskiko.renderApi=SOFTWARE` in `gradle.properties`

---

## Fragile Areas

**Dual-monitor window positioning:**
- Files: `app/src/main/kotlin/Main.kt` (not yet created)
- Why fragile: `GraphicsEnvironment.getScreenDevices()` ordering is OS-dependent and can change on monitor reconnect or driver update
- Common failures: Presentation window opens on wrong monitor; ordering shuffled after sleep/wake
- Safe modification: Show monitor preview with resolution labels in settings; allow manual drag as fallback; persist last-used `monitor_index`
- Test coverage: Cannot be automated — manual checklist item before each release

**VLCJ SwingPanel threading:**
- Files: `feature/media/src/main/kotlin/` (not yet created)
- Why fragile: VLCJ video surface updates MUST happen on the EDT (Event Dispatch Thread); Compose runs on its own dispatcher
- Common failures: Black video surface, rendering artifacts, or JVM crash if called from wrong thread
- Safe modification: Always wrap VLCJ state changes in `SwingUtilities.invokeLater { }`

---

## Missing Critical Features

**Holyrics export format unknown:**
- Problem: `HolyricsSongParser` cannot be coded until a real Holyrics export file is obtained; format (SQLite `.db`? XML `.lyr`? proprietary binary?) is unconfirmed
- Current workaround: Placeholder parser; plain-text import (`[Verse 1]` format) as interim
- Blocks: `:feature:import` module completion; Holyrics migration for church team
- Implementation complexity: Low once file is obtained; reverse-engineering the format is the risk
- Action required: Obtain export file from church team before starting `:feature:import`

---

## Test Coverage Gaps

**Zero tests currently:**
- What's not tested: Everything — no source files exist yet
- Risk: All downstream development is untested until test infrastructure is established
- Priority: High — establish test runner green in Phase 0 before writing any feature code
- Difficulty: Low — just need Gradle module with JUnit5 + Kotest configured and one passing test

**Manual-only areas (permanent):**
- What's not tested: Dual-monitor behavior, IP camera streams, VLCJ on real hardware, fullscreen after sleep/wake
- Risk: Regressions only caught in manual testing before releases
- Priority: Medium — document clearly in manual checklist
- Difficulty: High — inherently requires physical hardware and external devices

---

## Performance Bottlenecks (Anticipated)

**JVM startup time on HDD:**
- Problem: JVM cold start on spinning disk may exceed 3 s NFR
- Files: `app/src/main/kotlin/Main.kt` (not yet created)
- Measurement: Not yet measured — profile in Phase 0 on target hardware
- Cause: JVM class loading + JCEF binary (deferred — lazy init mitigates most of this)
- Improvement path: AppCDS (Application Class Data Sharing); GraalVM native image as last resort

**FTS5 search at scale:**
- Problem: FTS5 full-text search may degrade beyond 5,000 songs if not properly indexed
- Files: `core/db/src/main/sqldelight/TrinityLyrics.sq` (not yet created)
- NFR target: < 100 ms for up to 5,000 songs
- Fix approach: `songs_fts` virtual table already planned; ensure triggers keep it in sync

---

## Security Considerations

**IP camera credentials in URL:**
- Risk: Camera credentials (`http://admin:pass@...`) stored in plain text in `set_items.web_url`
- Files: `core/db/` — `set_items` table
- Current mitigation: Local SQLite DB in `%APPDATA%` — not accessible over network
- Recommendation: For V2, consider storing credentials in Windows Credential Manager; acceptable for MVP since data is local-only

---

_Concerns audit: 2026-05-17_
_Update as issues are fixed or new ones discovered_
