package dev.trinitychurch.lyrics.domain

sealed class Background {
    object Black : Background()
    data class SolidColor(val argb: Int) : Background()
    data class Image(val mediaId: String) : Background()
}
