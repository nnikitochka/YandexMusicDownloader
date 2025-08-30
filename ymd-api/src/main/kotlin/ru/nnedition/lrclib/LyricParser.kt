package ru.nnedition.lrclib

import ru.nnedition.lrclib.line.BlankLine
import ru.nnedition.lrclib.line.Line
import ru.nnedition.lrclib.line.LyricLine
import ru.nnedition.lrclib.line.MetaLine
import ru.nnedition.lrclib.line.TextLine
import java.util.regex.Pattern

object LyricParser {
    private val metaPattern = Pattern.compile("^\\[(\\w+):(.*)]$")
    private val lyricPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})(\\.\\d{2,3})?]")
    private val lyricRegex = this.lyricPattern.toRegex()

    fun parse(lines: List<String>) = Lyric(lines.mapIndexed { i, line -> readLine(line) }.toMutableList())

    fun readLine(line: String): Line {
        if (line.isBlank()) return BlankLine()

        val lyricMatcher = this.lyricPattern.matcher(line)
        if (lyricMatcher.find()) {
            val minutes = lyricMatcher.group(1).toInt()
            lyricMatcher.group(2).toInt().takeIf { it < 60 }?.let { seconds ->
                val milliSeconds = lyricMatcher.group(3)?.substring(1)?.toIntOrNull() ?: 0

                val text = line.replace(this.lyricRegex, "").trim()

                val timing = (minutes * 60 + seconds) * 1000L + milliSeconds

                return LyricLine(timing, text)
            }
        }

        val metaMatcher = metaPattern.matcher(line)
        if (metaMatcher.matches()) {
            val key = metaMatcher.group(1)
            val value = metaMatcher.group(2)
            return MetaLine.fromKey(key, value) ?: MetaLine(key, value)
        }

        return TextLine(line)
    }
}