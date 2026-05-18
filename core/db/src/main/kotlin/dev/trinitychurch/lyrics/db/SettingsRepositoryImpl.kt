package dev.trinitychurch.lyrics.db

import dev.trinitychurch.lyrics.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val db: TrinityLyricsDatabase
) : SettingsRepository {

    override suspend fun getString(key: String, default: String): String =
        withContext(Dispatchers.IO) {
            db.trinityLyricsQueries.getSetting(key).executeAsOneOrNull() ?: default
        }

    override suspend fun putString(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        db.trinityLyricsQueries.upsertSetting(key = key, value = value)
    }

    override suspend fun getInt(key: String, default: Int): Int =
        withContext(Dispatchers.IO) {
            db.trinityLyricsQueries.getSetting(key).executeAsOneOrNull()?.toIntOrNull() ?: default
        }

    override suspend fun putInt(key: String, value: Int): Unit = withContext(Dispatchers.IO) {
        db.trinityLyricsQueries.upsertSetting(key = key, value = value.toString())
    }
}
