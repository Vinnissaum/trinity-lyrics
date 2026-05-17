package dev.trinitychurch.lyrics.domain

interface SettingsRepository {
    suspend fun getString(key: String, default: String): String
    suspend fun putString(key: String, value: String)
    suspend fun getInt(key: String, default: Int): Int
    suspend fun putInt(key: String, value: Int)
}
