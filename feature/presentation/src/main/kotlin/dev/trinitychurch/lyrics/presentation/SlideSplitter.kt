package dev.trinitychurch.lyrics.presentation

import dev.trinitychurch.lyrics.domain.Slide
import dev.trinitychurch.lyrics.domain.SlideConfig
import dev.trinitychurch.lyrics.domain.SongSection

object SlideSplitter {

    fun split(section: SongSection, config: SlideConfig): List<Slide> {
        if (section.body.isBlank()) return emptyList()

        val displayLines = section.body.split("\n")
            .filterNot { it.isBlank() }
            .flatMap { wordWrap(it.trim(), config.maxCharsPerLine) }

        if (displayLines.isEmpty()) return emptyList()

        val slideLines = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (line in displayLines) {
            current.add(line)
            if (current.size == config.maxLinesPerSlide) {
                slideLines.add(current.toList())
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) slideLines.add(current.toList())

        val total = slideLines.size
        return slideLines.mapIndexed { index, lines ->
            Slide(
                sectionId = section.id,
                sectionLabel = section.label,
                sectionType = section.type,
                lines = lines,
                slideIndexInSection = index,
                totalSlidesInSection = total
            )
        }
    }

    private fun wordWrap(line: String, maxChars: Int): List<String> {
        if (line.length <= maxChars) return listOf(line)

        val result = mutableListOf<String>()
        var current = ""

        for (word in line.split(" ")) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length <= maxChars) {
                current = candidate
            } else {
                if (current.isNotEmpty()) {
                    result.add(current)
                    current = ""
                }
                var remaining = word
                while (remaining.length > maxChars) {
                    result.add(remaining.substring(0, maxChars))
                    remaining = remaining.substring(maxChars)
                }
                current = remaining
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }
}
