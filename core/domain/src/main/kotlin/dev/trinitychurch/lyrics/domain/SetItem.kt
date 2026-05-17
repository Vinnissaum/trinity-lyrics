package dev.trinitychurch.lyrics.domain

data class SetItem(
    val id: String,
    val setId: String,
    val songId: String,
    val sortOrder: Int
)
