package dev.trinitychurch.lyrics.presentation

import dev.trinitychurch.lyrics.domain.PresentationState
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PresentationStateStoreSpec : FunSpec({

    fun store() = PresentationStateStore()

    fun song(id: String = "song-1", sectionBody: String = "Line 1\nLine 2") = Song(
        id = id,
        title = "Test Song",
        sections = listOf(
            SongSection(
                id = "$id-sec",
                songId = id,
                label = "Verso 1",
                type = SectionType.VERSE,
                body = sectionBody,
                sortOrder = 1
            )
        )
    )

    fun set(vararg songIds: String) = ServiceSet(
        id = "set-1",
        name = "Sunday Service",
        items = songIds.mapIndexed { index, songId ->
            SetItem(id = "item-$index", setId = "set-1", songId = songId, sortOrder = index)
        }
    )

    val defaultConfig = SlideConfig()

    test("loadSet → state is Lyrics with currentSlideIndex = 0") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song()), defaultConfig)
        val state = store.state.value
        state.shouldBeInstanceOf<PresentationState.Lyrics>()
        (state as PresentationState.Lyrics).currentSlideIndex shouldBe 0
    }

    test("advance → currentSlideIndex increments") {
        val store = store()
        val body = "L1\nL2\nL3\nL4\nL5" // 5 lines → 2 slides with maxLinesPerSlide=4
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.advance()
        (store.state.value as PresentationState.Lyrics).currentSlideIndex shouldBe 1
    }

    test("advance at last slide → currentSlideIndex unchanged") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = "One line")), defaultConfig)
        store.advance()
        (store.state.value as PresentationState.Lyrics).currentSlideIndex shouldBe 0
    }

    test("previous at first slide → currentSlideIndex unchanged") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song()), defaultConfig)
        store.previous()
        (store.state.value as PresentationState.Lyrics).currentSlideIndex shouldBe 0
    }

    test("jumpToSlide → currentSlideIndex = target") {
        val store = store()
        val body = (1..13).joinToString("\n") { "Line $it" } // 13 lines → 4 slides
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.jumpToSlide(2)
        (store.state.value as PresentationState.Lyrics).currentSlideIndex shouldBe 2
    }

    test("toggleBlank from Lyrics → Blank") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song()), defaultConfig)
        store.toggleBlank()
        store.state.value.shouldBeInstanceOf<PresentationState.Blank>()
    }

    test("toggleBlank from Blank → restores Lyrics at same slide index") {
        val store = store()
        val body = (1..9).joinToString("\n") { "Line $it" } // 9 lines → 3 slides
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.advance()
        store.advance()
        store.toggleBlank()
        store.toggleBlank()
        val state = store.state.value as PresentationState.Lyrics
        state.currentSlideIndex shouldBe 2
    }

    test("toggleFreeze from not-frozen → frozenDisplayIndex = currentSlideIndex") {
        val store = store()
        val body = (1..9).joinToString("\n") { "Line $it" }
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.advance()
        store.toggleFreeze()
        val state = store.state.value as PresentationState.Lyrics
        state.frozenDisplayIndex shouldBe 1
        state.frozen shouldBe true
    }

    test("toggleFreeze from frozen → frozenDisplayIndex = null") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song()), defaultConfig)
        store.toggleFreeze()
        store.toggleFreeze()
        val state = store.state.value as PresentationState.Lyrics
        state.frozenDisplayIndex shouldBe null
        state.frozen shouldBe false
    }

    test("frozen: advance increments currentSlideIndex but displaySlideIndex unchanged") {
        val store = store()
        val body = (1..9).joinToString("\n") { "Line $it" }
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.toggleFreeze()
        store.advance()
        val state = store.state.value as PresentationState.Lyrics
        state.currentSlideIndex shouldBe 1
        state.displaySlideIndex shouldBe 0
    }

    test("frozen: jumpToSlide updates currentSlideIndex but displaySlideIndex unchanged") {
        val store = store()
        val body = (1..9).joinToString("\n") { "Line $it" }
        store.loadSet(set("song-1"), mapOf("song-1" to song(sectionBody = body)), defaultConfig)
        store.toggleFreeze()
        store.jumpToSlide(2)
        val state = store.state.value as PresentationState.Lyrics
        state.currentSlideIndex shouldBe 2
        state.displaySlideIndex shouldBe 0
    }

    test("clear → Idle") {
        val store = store()
        store.loadSet(set("song-1"), mapOf("song-1" to song()), defaultConfig)
        store.clear()
        store.state.value.shouldBeInstanceOf<PresentationState.Idle>()
    }

    test("multi-song set: advance past last slide of song1 → first slide of song2") {
        val store = store()
        val s1 = song(id = "s1", sectionBody = "Only line")
        val s2 = song(id = "s2", sectionBody = "Song2 line")
        store.loadSet(set("s1", "s2"), mapOf("s1" to s1, "s2" to s2), defaultConfig)
        store.advance()
        val state = store.state.value as PresentationState.Lyrics
        state.currentSlideIndex shouldBe 1
        state.allSlides[1].sectionId shouldBe "s2-sec"
    }
})
