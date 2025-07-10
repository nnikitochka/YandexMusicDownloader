package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

/**
 * Реализация провайдера FFmpeg для Linux.
 * Использует стандартный путь к ffmpeg `/usr/bin/ffmpeg`.
 *
 * @param ffmpegFile Файл ffmpeg.
 */
class LinuxFfmpegProvider(
    ffmpegFile: File
) : FileFfmpegProvider(ffmpegFile) {
    constructor(
        ffmpegPath: String = "/usr/bin/ffmpeg"
    ) : this(File(ffmpegPath))
}