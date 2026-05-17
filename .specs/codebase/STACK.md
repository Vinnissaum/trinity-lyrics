# Tech Stack

**Analyzed:** 2026-05-17
**Source:** `docs/TDD.md` (comprehensive technical design doc; no build files exist yet — project is pre-scaffold)

## Core

- Language: Kotlin 2.x
- UI Framework: Compose Multiplatform Desktop 1.8.x
- Build System: Gradle 8.x + Kotlin DSL
- Package Manager: Gradle (convention plugins, version catalog expected)

## UI / Frontend

- UI Framework: Compose Multiplatform Desktop 1.8.x
- State Management: Kotlin `StateFlow` + Compose `collectAsState`
- Navigation: Decompose 3.x (JetBrains)
- Icons: Material Icons Extended (same version as CMP)
- WebView: compose-webview-multiplatform 1.9.x (JCEF / Chromium)

## Backend / Data

- Database: SQLDelight 2.x over SQLite
  - FTS5 virtual table for full-text search
  - Migration files: `.sqm` format, auto-run at startup
- DI: Koin Multiplatform 4.x
- HTTP Client: Ktor Client 3.x (used for camera URL fetching if needed)
- Settings: SQLDelight key-value table (`settings`)

## Media

- Video Playback: VLCJ 4.x (requires VLC 3.0+ installed on host)
  - `EmbeddedMediaPlayer` rendered via `SwingPanel` in Compose
- Image: Compose `Image` composable + ImageIO for thumbnail generation
- WebView / IP Camera: JCEF (bundled Chromium, ~80 MB)

## Testing

- Unit: JUnit5 5.x + Kotest 6.x
- UI: Compose UI Testing Framework (same version as CMP)
- Integration: JUnit5 + in-memory SQLite (SQLDelight)
- E2E: Manual checklist (no automated E2E framework)
- Coverage: Not yet configured

## External Services / Integrations

- Holyrics: file-based import (`.db` / `.lyr` — format TBD; real export file needed)
- IP Cameras: HTTP/MJPEG URLs rendered in JCEF WebView (RTSP not supported)
- VLC: external runtime dependency (detection at startup)

## Development Tools

- Build: Gradle 8.x + Kotlin DSL
- Dependency management: Gradle Version Catalog (planned)
- Persistence paths: `%APPDATA%\TrinityLyrics\` (database, media, thumbnails)
- PPTX (V2 only): Apache POI 5.x (deferred)

## Platform

- Target OS: Windows 10 (1803+) and Windows 11 — **Windows-first**
- JVM: JVM-based desktop app (no GraalVM native image at MVP; AppCDS investigated if startup exceeds 3 s)
- Installer target size: < 120 MB (JCEF ~80 MB dominates)
