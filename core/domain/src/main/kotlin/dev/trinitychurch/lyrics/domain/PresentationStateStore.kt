package dev.trinitychurch.lyrics.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresentationStateStore {
    private val _slideIndex = MutableStateFlow(0)
    val slideIndex: StateFlow<Int> = _slideIndex.asStateFlow()

    fun advance() {
        _slideIndex.value++
    }
}
