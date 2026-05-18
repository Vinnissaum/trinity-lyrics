package dev.trinitychurch.lyrics.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class SettingsRepositoryTest : FunSpec({

    lateinit var driver: JdbcSqliteDriver
    lateinit var repo: SettingsRepositoryImpl

    beforeEach {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrinityLyricsDatabase.Schema.create(driver)
        repo = SettingsRepositoryImpl(TrinityLyricsDatabase(driver))
    }

    afterEach {
        driver.close()
    }

    test("putString → getString returns stored value") {
        runBlocking {
            repo.putString("k", "v")
            repo.getString("k", "") shouldBe "v"
        }
    }

    test("getString with missing key returns default") {
        runBlocking {
            repo.getString("missing", "default") shouldBe "default"
        }
    }

    test("putInt → getInt round-trip") {
        runBlocking {
            repo.putInt("font_size", 48)
            repo.getInt("font_size", 0) shouldBe 48
        }
    }

    test("getInt with missing key returns default") {
        runBlocking {
            repo.getInt("not_set", 99) shouldBe 99
        }
    }

    test("putString overwrites existing value") {
        runBlocking {
            repo.putString("key", "first")
            repo.putString("key", "second")
            repo.getString("key", "") shouldBe "second"
        }
    }
})
