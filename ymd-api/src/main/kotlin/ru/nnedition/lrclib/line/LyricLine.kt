package ru.nnedition.lrclib.line

data class LyricLine(
    var timing: Long,
    var text: String,
) : Line {
    fun serialize(): String {
        val minutes = (timing / 60000).toInt()
        val seconds = ((timing / 1000) % 60).toInt()
        val frac = (timing % 1000).toInt()
        val fracStr = if (frac < 100) "%02d".format(frac) else "%03d".format(frac)
        return "[%02d:%02d.%s] %s".format(minutes, seconds, fracStr, text)
    }
}