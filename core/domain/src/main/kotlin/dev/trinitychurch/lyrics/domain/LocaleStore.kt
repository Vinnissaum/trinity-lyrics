package dev.trinitychurch.lyrics.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocaleStore(private val settings: SettingsRepository) {
    private val _locale = MutableStateFlow(AppLocale.PT_BR)
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    private var _firstRun = true

    suspend fun load() {
        val saved = settings.getString("app.locale", "")
        _firstRun = saved.isEmpty()
        _locale.value = AppLocale.entries.firstOrNull { it.code == saved } ?: AppLocale.PT_BR
    }

    suspend fun setLocale(locale: AppLocale) {
        _locale.value = locale
        settings.putString("app.locale", locale.code)
    }

    fun isFirstRun(): Boolean = _firstRun
}
