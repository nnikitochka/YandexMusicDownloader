package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File
import java.io.IOException

open class FileFFmpegProvider(
    val ffmpegFile: File
) : FFmpegProvider {
    constructor(
        ffmpegPath: String
    ) : this(File(ffmpegPath))

    init {
        require(ffmpegFile.exists()) { "FFmpeg не был найден по пути: ${ffmpegFile.path}." }
    }

    override fun mux(inPath: File, outPath: File) {
        val process = ProcessBuilder(
            ffmpegFile.absolutePath,
            "-i",
            inPath.absolutePath,
            "-c:a",
            "copy",
            outPath.absolutePath
        ).start()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorOutput = process.errorStream.bufferedReader().readText()
            throw IOException("ffmpeg failed: $errorOutput")
        }
    }
}