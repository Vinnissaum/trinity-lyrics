package dev.trinitychurch.lyrics.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LocaleStoreSpec : StringSpec({

    fun fakeSettings(initial: Map<String, String> = emptyMap()): SettingsRepository {
        val store = initial.toMutableMap()
        return object : SettingsRepository {
            override suspend fun getString(key: String, default: String): String = store[key] ?: default
            override suspend fun putString(key: String, value: String) { store[key] = value }
            override suspend fun getInt(key: String, default: Int): Int = store[key]?.toIntOrNull() ?: default
            override suspend fun putInt(key: String, value: Int) { store[key] = value.toString() }
        }
    }

    "load() with no saved value -> locale is PT_BR" {
        val store = LocaleStore(fakeSettings())
        store.load()
        store.locale.value shouldBe AppLocale.PT_BR
    }

    "load() with saved 'en' -> locale is EN" {
        val store = LocaleStore(fakeSettings(mapOf("app.locale" to "en")))
        store.load()
        store.locale.value shouldBe AppLocale.EN
    }

    "setLocale(EN) -> updates state and persists to settings" {
        val settings = fakeSettings()
        val store = LocaleStore(settings)
        store.load()
        store.setLocale(AppLocale.EN)
        store.locale.value shouldBe AppLocale.EN
        settings.getString("app.locale", "") shouldBe "en"
    }

    "isFirstRun() is true when key absent, false when locale is already saved" {
        val storeFirst = LocaleStore(fakeSettings())
        storeFirst.load()
        storeFirst.isFirstRun() shouldBe true

        val storeReturn = LocaleStore(fakeSettings(mapOf("app.locale" to "pt-BR")))
        storeReturn.load()
        storeReturn.isFirstRun() shouldBe false
    }
})
