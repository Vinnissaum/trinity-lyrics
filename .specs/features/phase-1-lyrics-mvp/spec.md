# Phase 1 — Lyrics MVP Specification

**Feature:** Full lyrics presentation pipeline — CRUD, sets, keyboard + click navigation, import  
**Milestone:** Phase 1 — First real Sunday service  
**Status:** Implemented (⚠️ T16 UI tests missing; T17/T21 manual checklists pending)  
**Last updated:** 2026-05-18  
**Depends on:** Phase 0 scaffold (DONE), i18n-language-selection spec (DONE — implements L-01..L-09)

---

## Problem Statement

Church AV operators use Holyrics today. New volunteers take weeks to learn it. Phase 1 delivers a complete lyrics presentation workflow — import existing songs, build a set list, and run a Sunday service — with a UX simple enough that a first-timer can operate it within 30 minutes. Slide navigation uses keyboard arrows and Space exactly like PowerPoint (zero relearning), plus visual thumbnail click for quick jumps.

---

## Goals

- [ ] Operator imports existing Holyrics library (JSON export) in under 2 minutes
- [ ] Operator builds a service set and presents lyrics to the congregation
- [ ] Slides navigated by keyboard arrows (Left/Right) and Space bar — PowerPoint muscle memory
- [ ] Slides also navigated by clicking any slide thumbnail in the operator console
- [ ] App is used successfully in one real Sunday service without operator confusion

---

## Out of Scope

| Feature | Reason |
|---|---|
| Media (images, videos) | Phase 2 |
| Countdown timer | Phase 2 |
| WebView / IP camera | Phase 2 |
| Video backgrounds for lyrics | Phase 2 |
| Song tags with color UI | Phase 2 (schema exists, UI deferred) |
| PPTX rendering | V2 |
| Keyboard shortcuts 1–9 (direct slide jump) | Phase 2 |
| Fade/slide transitions | Phase 2 — cut only at MVP |
| Presenter notes (operator-only per-slide text) | V2 |
| CCLI export | V2 |
| Backup/export ZIP | Phase 2 |
| Soft-delete recovery UI ("Recently Deleted" screen) | Phase 2 — soft-delete is implemented; recovery UI is deferred |

---

## User Stories

### P1: Import Holyrics Library ⭐ MVP

**User Story**: As an AV operator, I want to import my existing Holyrics song library with one click, so I don't have to re-enter hundreds of songs manually.

**Why P1**: Migration blocker for adoption — church team has existing library; zero value without it.

**Acceptance Criteria**:

1. WHEN user selects a Holyrics JSON export file THEN the app SHALL display a preview: "Encontradas N músicas" / "Found N songs"
2. WHEN user confirms import THEN all songs SHALL be saved to the local DB with `source = "holyrics"`
3. WHEN a paragraph's `description` field is empty or blank THEN the section label SHALL default to "Estrofe N" (PT-BR) or "Strophe N" (EN) where N is the paragraph number
4. WHEN a song with identical title AND artist already exists in the DB THEN the wizard SHALL warn and offer Skip / Overwrite per song
5. WHEN the selected file is not valid JSON or not a JSON array THEN the app SHALL show an error dialog and NOT crash
6. WHEN the JSON array is empty THEN the wizard SHALL show "0 músicas encontradas" / "0 songs found"
7. WHEN import completes THEN the song library screen SHALL immediately reflect all imported songs

**Independent Test**: Select the provided sample Holyrics JSON with 3 songs → confirm → library shows "23", "MARCHAREMOS", "NADA ALÉM DO SANGUE".

---

### P1: Create and Edit Songs ⭐ MVP

**User Story**: As an AV operator, I want to create and edit songs with their sections, so I can enter new songs or correct imported ones.

**Why P1**: Import alone is not enough — operators need to fix typos and add new songs during service prep.

**Acceptance Criteria**:

1. WHEN user creates a new song THEN they SHALL enter a title (required), artist (optional), and at least one section body before saving
2. WHEN editing a song THEN all sections SHALL be listed in sort order with a reorder handle
3. WHEN user drags a section to a new position THEN `sort_order` SHALL update and persist
4. WHEN user saves a song THEN it SHALL appear immediately in the song library list (no manual refresh)
5. WHEN user deletes a song THEN it SHALL be soft-deleted (`deleted_at` set) and disappear from the library; it SHALL NOT appear in set builder search
6. WHEN the song editor is open THEN a live slide preview panel SHALL show how the current section's body splits into slides using the current `SlideConfig`
7. WHEN user adds a section THEN section type SHALL be selectable (Verso / Chorus / Bridge / etc.) and label SHALL be auto-filled ("Verso 1", "Refrão", etc.) with manual override

**Independent Test**: Create a song with 2 sections → drag to reorder → save → verify library shows song, sections in new order.

---

### P1: Search Song Library ⭐ MVP

**User Story**: As an AV operator, I want to search songs by title, artist, or lyrics text, so I can quickly find any song during live service prep.

**Why P1**: With hundreds of songs, search is the only scalable navigation method.

**Acceptance Criteria**:

1. WHEN user types in the search field THEN results SHALL update within 100 ms (SQLite FTS5)
2. WHEN the query matches lyrics body but not title/artist THEN the song SHALL still appear in results
3. WHEN the search field is cleared THEN all non-deleted songs SHALL be shown
4. WHEN no songs match THEN an empty-state message SHALL be shown ("Nenhuma música encontrada")

**Independent Test**: Import 3 songs → search for a word from the lyrics body → correct song appears.

---

### P1: Build a Service Set ⭐ MVP

**User Story**: As an AV operator, I want to create an ordered list of songs for a service, so I can plan the worship set in advance and present it in order.

**Why P1**: Without sets, the operator must manually load each song during service — error-prone under pressure.

**Acceptance Criteria**:

1. WHEN user creates a new set THEN they SHALL give it a name (required) and optional service date
2. WHEN user adds a song to a set THEN they SHALL search the library and tap "Adicionar" / "Add"
3. WHEN user reorders set items via drag THEN the new `sort_order` SHALL persist
4. WHEN user removes a song from a set THEN the song SHALL remain in the library; only the set item is deleted
5. WHEN multiple sets exist THEN they SHALL be listed chronologically by service date

**Independent Test**: Create set "Culto 25/05" → add 3 songs → reorder → remove middle song → set shows 2 songs in correct order.

---

### P1: Navigate Slides via Keyboard ⭐ MVP

**User Story**: As an AV operator presenting a service, I want to navigate slides with Right arrow and Space (advance) and Left arrow (previous) exactly like PowerPoint, so I can run the service using existing muscle memory without looking at the keyboard.

**Why P1**: Core interaction model. Everything else is secondary to this being right.

**Acceptance Criteria**:

1. WHEN operator presses Right arrow OR Space THEN the next slide SHALL appear in the projection window in ≤ 16 ms (one frame at 60 fps)
2. WHEN operator presses Left arrow THEN the previous slide SHALL appear in the projection window in ≤ 16 ms
3. WHEN on the last slide AND Right/Space is pressed THEN nothing SHALL happen (no wrap-around, no crash)
4. WHEN on the first slide AND Left is pressed THEN nothing SHALL happen
5. WHEN between songs in a set THEN advancing past the last slide of song N SHALL move to the first slide of song N+1
6. WHEN B is pressed THEN the projection window SHALL become fully black; pressing B again SHALL restore the current slide
7. WHEN F is pressed THEN the projection window SHALL freeze on the current display slide while the operator console continues to update; pressing F again SHALL unfreeze
8. WHEN Esc is pressed THEN the presentation SHALL exit and the operator console SHALL return to the set view
9. WHEN the operator window does not have focus THEN keyboard shortcuts SHALL still work (operator may click thumbnails, switching focus)

**Independent Test**: Load a 3-song set → advance through all slides with Right arrow → B (black → restore) → F (freeze while navigating) → F (unfreeze) → Esc → back at set view.

---

### P1: Navigate Slides via Thumbnail Click ⭐ MVP

**User Story**: As an AV operator, I want to click any slide thumbnail in the operator console to jump directly to that slide, so I can quickly navigate to any point in the song without pressing arrow keys repeatedly.

**Why P1**: Critical for live service recovery — if you lose your place, you need to jump, not arrow-key back.

**Acceptance Criteria**:

1. WHEN a song is loaded THEN the operator console SHALL display a scrollable grid of all slide thumbnails
2. WHEN a thumbnail is clicked THEN the `currentSlideIndex` SHALL update to that slide AND (if not frozen) the projection window SHALL show it immediately
3. WHEN a thumbnail represents the current display slide THEN it SHALL be visually highlighted (distinct border/color)
4. WHEN frozen AND a thumbnail is clicked THEN `currentSlideIndex` SHALL update (operator navigates freely) BUT `displaySlideIndex` SHALL NOT change (projection stays frozen)
5. WHEN a thumbnail is clicked AND focus moves to the slide grid THEN keyboard shortcuts SHALL continue to work

**Independent Test**: Load song with 10 slides → click slide 7 → projection shows slide 7 → thumbnail 7 highlighted.

---

### P1: Projection Window — Fullscreen Lyrics ⭐ MVP

**User Story**: As a congregation member, I want to see large, readable lyrics on the projector/TV, so I can follow along during worship.

**Why P1**: This IS the product. Without this, nothing else matters.

**Acceptance Criteria**:

1. WHEN a song is loaded THEN the projection window SHALL display slide text in large, centered font on a solid black background
2. WHEN the slide changes THEN the new text SHALL appear in ≤ 16 ms with a cut (no fade at MVP)
3. WHEN in Blank state THEN the projection window SHALL show a fully black screen with no text visible
4. WHEN in Idle state THEN the projection window SHALL show a fully black screen
5. WHEN the presentation window is on a second monitor THEN it SHALL open fullscreen without title bar or OS decorations
6. WHEN the single monitor target is configured in settings THEN the projection window SHALL open on that monitor

**Independent Test**: Physical dual-monitor setup → start presentation → projection window opens fullscreen on second monitor → slides readable from 5 meters.

---

### P1: Plain-Text Song Import

**User Story**: As an AV operator, I want to paste lyrics in plain text with `[Verse 1]` / `[Chorus]` headers, so I can quickly add a new song without filling out a form.

**Why P1**: Operators frequently copy-paste lyrics from websites; this saves minutes per song.

**Acceptance Criteria**:

1. WHEN text contains `[Verse N]` / `[Chorus]` / `[Bridge]` headers THEN each block SHALL become a section with the corresponding `SectionType`
2. WHEN text contains PT-BR headers (`[Estrofe]`, `[Refrão]`, `[Ponte]`, `[Verso]`) THEN they SHALL also map to the correct `SectionType`
3. WHEN text has no headers THEN the entire text SHALL become one section of type VERSE
4. WHEN the paste result is confirmed THEN the song SHALL open in the editor for title/artist entry before saving

**Independent Test**: Paste "SENHOR\nÉ MEU PASTOR\n\n[Refrão]\nHaleluia" → result has 2 sections: Verse + Chorus.

---

### P1: App Settings

**User Story**: As an AV operator, I want to configure the target presentation monitor and font settings, so the app works with my hardware and the text is readable.

**Acceptance Criteria**:

1. WHEN settings screen is open THEN a list of detected monitors SHALL be shown with name and resolution (e.g. "Monitor 2 — 1920×1080")
2. WHEN user selects a monitor THEN the NEXT time a presentation starts it SHALL open on that monitor
3. WHEN user changes base font size THEN the slide preview in the song editor SHALL update immediately
4. WHEN user changes max lines per slide THEN all subsequent slide generations SHALL use the new value
5. WHEN user changes language (PT-BR / EN) THEN the UI SHALL recompose immediately per the i18n spec (L-08, L-09)

**Independent Test**: Change monitor to Monitor 2 → start presentation → projection opens on Monitor 2.

---

### P1: Language Selection (implements i18n spec)

**User Story**: As a user, I want PT-BR (default) or English, with a first-run language picker.

**Why P1**: All UI strings must go through `StringResources` from day one. Hardcoding strings creates a refactor burden that grows with every screen.

**Acceptance Criteria**: Implements requirements L-01 through L-09 from `.specs/features/i18n-language-selection/spec.md`.

---

## Edge Cases

- WHEN a song has zero sections THEN the editor SHALL NOT allow saving (inline validation error)
- WHEN `SlideSplitter` receives a line longer than `maxCharsPerLine` THEN it SHALL word-wrap at the nearest space before the limit; if no space exists it SHALL hard-break at the limit
- WHEN `SlideSplitter` receives an empty body THEN it SHALL return an empty `List<Slide>`
- WHEN the presentation monitor is unplugged mid-service THEN the operator window SHALL remain fully functional; projection window closes or repositions without crashing
- WHEN a set has no items THEN attempting to start presentation SHALL show "Adicione músicas ao culto primeiro" / "Add songs to the set first"
- WHEN the Holyrics JSON has songs with no paragraphs THEN those songs SHALL be imported with zero sections (operator can edit manually)
- WHEN `PresentationStateStore.advance()` is called on the last slide of the last song THEN state SHALL remain unchanged

---

## Requirement Traceability

| ID | Story | Description | Status |
|---|---|---|---|
| P1-01 | Holyrics Import | Parse JSON file → song list with paragraph mapping | Done |
| P1-02 | Holyrics Import | Preview count before confirm | Done |
| P1-03 | Holyrics Import | Save with `source="holyrics"` | Done |
| P1-04 | Holyrics Import | Empty `description` → "Estrofe N" / "Strophe N" | Done |
| P1-05 | Holyrics Import | Duplicate title+artist → warn + skip/overwrite | Done |
| P1-06 | Holyrics Import | Malformed JSON → error dialog, no crash | Done |
| P1-07 | Holyrics Import | Library refreshes immediately post-import | Done |
| P1-08 | Song CRUD | Create song with required title + min 1 section | Done |
| P1-09 | Song CRUD | Edit sections in order with drag-to-reorder | Done |
| P1-10 | Song CRUD | Soft-delete disappears from library + set search | Done |
| P1-11 | Song CRUD | Live slide preview in editor using SlideConfig | Done |
| P1-12 | Song CRUD | Section type selector + auto-label | Done |
| P1-13 | Song Search | FTS5 search < 100ms, body match included | Done |
| P1-14 | Song Search | Clear → all songs shown | Done |
| P1-15 | Song Search | Empty result → empty-state message | Done |
| P1-16 | Set Builder | Create set with name + optional date | Done |
| P1-17 | Set Builder | Add song from library search | Done |
| P1-18 | Set Builder | Drag reorder → `sort_order` persists | Done |
| P1-19 | Set Builder | Remove item → song stays in library | Done |
| P1-20 | Keyboard Nav | Right/Space → advance ≤16ms | Done |
| P1-21 | Keyboard Nav | Left → previous ≤16ms | Done |
| P1-22 | Keyboard Nav | Last slide + Right → no action | Done |
| P1-23 | Keyboard Nav | First slide + Left → no action | Done |
| P1-24 | Keyboard Nav | Advance past last slide of song N → first slide of song N+1 | Done |
| P1-25 | Keyboard Nav | B key → blank/unblank | Done |
| P1-26 | Keyboard Nav | F key → freeze/unfreeze (projection stays, operator navigates) | Done |
| P1-27 | Keyboard Nav | Esc → exit presentation | Done |
| P1-28 | Click Nav | Thumbnail grid shown for active song | Done |
| P1-29 | Click Nav | Click thumbnail → jump (if not frozen) | Done |
| P1-30 | Click Nav | Current display slide thumbnail highlighted | Done |
| P1-31 | Click Nav | Click during freeze → operator moves, projection frozen | Done |
| P1-32 | Projection | Fullscreen lyrics on target monitor, no decorations | Done |
| P1-33 | Projection | Slide change ≤16ms (cut only) | Done |
| P1-34 | Projection | Blank state → fully black screen | Done |
| P1-35 | Plain Text | [Section] headers → SectionType mapping (EN + PT-BR) | Done |
| P1-36 | Plain Text | No headers → single VERSE section | Done |
| P1-37 | Settings | Monitor list with resolution labels | Done |
| P1-38 | Settings | Font size + max lines change → preview updates | Done |
| P1-39 | i18n | All requirements from i18n spec (L-01..L-09) | Done |

---

## Success Criteria

- [ ] Import 3 Holyrics songs in under 30 seconds
- [ ] Build a 5-song service set in under 2 minutes
- [ ] Advance through all slides of a song using only keyboard arrows — zero errors
- [ ] Click any thumbnail to jump — projection updates in ≤1 frame
- [ ] Blank (B) and Freeze (F) work correctly in a dry-run service simulation
- [ ] App used in one real Sunday service without operator confusion or crash
- [ ] `./gradlew test` green with ≥90% state transition coverage on `PresentationStateStore`
- [ ] `./gradlew test` green with 100% branch coverage on `SlideSplitter`
