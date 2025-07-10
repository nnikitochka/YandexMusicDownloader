package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File
import java.io.IOException

class LinuxFFmpegProvider : FFmpegProvider() {
    val ffmpegPath: String = "/usr/bin/ffmpeg"
    val ffmpegFile: File = File(ffmpegPath)

    init {
        require(ffmpegFile.exists()) {
            "FFmpeg не был найден по пути: $ffmpegPath. Установите FFmpeg и убедитесь, что он доступен в системе."
        }
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