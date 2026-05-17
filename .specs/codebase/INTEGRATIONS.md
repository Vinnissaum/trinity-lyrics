# External Integrations

**Analyzed:** 2026-05-17
**Source:** `docs/TDD.md`

## Media Playback — VLCJ

**Service:** VLCJ 4.x (Java bindings for VLC)
**Purpose:** Hardware-accelerated video playback (H.264, H.265, and virtually all codecs) for video backgrounds and media presentations
**Implementation:** `:feature:media` and `:feature:presentation`
**Configuration:** VLC must be installed on host machine; app detects VLC at startup
**Authentication:** N/A
**Key integration points:**
- `EmbeddedMediaPlayer` renders via `SwingPanel {}` (Compose Desktop Swing interop)
- Video surface must update on EDT (Event Dispatch Thread)
- Startup detection: search `PATH` + `C:\Program Files\VideoLAN\VLC`

**Startup strategy:**
```
1. Check PATH and common install locations for VLC 3.0+
2. If absent: show one-time banner "VLC required for video. [Install VLC]"
3. If present: initialize VLCJ NativeLibraryLocator with detected path
4. If VLC < 3.0: warn user — version mismatch causes silent init failure
```

**Risk:** VLCJ 4.x requires VLC 3.0+; older VLC causes silent init failure (RISK-2 from TDD.md)

---

## WebView / IP Camera — JCEF (compose-webview-multiplatform)

**Service:** compose-webview-multiplatform 1.9.x wrapping JCEF (Java Chromium Embedded Framework)
**Purpose:** Full Chromium WebView for IP camera HTTP UIs, MJPEG streams, and arbitrary URLs
**Implementation:** `:feature:webviewer`
**Configuration:** JCEF is bundled with the app (~80 MB Chromium binary); no external browser install required
**Authentication:** Camera credentials passed via URL (e.g., `http://admin:pass@192.168.1.10`)
**Security:** JCEF WebView is sandboxed from the host Compose application

**Key integration point:**
```kotlin
@Composable
fun IpCameraView(url: String) {
  WebView(
    state = rememberWebViewState(url),
    modifier = Modifier.fillMaxSize(),
    captureBackPresses = false
  )
}
```

**Initialization:** Lazy — JCEF takes 2–5 seconds to initialize; defer until first WebView set item is loaded
**Limitation:** RTSP streams not supported; HTTP/MJPEG and full Chromium rendering only
**Risk:** ActiveX-only camera UIs won't render in Chromium (RISK-3 from TDD.md)

---

## Data Import — Holyrics

**Service:** Holyrics (external church presentation software)
**Purpose:** One-way import of existing song library from Holyrics into Trinity Lyrics
**Implementation:** `:feature:import` — `HolyricsSongParser.kt`
**Configuration:** User selects Holyrics export file (`.db` or `.lyr` format)
**Authentication:** N/A — file-based only

**Import wizard flow:**
1. User selects Holyrics database file or export folder
2. App parses songs, sections, and tags
3. Preview: "Found 142 songs. 3 duplicates detected."
4. User confirms
5. Songs inserted with `source = "holyrics"` marker

**Field mapping:**
- Holyrics section types → `SectionType` enum
- Holyrics categories → Trinity tags
- Holyrics song backgrounds → linked by file path if media exists

**Critical constraint:** Do NOT implement `HolyricsSongParser` without a real Holyrics export file (OQ-1 from TDD.md). The exact format (`.db` SQLite? `.lyr` XML? proprietary binary?) is unknown until a real export is obtained.

---

## Database — SQLite via SQLDelight

**Service:** SQLite (embedded, file-based)
**Purpose:** All persistent storage — songs, sections, sets, media metadata, settings
**Implementation:** `:core:db` — `TrinityLyrics.sq` schema
**Configuration:**
- Database file: `%APPDATA%\TrinityLyrics\database.db`
- Migrations: `.sqm` files auto-run at startup via SQLDelight migration runner
**Authentication:** N/A — local file access only

**FTS5 virtual table for search:**
```sql
CREATE VIRTUAL TABLE songs_fts USING fts5(
  title, artist, body,
  content='songs',
  content_rowid='rowid'
);
```

**After schema changes:** Run `./gradlew generateSqlDelightInterface` to regenerate DAOs

---

## File System — Media Library

**Service:** Windows file system
**Purpose:** Storage and access for imported media files and generated thumbnails
**Implementation:** `:feature:media` — `MediaRepository`
**Paths:**
- Media files: `%APPDATA%\TrinityLyrics\media\`
- Thumbnails: `%APPDATA%\TrinityLyrics\thumbnails\` (320×180 JPEG)

**Thumbnail generation:**
- Images: resize via `ImageIO` (Java standard library)
- Videos: extract frame at 2s via VLCJ

**File path strategy:** Absolute path stored; relative-to-media-root as fallback for portability

---

## No Network Integrations at MVP

Trinity Lyrics is **100% offline**. No cloud services, no telemetry, no APIs are called at runtime. Ktor Client is a dependency but used only for potential IP camera URL fetching (HTTP, local network only).
