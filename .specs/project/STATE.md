# Project State — Trinity Lyrics

**Last updated:** 2026-05-17
**Current phase:** Phase 1 — Lyrics MVP (Specified — ready for execution)

---

## Decisions

**D-001 — Stack: Kotlin + Compose Multiplatform Desktop**
- Date: 2026-05-17
- Decision: Kotlin 2.x + CMP Desktop 1.8.x over Tauri+React
- Rationale: User chose Kotlin despite AI-tooling argument for web stack; JVM ecosystem for VLCJ and JCEF is a strong fit for desktop AV tool
- Status: Final

**D-002 — Dual-window architecture via shared Koin singleton**
- Date: 2026-05-17
- Decision: Both Compose windows share `PresentationStateStore` as a Koin singleton in the same JVM process
- Rationale: No IPC, no serialization, no latency — pure StateFlow emission; simplest possible architecture for this use case
- Status: Final

**D-003 — VLCJ for video (not JavaFX MediaView)**
- Date: 2026-05-17
- Decision: VLCJ 4.x over JavaFX MediaView
- Rationale: VLCJ supports H.264, H.265, and virtually every codec without Windows Media Feature Pack; VLC is widely installed; free
- Status: Final

**D-004 — PPTX deferred to V2**
- Date: 2026-05-17
- Decision: No PPTX rendering at MVP; show "Export to images" helper dialog instead
- Rationale: Adds significant complexity (Apache POI slide rendering); church team can use workaround
- Status: Final

**D-005 — RTSP not supported in V1; HTTP/MJPEG only**
- Date: 2026-05-17
- Decision: IP camera integration via HTTP URLs and MJPEG only; RTSP deferred
- Rationale: JCEF handles HTTP natively; RTSP requires separate media server component
- Status: Final

**D-006 — Holyrics import required at MVP**
- Date: 2026-05-17
- Decision: Holyrics import wizard is a Phase 1 (MVP) requirement, not V2
- Rationale: Church team has existing library in Holyrics; migration blocker for adoption
- Status: Final

**D-007 — TDD workflow with Kotest**
- Date: 2026-05-17
- Decision: Test-first (TDD) for all non-trivial logic; Kotest DSL exclusively (no JUnit5 `@Test` style mixing)
- Rationale: SlideSplitter and PresentationStateStore are pure logic — must be 100% tested before building UI on top
- Status: Final

**D-008 — Soft delete with 30-day recovery**
- Date: 2026-05-17
- Decision: Songs use soft-delete via `deleted_at` timestamp; 30-day recovery window
- Rationale: Church AV teams accidentally delete songs; recovery prevents data loss during service
- Status: Final

**D-009 — SQLDelight UUID primary keys (TEXT)**
- Date: 2026-05-17
- Decision: All primary keys are `TEXT NOT NULL` UUID strings
- Rationale: Enables offline creation, ZIP backup merging, and import deduplication without integer collision
- Status: Final

**D-010 — Windows MSI as primary distribution format**
- Date: 2026-05-17
- Decision: `./gradlew packageMsi` via CMP Desktop `nativeDistributions`; upgadeUuid = `F3A4B5C6-D7E8-4F9A-B0C1-D2E3F4A5B6C7` (must never change)
- Rationale: Bundles JVM — users need no Java installed; integrates with Windows Add/Remove Programs; single `.msi` file to distribute
- Status: Final

**D-012 — Slide navigation: keyboard arrows + Space (like PPT) + click thumbnail**
- Date: 2026-05-17
- Decision: Right arrow + Space = advance; Left arrow = previous; B = blank; F = freeze; Esc = exit. Also: clicking any slide thumbnail in the operator console jumps to that slide.
- Rationale: User explicitly clarified "exactly like PowerPoint" for keyboard nav; thumbnail click is essential for live service recovery (jump without many arrow presses)
- Status: Final

**D-013 — Holyrics export format is JSON array, not .db or .lyr**
- Date: 2026-05-17
- Decision: Parse Holyrics exports as JSON array `[{id, title, artist, lyrics:{paragraphs:[]}}]` using kotlinx.serialization
- Rationale: Real export file provided by user confirmed JSON format (not SQLite DB as assumed in TDD); all paragraph descriptions are empty in practice → default to "Estrofe N"
- Status: Final

**D-014 — Freeze uses `frozenDisplayIndex: Int?` not a boolean flag**
- Date: 2026-05-17
- Decision: `PresentationState.Lyrics.frozenDisplayIndex: Int?` — null = not frozen; non-null = slide index to show on projection while operator navigates `currentSlideIndex` freely
- Rationale: Avoids race condition; single field captures both frozen state and frozen slide index; `displaySlideIndex = frozenDisplayIndex ?: currentSlideIndex`
- Status: Final

**D-011 — App UI language: PT-BR default, EN option; first-run selection screen**
- Date: 2026-05-17
- Decision: App ships PT-BR as default language; English is the only other option (Phase 1 scope). Language is chosen on first run via a full-screen picker and persisted in SQLDelight `settings` table. No external i18n library — plain `StringResources` interface with per-locale objects in `:core:ui`.
- Rationale: Church team is Brazilian (PT-BR default); English needed for international/volunteer users. Moving from Phase 2 to Phase 1 since it shapes all UI string decisions from day one.
- Status: Final

---

## Blockers

**B-001 — Holyrics export format — RESOLVED 2026-05-17**
- What: Holyrics exports as a JSON array (not `.db` or `.lyr` as assumed in TDD)
- Format: `[{ id, title, artist, lyrics: { paragraphs: [{number, description, text}] } }]`
- Resolution: User provided a real 3-song export sample. `HolyricsSongParser` can now be coded.
- Key mapping: `paragraph.description.ifBlank { "Estrofe N" }` → label; all sections are VERSE type
- Status: Resolved

---

## Todos

- [ ] Create `CLAUDE.md` at repo root with stack, module graph, naming conventions, and common gotchas (see TDD.md §Appendix B)
- [ ] Validate Skia renderer on target hardware (5-year-old PC) — Phase 0
- [x] Obtain Holyrics export file from church team — DONE (2026-05-17, JSON format confirmed)
- [ ] Add app icon `.ico` file and wire into `nativeDistributions` (before first MSI build)
- [x] Specify Phase 0 Scaffold feature — DONE (2026-05-17)
- [x] Implement Phase 0 Scaffold (T1–T9) — DONE (2026-05-17)
- [x] Add MSI `nativeDistributions` to `:app` — DONE (2026-05-17)
- [x] Specify i18n language selection feature — DONE (2026-05-17)
- [x] Specify Phase 1 Lyrics MVP — DONE (2026-05-17), 21 tasks, 39 requirements

---

## Deferred Ideas

- GraalVM native image — investigate after Phase 1 if startup > 3 s
- AppCDS — lighter startup optimization, investigate in Phase 0 profiling
- VLC bundling in installer — revisit if adoption is low due to VLC install friction
- CCLI license reporting — explicitly out of scope; document in settings as "coming someday"
- Multi-language lyric variants per song — useful for bilingual services; defer to V2+
- Additional languages beyond PT-BR and EN — driven by adoption
- Windows Credential Manager for camera credentials — security improvement over plain-text URL in DB

---

## Preferences

- Model guidance tip (lightweight tasks): noted once, do not repeat
