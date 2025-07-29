package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File
import java.io.IOException

/**
 * Реализация [FfmpegProvider], которая использует локальный исполняемый файл ffmpeg.
 * @param ffmpegFile Файл исполняемого файла ffmpeg.
 */
open class FileFfmpegProvider(
    val ffmpegFile: File
) : FfmpegProvider {
    constructor(
        ffmpegPath: String
    ) : this(File(ffmpegPath))

    init {
        require(isFFmpeg()) { "FFmpeg не был найден по пути: ${ffmpegFile.path}." }
    }

    /**
     * @throws IllegalArgumentException Если входной и выходной файлы одинаковые.
     * @throws IOException Если произошла ошибка при выполнении ffmpeg.
     */
    override fun mux(input: File, output: File) {
        require(input.absolutePath != output.absolutePath) {
            "Входной и выходной файлы должны отличаться!"
        }

        val process = ProcessBuilder(
            ffmpegFile.absolutePath,
            "-i",
            input.absolutePath,
            "-c:a",
            "copy",
            output.absolutePath
        ).start()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorOutput = process.errorStream.bufferedReader().readText()
            throw IOException("ffmpeg failed: $errorOutput")
        }
    }

    /**
     * @throws IllegalArgumentException Если входной и выходной файлы одинаковые.
     * @throws IOException Если произошла ошибка при выполнении ffmpeg.
     */
    override fun convert(input: File, output: File) {
        require(input.absolutePath != output.absolutePath) {
            "Входной и выходной файлы должны отличаться!"
        }

        val process = ProcessBuilder(
            ffmpegFile.absolutePath,
            "-i",
            input.absolutePath,
            output.absolutePath
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
        if (!this.ffmpegFile.exists() || !this.ffmpegFile.canExecute()) return false

        return try {
            val process = Runtime.getRuntime().exec(arrayOf(this.ffmpegFile.path, "-version"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()

            process.waitFor() == 0 && output.contains("ffmpeg version")
        } catch (e: Exception) {
            false
        }
    }
}