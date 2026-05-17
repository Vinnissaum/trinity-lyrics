package dev.trinitychurch.lyrics.domain

data class Song(
    val id: String,
    val title: String,
    val artist: String = "",
    val sections: List<SongSection> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val slideConfig: SlideConfig? = null,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
