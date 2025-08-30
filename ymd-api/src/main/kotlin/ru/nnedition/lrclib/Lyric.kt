package ru.nnedition.lrclib

import ru.nnedition.lrclib.line.Line
import ru.nnedition.lrclib.line.LyricLine
import ru.nnedition.lrclib.line.MetaLine
import java.io.File

class Lyric(
    lines: MutableList<Line>
) {
    var lyric: MutableList<LyricLine> = lines.filterIsInstance<LyricLine>().sortedBy { it.timing }.toMutableList()
    var meta: LyricMeta = LyricMeta().also { meta ->
        lines.forEach { line ->
            when (line) {
                is MetaLine.Artists -> meta.artists = line.value.split(", ").toMutableList()
                is MetaLine.Title -> meta.title = line.value
                is MetaLine.Album -> meta.album = line.value
                is MetaLine.Writers -> meta.writers = line.value.split(", ").toMutableList()
                is MetaLine.Offset -> meta.offset = line.value.toLongOrNull()
                is MetaLine -> meta.other[line.key] = line.value
            }
        }
    }

    fun save(file: File) {
        val fileLines = mutableListOf<String>()

        this.meta.artists.takeIf { it.isNotEmpty() }?.let {
            fileLines.add("[ar:${it.joinToString(", ")}]")
        }
        this.meta.title?.let { fileLines.add("[ti:$it]") }
        this.meta.album?.let { fileLines.add("[al:$it]") }
        this.meta.writers.takeIf { it.isNotEmpty() }?.let {
            fileLines.add("[by:${it.joinToString(", ")}]")
        }
        this.meta.offset?.let { fileLines.add("[offset:$it]") }
        this.meta.other.forEach { fileLines.add("[${it.key}:${it.value}]") }

        fileLines.add("")

        this.lyric.sortedBy { it.timing }.forEach { fileLines.add(it.serialize()) }

        file.writeText(fileLines.joinToString("\n"))
    }

    companion object {
        fun load(file: File): Lyric {
            require(file.exists()) { "File ${file.absolutePath} does not exist." }

            val lines = file.readLines()

            return LyricParser.parse(lines)
        }
    }
}