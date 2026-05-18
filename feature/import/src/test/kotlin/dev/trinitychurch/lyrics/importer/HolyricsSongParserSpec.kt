package dev.trinitychurch.lyrics.importer

import dev.trinitychurch.lyrics.domain.SectionType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

class HolyricsSongParserSpec : FreeSpec({

    val parser = HolyricsSongParser()

    val threeSongJson = """
        [
          {
            "id": 1,
            "title": "Song One",
            "artist": "Artist A",
            "lyrics": {
              "paragraphs": [
                {"number": 1, "description": "Verse 1", "text": "Line one"},
                {"number": 2, "description": "Chorus", "text": "Refrain here"}
              ]
            }
          },
          {
            "id": 2,
            "title": "Song Two",
            "artist": "Artist B",
            "lyrics": {
              "paragraphs": [
                {"number": 1, "description": "", "text": "Body text"}
              ]
            }
          },
          {
            "id": 3,
            "title": "Song Three",
            "artist": "",
            "lyrics": {
              "paragraphs": [
                {"number": 1, "description": "Estrofe 1", "text": "Some verse"}
              ]
            }
          }
        ]
    """.trimIndent()

    "valid JSON with 3 songs returns 3 Song objects" {
        val songs = parser.parse(threeSongJson)
        songs shouldHaveSize 3
    }

    "empty description → section label defaults to 'Estrofe N'" {
        val json = """
            [{"id": 1, "title": "T", "lyrics": {"paragraphs": [
                {"number": 1, "description": "", "text": "Body"}
            ]}}]
        """.trimIndent()
        val sections = parser.parse(json).first().sections
        sections.first().label shouldBe "Estrofe 1"
    }

    "non-empty description → section label uses it" {
        val json = """
            [{"id": 1, "title": "T", "lyrics": {"paragraphs": [
                {"number": 1, "description": "My Custom Label", "text": "Body"}
            ]}}]
        """.trimIndent()
        val sections = parser.parse(json).first().sections
        sections.first().label shouldBe "My Custom Label"
    }

    "paragraph text becomes SongSection.body" {
        val json = """
            [{"id": 1, "title": "T", "lyrics": {"paragraphs": [
                {"number": 1, "description": "", "text": "The body text here"}
            ]}}]
        """.trimIndent()
        parser.parse(json).first().sections.first().body shouldBe "The body text here"
    }

    "paragraph.number maps to SongSection.sortOrder" {
        val json = """
            [{"id": 1, "title": "T", "lyrics": {"paragraphs": [
                {"number": 5, "description": "", "text": "X"}
            ]}}]
        """.trimIndent()
        parser.parse(json).first().sections.first().sortOrder shouldBe 5
    }

    "all sections have SectionType.VERSE" {
        val songs = parser.parse(threeSongJson)
        songs.flatMap { it.sections }.forEach { section ->
            section.type shouldBe SectionType.VERSE
        }
    }

    "generated Song.id is a valid UUID" {
        val json = """
            [{"id": 999, "title": "T", "lyrics": {"paragraphs": []}}]
        """.trimIndent()
        val song = parser.parse(json).first()
        song.id shouldMatch Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    "empty paragraphs array → song imported with 0 sections" {
        val json = """
            [{"id": 1, "title": "T", "lyrics": {"paragraphs": []}}]
        """.trimIndent()
        parser.parse(json).first().sections.shouldBeEmpty()
    }

    "malformed JSON → throws HolyricsParseException not SerializationException" {
        shouldThrow<HolyricsParseException> {
            parser.parse("not-valid-json{{{")
        }
    }
})
