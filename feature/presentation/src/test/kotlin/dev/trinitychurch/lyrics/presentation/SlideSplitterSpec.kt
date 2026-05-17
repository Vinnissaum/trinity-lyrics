package dev.trinitychurch.lyrics.presentation

import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.SongSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SlideSplitterSpec : FunSpec({

    val defaultConfig = SlideConfig(maxLinesPerSlide = 4, maxCharsPerLine = 60)

    fun section(body: String, id: String = "sec-1") = SongSection(
        id = id, songId = "song-1", label = "Verso 1",
        type = SectionType.VERSE, body = body, sortOrder = 1
    )

    test("empty body returns empty list") {
        SlideSplitter.split(section(""), defaultConfig).shouldBeEmpty()
    }

    test("single line under maxCharsPerLine produces one slide with one line") {
        val result = SlideSplitter.split(section("Aleluia"), defaultConfig)
        result shouldHaveSize 1
        result[0].lines shouldBe listOf("Aleluia")
    }

    test("exactly maxLinesPerSlide lines produces one slide") {
        val body = "L1\nL2\nL3\nL4"
        val result = SlideSplitter.split(section(body), defaultConfig)
        result shouldHaveSize 1
        result[0].lines shouldHaveSize 4
    }

    test("maxLinesPerSlide + 1 lines produces two slides") {
        val body = "L1\nL2\nL3\nL4\nL5"
        val result = SlideSplitter.split(section(body), defaultConfig)
        result shouldHaveSize 2
        result[0].lines shouldHaveSize 4
        result[1].lines shouldHaveSize 1
    }

    test("line over maxCharsPerLine is word-wrapped to multiple display lines") {
        val config = SlideConfig(maxLinesPerSlide = 4, maxCharsPerLine = 10)
        val result = SlideSplitter.split(section("Hello World Today"), config)
        result shouldHaveSize 1
        result[0].lines shouldHaveSize 3
    }

    test("line with no spaces over limit is hard-broken at maxCharsPerLine") {
        val config = SlideConfig(maxLinesPerSlide = 4, maxCharsPerLine = 10)
        val result = SlideSplitter.split(section("ABCDEFGHIJKLMNOP"), config)
        result shouldHaveSize 1
        result[0].lines shouldBe listOf("ABCDEFGHIJ", "KLMNOP")
    }

    test("multiple sections split independently with correct metadata") {
        val s1 = section("Line A", id = "s1")
        val s2 = section("Line B", id = "s2")
        val r1 = SlideSplitter.split(s1, defaultConfig)
        val r2 = SlideSplitter.split(s2, defaultConfig)
        r1 shouldHaveSize 1
        r2 shouldHaveSize 1
        r1[0].lines shouldBe listOf("Line A")
        r2[0].lines shouldBe listOf("Line B")
        r1[0].sectionId shouldBe "s1"
        r2[0].sectionId shouldBe "s2"
    }

    test("body with blank lines discards blank lines") {
        val body = "Line1\n\nLine2\n\nLine3"
        val result = SlideSplitter.split(section(body), defaultConfig)
        result shouldHaveSize 1
        result[0].lines shouldHaveSize 3
    }

    test("slideIndexInSection and totalSlidesInSection are correct for multi-slide section") {
        val body = "L1\nL2\nL3\nL4\nL5"
        val result = SlideSplitter.split(section(body), defaultConfig)
        result[0].slideIndexInSection shouldBe 0
        result[0].totalSlidesInSection shouldBe 2
        result[1].slideIndexInSection shouldBe 1
        result[1].totalSlidesInSection shouldBe 2
    }

    test("blank-only body returns empty list") {
        SlideSplitter.split(section("   \n  \n   "), defaultConfig).shouldBeEmpty()
    }
})
