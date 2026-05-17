package dev.trinitychurch.lyrics.domain

data class SongSection(
    val id: String,
    val songId: String,
    val label: String,
    val type: SectionType,
    val body: String,
    val sortOrder: Int,
    val repeatCount: Int = 1
)
