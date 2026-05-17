# Language Selection (i18n) — Specification

**Feature:** PT-BR / English language selection with first-run picker
**Milestone:** Phase 1 — MVP
**Status:** Specified
**Last updated:** 2026-05-17
**Decision ref:** D-011

---

## Problem Statement

All UI strings must be defined from Phase 1 onward. Hardcoding Portuguese directly into Composables
creates a refactor burden later. Defining a `StringResources` interface now costs nothing and allows
both PT-BR (default) and English to be supported simultaneously from the first release.

The first-run experience must not assume PT-BR — a volunteer from a different background should be
able to switch to English before they see any other screen.

---

## Goals

- [ ] All visible app strings go through `StringResources` — no hardcoded UI text in Composables
- [ ] PT-BR is the default when no preference is stored
- [ ] A first-run language picker is shown before the main UI on a fresh install
- [ ] Language can be changed at any time from app settings
- [ ] Language preference persists across restarts (SQLDelight `settings` table)
- [ ] No external i18n library or resource files — plain Kotlin objects only

---

## Out of Scope

| Item | Reason |
|---|---|
| More than 2 languages | Driven by adoption; add via a new `StringResources` impl |
| Per-song language variants | Phase V2+ — tracked in Deferred Ideas |
| RTL layout support | Not needed for PT-BR or EN |
| Installer dialog language | CMP Desktop MSI does not support interactive installer language selection via WiX; app handles language choice post-install |
| Automatic locale detection | Simpler to always show the picker on first run; avoids wrong-language surprises |

---

## Architecture

### `AppLocale` enum — `:core:domain`

```kotlin
enum class AppLocale(val code: String, val displayName: String) {
    PT_BR("pt-BR", "Português (Brasil)"),
    EN("en",       "English")
}
```

### `LocaleStore` — `:core:domain`

```kotlin
class LocaleStore(private val settingsRepository: SettingsRepository) {
    private val _locale = MutableStateFlow(AppLocale.PT_BR)
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    suspend fun load() {
        val saved = settingsRepository.getString("app.locale")
        _locale.value = AppLocale.entries.firstOrNull { it.code == saved } ?: AppLocale.PT_BR
    }

    suspend fun setLocale(locale: AppLocale) {
        _locale.value = locale
        settingsRepository.putString("app.locale", locale.code)
    }

    fun isFirstRun(): Boolean = /* settings key "app.locale" not yet written */ ...
}
```

`SettingsRepository` is a simple interface in `:core:domain` (implemented in `:core:db` via SQLDelight).

### `StringResources` interface — `:core:ui`

One property per UI string. Organized by screen/feature.

```kotlin
interface StringResources {
    // Common
    val appName: String
    val ok: String
    val cancel: String
    val save: String
    val delete: String
    val settings: String

    // Language picker
    val chooseLanguage: String
    val chooseLanguageSubtitle: String
    val continueButton: String

    // Operator window
    val slideLabel: String       // "Slide" / "Slide"
    val advanceButton: String    // "Avançar" / "Advance"
    val blankScreen: String      // "Tela em branco" / "Blank screen"
    val freezeScreen: String     // "Congelar" / "Freeze"

    // … one entry per visible string; add as features are implemented
}
```

### PT-BR and EN implementations — `:core:ui`

```kotlin
object PtBrStrings : StringResources {
    override val appName          = "Trinity Lyrics"
    override val ok               = "OK"
    override val cancel           = "Cancelar"
    override val save             = "Salvar"
    override val delete           = "Excluir"
    override val settings         = "Configurações"
    override val chooseLanguage   = "Escolha o idioma"
    override val chooseLanguageSubtitle = "Você pode alterar isso nas configurações a qualquer momento."
    override val continueButton   = "Continuar"
    override val slideLabel       = "Slide"
    override val advanceButton    = "Avançar"
    override val blankScreen      = "Tela em branco"
    override val freezeScreen     = "Congelar"
}

object EnStrings : StringResources {
    override val appName          = "Trinity Lyrics"
    override val ok               = "OK"
    override val cancel           = "Cancel"
    override val save             = "Save"
    override val delete           = "Delete"
    override val settings         = "Settings"
    override val chooseLanguage   = "Choose language"
    override val chooseLanguageSubtitle = "You can change this in settings at any time."
    override val continueButton   = "Continue"
    override val slideLabel       = "Slide"
    override val advanceButton    = "Advance"
    override val blankScreen      = "Blank screen"
    override val freezeScreen     = "Freeze"
}
```

### `LocalStrings` CompositionLocal — `:core:ui`

```kotlin
val LocalStrings = compositionLocalOf<StringResources> { PtBrStrings }
```

Usage in any Composable:
```kotlin
val strings = LocalStrings.current
Text(strings.advanceButton)
```

Provided at the root level (in `Main.kt`) by observing `LocaleStore.locale`:
```kotlin
val locale by localeStore.locale.collectAsState()
val strings = when (locale) {
    AppLocale.PT_BR -> PtBrStrings
    AppLocale.EN    -> EnStrings
}
CompositionLocalProvider(LocalStrings provides strings) {
    // windows
}
```

---

## User Stories

### P1: First-run language picker ⭐ MVP

**Story:** As a new user opening Trinity Lyrics for the first time, I want to choose my language
before I see any other screen, so the app feels native to me from the start.

**Acceptance Criteria:**

1. WHEN the app starts AND `settings.app.locale` is not set THEN a full-screen language picker SHALL
   be shown before the operator/projection windows
2. WHEN the picker is shown THEN PT-BR SHALL be pre-selected (highlighted / radio checked)
3. WHEN the user selects a language and presses "Continuar / Continue" THEN the preference SHALL be
   saved to `settings.app.locale` AND the main UI SHALL open in that language
4. WHEN the app starts AND `settings.app.locale` IS set THEN the picker SHALL NOT be shown

---

### P1: All UI strings through `StringResources` ⭐ MVP

**Story:** As a developer, I want all visible text to come from `LocalStrings.current` so that adding
a new language never requires touching Composables.

**Acceptance Criteria:**

1. WHEN any Composable displays user-visible text THEN it SHALL use `LocalStrings.current.<property>`
2. WHEN `AppLocale.PT_BR` is active THEN all strings SHALL be in Portuguese
3. WHEN `AppLocale.EN` is active THEN all strings SHALL be in English
4. WHEN a new string is needed THEN a developer SHALL only add it to `StringResources` + both impls

---

### P1: Language change in settings ⭐ MVP

**Story:** As a user, I want to change the app language from settings at any time without restarting.

**Acceptance Criteria:**

1. WHEN the user opens Settings AND changes the language THEN the UI SHALL recompose into the new
   language immediately (no restart required)
2. WHEN the app restarts after a language change THEN it SHALL start in the last selected language
3. WHEN the user changes language THEN the setting SHALL be persisted to `settings.app.locale`

---

## Requirement Traceability

| ID | Story | Status |
|---|---|---|
| L-01 | First-run: picker shown when no locale set | Pending |
| L-02 | First-run: PT-BR pre-selected | Pending |
| L-03 | First-run: saves preference + opens main UI | Pending |
| L-04 | First-run: picker not shown on subsequent starts | Pending |
| L-05 | All strings via `LocalStrings.current` | Pending |
| L-06 | PT-BR strings correct | Pending |
| L-07 | EN strings correct | Pending |
| L-08 | Settings: language change recomposes immediately | Pending |
| L-09 | Settings: language persisted across restarts | Pending |

---

## Module placement

| Artifact | Module | Reason |
|---|---|---|
| `AppLocale` enum | `:core:domain` | Pure Kotlin, referenced by domain logic |
| `LocaleStore` | `:core:domain` | Business logic, StateFlow |
| `SettingsRepository` interface | `:core:domain` | Domain-layer abstraction |
| `StringResources` interface | `:core:ui` | UI contract, depends on `:core:domain` |
| `PtBrStrings`, `EnStrings` | `:core:ui` | UI layer implementations |
| `LocalStrings` CompositionLocal | `:core:ui` | Compose-specific |
| First-run picker Composable | `:app` or `:core:ui` | App-level routing (first-run gate) |

---

## Definition of Done

- [ ] `./gradlew test` passes with unit tests for `LocaleStore` (load default, load saved, change locale)
- [ ] Fresh install (cleared `%APPDATA%\TrinityLyrics\`) shows the language picker before any other screen
- [ ] Selecting PT-BR: all strings display in Portuguese
- [ ] Selecting EN: all strings display in English
- [ ] Changing language in settings recomposes the UI without restart
- [ ] `LocalStrings.current` is used in every Composable that renders user-visible text
- [ ] No hardcoded Portuguese or English strings in any Composable
