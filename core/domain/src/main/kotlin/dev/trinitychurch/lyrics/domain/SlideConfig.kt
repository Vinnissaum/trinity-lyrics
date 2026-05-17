package dev.trinitychurch.lyrics.domain

data class SlideConfig(
    val maxLinesPerSlide: Int = 4,
    val maxCharsPerLine: Int = 60,
    val fontSizeSp: Int = 48
)
