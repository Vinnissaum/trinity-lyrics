package dev.trinitychurch.lyrics.domain

sealed class PresentationState {
    object Idle : PresentationState()
    object Blank : PresentationState()

    data class Lyrics(
        val set: ServiceSet,
        val allSlides: List<Slide>,
        val currentSlideIndex: Int,
        val frozenDisplayIndex: Int? = null,
        val background: Background = Background.Black
    ) : PresentationState() {
        val displaySlideIndex: Int get() = frozenDisplayIndex ?: currentSlideIndex
        val frozen: Boolean get() = frozenDisplayIndex != null
    }
}
