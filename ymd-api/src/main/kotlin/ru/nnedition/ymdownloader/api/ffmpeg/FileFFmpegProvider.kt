package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File
import java.io.IOException

/**
 * Реализация [FFmpegProvider], которая использует локальный исполняемый файл ffmpeg.
 * @param ffmpegFile Файл исполняемого файла ffmpeg.
 */
open class FileFFmpegProvider(
    val ffmpegFile: File
) : FFmpegProvider {
    constructor(
        ffmpegPath: String
    ) : this(File(ffmpegPath))

    init {
        require(isFFmpeg()) { "FFmpeg не был найден по пути: ${ffmpegFile.path}." }
    }

    /**
     * @throws IOException Если произошла ошибка при выполнении ffmpeg.
     */
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

    /**
     * Проверяет, является ли указанный файл ffmpeg исполняемым и корректным.
     * @return true, если файл является корректным ffmpeg, иначе false.
     */
    fun isFFmpeg(): Boolean {
        if (!ffmpegFile.exists() || !ffmpegFile.canExecute()) return false

        return try {
            val process = Runtime.getRuntime().exec(arrayOf(ffmpegFile.path, "-version"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()

            process.waitFor() == 0 && output.contains("ffmpeg version")
        } catch (e: Exception) {
            false
        }
    }
}