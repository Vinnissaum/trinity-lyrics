package dev.trinitychurch.lyrics.importer

import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.UUID

class HolyricsParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class HolyricsSongParser {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    open fun parse(jsonText: String): List<Song> {
        val dtos = try {
            json.decodeFromString<List<HolyricsSong>>(jsonText)
        } catch (e: SerializationException) {
            throw HolyricsParseException("Failed to parse Holyrics JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw HolyricsParseException("Invalid Holyrics JSON structure: ${e.message}", e)
        }
        return dtos.map { dto -> dto.toSong() }
    }

    private fun HolyricsSong.toSong(): Song {
        val songId = UUID.randomUUID().toString()
        val sections = lyrics.paragraphs.mapIndexed { _, paragraph ->
            SongSection(
                id = UUID.randomUUID().toString(),
                songId = songId,
                label = paragraph.description.ifBlank { "Estrofe ${paragraph.number}" },
                type = SectionType.VERSE,
                body = paragraph.text,
                sortOrder = paragraph.number
            )
        }
        return Song(
            id = songId,
            title = title,
            artist = artist,
            sections = sections,
            source = "holyrics"
        )
    }
}
