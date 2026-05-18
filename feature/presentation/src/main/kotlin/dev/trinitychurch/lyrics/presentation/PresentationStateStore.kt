package dev.trinitychurch.lyrics.presentation

import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresentationStateStore {

    private val _state = MutableStateFlow<PresentationState>(PresentationState.Idle)
    val state: StateFlow<PresentationState> = _state.asStateFlow()

    private var preBlankState: PresentationState.Lyrics? = null

    fun loadSet(set: ServiceSet, songs: Map<String, Song>, config: SlideConfig) {
        val allSlides = set.items
            .sortedBy { it.sortOrder }
            .flatMap { item ->
                val song = songs[item.songId] ?: return@flatMap emptyList()
                song.sections.sortedBy { it.sortOrder }.flatMap { section ->
                    SlideSplitter.split(section, config)
                }
            }
        _state.value = PresentationState.Lyrics(
            set = set,
            allSlides = allSlides,
            currentSlideIndex = 0
        )
    }

    fun advance() {
        val current = _state.value as? PresentationState.Lyrics ?: return
        if (current.currentSlideIndex < current.allSlides.lastIndex) {
            _state.value = current.copy(currentSlideIndex = current.currentSlideIndex + 1)
        }
    }

    fun previous() {
        val current = _state.value as? PresentationState.Lyrics ?: return
        if (current.currentSlideIndex > 0) {
            _state.value = current.copy(currentSlideIndex = current.currentSlideIndex - 1)
        }
    }

    fun jumpToSlide(index: Int) {
        val current = _state.value as? PresentationState.Lyrics ?: return
        _state.value = current.copy(currentSlideIndex = index)
    }

    fun toggleBlank() {
        when (val current = _state.value) {
            is PresentationState.Lyrics -> {
                preBlankState = current
                _state.value = PresentationState.Blank
            }
            is PresentationState.Blank -> {
                _state.value = preBlankState ?: PresentationState.Idle
                preBlankState = null
            }
            is PresentationState.Idle -> {
                _state.value = PresentationState.Blank
            }
        }
    }

    fun toggleFreeze() {
        val current = _state.value as? PresentationState.Lyrics ?: return
        _state.value = if (current.frozen) {
            current.copy(frozenDisplayIndex = null)
        } else {
            current.copy(frozenDisplayIndex = current.currentSlideIndex)
        }
    }

    fun clear() {
        _state.value = PresentationState.Idle
        preBlankState = null
    }
}
