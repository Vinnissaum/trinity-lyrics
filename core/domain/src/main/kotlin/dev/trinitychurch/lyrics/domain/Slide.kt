package dev.trinitychurch.lyrics.domain

data class Slide(
    val sectionId: String,
    val sectionLabel: String,
    val sectionType: SectionType,
    val lines: List<String>,
    val slideIndexInSection: Int,
    val totalSlidesInSection: Int
)
