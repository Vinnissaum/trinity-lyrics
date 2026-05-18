package dev.trinitychurch.lyrics.importer

import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.SongSection
import java.util.UUID

object PlainTextSongParser {

    private val HEADER_REGEX = Regex("""^\[(.+)]$""")

    private val TYPE_MAP: Map<Regex, SectionType> = mapOf(
        Regex("verse|verso|estrofe|^v$", RegexOption.IGNORE_CASE) to SectionType.VERSE,
        Regex("chorus|refrão|refrao|^c$", RegexOption.IGNORE_CASE) to SectionType.CHORUS,
        Regex("bridge|ponte|^b$", RegexOption.IGNORE_CASE) to SectionType.BRIDGE,
        Regex("pre-chorus|pre-refrão|pre-refrao", RegexOption.IGNORE_CASE) to SectionType.PRE_CHORUS,
        Regex("outro|final", RegexOption.IGNORE_CASE) to SectionType.OUTRO,
        Regex("interlude|interlúdio|interludio", RegexOption.IGNORE_CASE) to SectionType.INTERLUDE,
        Regex("tag|coda", RegexOption.IGNORE_CASE) to SectionType.TAG,
    )

    fun parse(text: String): List<SongSection> {
        if (text.isBlank()) return emptyList()

        val lines = text.lines()
        val sections = mutableListOf<SongSection>()
        var currentLabel: String? = null
        var currentType = SectionType.VERSE
        var bodyLines = mutableListOf<String>()
        var sortOrder = 0

        fun flush() {
            if (currentLabel != null || bodyLines.isNotEmpty()) {
                val label = currentLabel ?: ""
                val body = bodyLines.joinToString("\n")
                if (body.isNotEmpty() || currentLabel != null) {
                    sections.add(
                        SongSection(
                            id = UUID.randomUUID().toString(),
                            songId = "",
                            label = label,
                            type = currentType,
                            body = body,
                            sortOrder = sortOrder++
                        )
                    )
                }
            }
        }

        var hasHeaders = false
        for (line in lines) {
            val headerMatch = HEADER_REGEX.find(line.trim())
            if (headerMatch != null) {
                hasHeaders = true
                flush()
                currentLabel = headerMatch.groupValues[1]
                currentType = resolveType(currentLabel!!)
                bodyLines = mutableListOf()
            } else {
                bodyLines.add(line)
            }
        }

        if (!hasHeaders) {
            val body = lines.joinToString("\n")
            return listOf(
                SongSection(
                    id = UUID.randomUUID().toString(),
                    songId = "",
                    label = "",
                    type = SectionType.VERSE,
                    body = body,
                    sortOrder = 0
                )
            )
        }

        flush()
        return sections
    }

    private fun resolveType(header: String): SectionType {
        val keyword = header.split(" ").first()
        for ((pattern, type) in TYPE_MAP) {
            if (pattern.matches(keyword)) return type
        }
        return SectionType.VERSE
    }
}
