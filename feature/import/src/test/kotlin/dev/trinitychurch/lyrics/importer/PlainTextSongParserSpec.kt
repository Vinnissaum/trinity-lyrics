package dev.trinitychurch.lyrics.importer

import dev.trinitychurch.lyrics.domain.SectionType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PlainTextSongParserSpec : FreeSpec({

    "[Verse 1] header with body lines → one VERSE section with joined body" {
        val sections = PlainTextSongParser.parse("[Verse 1]\nLine1\nLine2")
        sections shouldHaveSize 1
        sections.first().type shouldBe SectionType.VERSE
        sections.first().body shouldBe "Line1\nLine2"
    }

    "[Refrão] header → one CHORUS section" {
        val sections = PlainTextSongParser.parse("[Refrão]\nHaleluia")
        sections shouldHaveSize 1
        sections.first().type shouldBe SectionType.CHORUS
    }

    "multiple headers → multiple sections with correct types" {
        val input = "[Verse 1]\nVerse body\n[Chorus]\nChorus body\n[Bridge]\nBridge body"
        val sections = PlainTextSongParser.parse(input)
        sections shouldHaveSize 3
        sections[0].type shouldBe SectionType.VERSE
        sections[1].type shouldBe SectionType.CHORUS
        sections[2].type shouldBe SectionType.BRIDGE
    }

    "no headers → one VERSE section with all text" {
        val sections = PlainTextSongParser.parse("Just some lyrics\nwithout headers")
        sections shouldHaveSize 1
        sections.first().type shouldBe SectionType.VERSE
        sections.first().body shouldBe "Just some lyrics\nwithout headers"
    }

    "empty input → empty list" {
        PlainTextSongParser.parse("").shouldBeEmpty()
        PlainTextSongParser.parse("   ").shouldBeEmpty()
    }

    "unknown header [Intro] → VERSE (default fallback)" {
        val sections = PlainTextSongParser.parse("[Intro]\nSome intro lines")
        sections shouldHaveSize 1
        sections.first().type shouldBe SectionType.VERSE
    }

    "section body lines preserve newlines" {
        val input = "[Verse 1]\nFirst line\nSecond line\nThird line"
        val body = PlainTextSongParser.parse(input).first().body
        body shouldBe "First line\nSecond line\nThird line"
    }
})
