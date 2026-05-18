package dev.trinitychurch.lyrics.importer

import kotlinx.serialization.Serializable

@Serializable
data class HolyricsSong(
    val id: Long,
    val title: String,
    val artist: String = "",
    val lyrics: HolyricsLyrics
)

@Serializable
data class HolyricsLyrics(val paragraphs: List<HolyricsParagraph>)

@Serializable
data class HolyricsParagraph(
    val number: Int,
    val description: String = "",
    val text: String
)
