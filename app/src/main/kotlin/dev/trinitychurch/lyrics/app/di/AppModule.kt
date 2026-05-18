package dev.trinitychurch.lyrics.app.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.db.SetRepository
import dev.trinitychurch.lyrics.db.SettingsRepositoryImpl
import dev.trinitychurch.lyrics.db.SongRepository
import dev.trinitychurch.lyrics.db.TrinityLyricsDatabase
import dev.trinitychurch.lyrics.domain.LocaleStore
import dev.trinitychurch.lyrics.domain.SettingsRepository
import dev.trinitychurch.lyrics.presentation.PresentationStateStore
import org.koin.dsl.module
import java.io.File

val AppModule = module {
    single<SqlDriver> {
        val appData = System.getenv("APPDATA")
            ?: "${System.getProperty("user.home")}/AppData/Roaming"
        val dbDir = File("$appData/TrinityLyrics").also { it.mkdirs() }
        val dbFile = File(dbDir, "trinity.db")
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) TrinityLyricsDatabase.Schema.create(driver)
        driver
    }
    single { TrinityLyricsDatabase(get()) }
    single { SongRepository(get(), get()) }
    single { SetRepository(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single { PresentationStateStore() }
    single { LocaleStore(get()) }
}
