# Trinity Lyrics

**Vision:** A Windows-first desktop app for church liturgical presentation that a non-technical volunteer can operate within 30 minutes of first use — replacing Holyrics with a purpose-built, confidence-inspiring UX.
**For:** Church AV volunteers (non-technical); primary operator is typically a new volunteer each Sunday
**Solves:** Holyrics is feature-rich but UX-heavy; new volunteers take too long to learn it; Trinity Lyrics makes Sunday morning presentation frictionless

## Goals

- **Operator confidence:** Any volunteer can run a full Sunday service within 30 minutes of first opening the app (no measurable training time required)
- **Reliable dual-monitor presentation:** Presentation window opens on projector, syncs with operator console in < 16 ms (1 frame at 60 fps)
- **Fast startup:** App ready in < 3 seconds on a 5-year-old Windows PC (i5, 8 GB RAM, HDD)
- **Offline-first:** 100% functionality with zero network access during service
- **Holyrics migration:** Church can import existing song library via Holyrics import wizard at MVP launch

## Tech Stack

**Core:**
- Language: Kotlin 2.x
- UI Framework: Compose Multiplatform Desktop 1.8.x
- Database: SQLDelight 2.x (SQLite)
- DI: Koin 4.x
- Navigation: Decompose 3.x

**Key dependencies:**
- VLCJ 4.x — video playback (requires VLC installed)
- compose-webview-multiplatform 1.9.x — JCEF WebView for IP cameras
- JUnit5 5.x + Kotest 6.x — TDD testing
- Ktor Client 3.x — HTTP (local network only)

## Scope

**MVP (Phase 0 + Phase 1) includes:**
- Full song CRUD with section editor (verse, chorus, bridge, etc.)
- SQLite FTS5 full-text search across title, artist, tags, lyrics
- Service set builder (songs only for Phase 1)
- Dual-monitor fullscreen lyrics presentation
- Keyboard-first controls: Space, arrows, B (blank), F (freeze), Esc, 1-9
- Solid-color and static-image backgrounds
- Plain-text import (`[Verse 1]` format)
- Holyrics import wizard (requires real export file)
- App settings: font, slide layout, target monitor
- Portuguese UI strings

**V1 (Phase 2) includes:**
- Media library (images + videos via VLCJ)
- Video backgrounds for lyrics
- Countdown timer with video background support
- Web/IP camera viewer (JCEF WebView)
- Set items: all types (song, media, countdown, webview, blank)
- Library export/import (ZIP backup)
- Fade transitions
- Portuguese UI + English option

**Explicitly out of scope (MVP and V1):**
- PPTX/slide rendering (deferred to V2)
- RTSP camera streaming (HTTP/MJPEG only)
- Cloud storage or multi-device sync
- Multi-user collaboration
- Live streaming integration
- CCLI license reporting
- Mobile or web deployment
- Audio mixing or video editing

## Constraints

- **Platform:** Windows 10 (1803+) and Windows 11 only — Windows-first
- **Runtime:** JVM desktop (no mobile, no web, no GraalVM native at MVP)
- **External dependency:** VLCJ requires VLC 3.0+ installed on host machine
- **Holyrics blocker:** Cannot implement `HolyricsSongParser` without a real Holyrics export file — must obtain from church team before starting `:feature:import`
- **Development:** AI-first development using Claude Code as primary dev tool
- **Installer size:** < 120 MB (JCEF ~80 MB dominates)
