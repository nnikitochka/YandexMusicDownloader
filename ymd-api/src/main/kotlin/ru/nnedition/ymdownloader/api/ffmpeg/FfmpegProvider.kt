package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

/**
 * Интерфейс для работы с ffmpeg.
 * Предоставляет методы для выполнения mux (перекодирования) аудиофайлов.
 */
interface FfmpegProvider {
    /**
     * Выполняет извлечение аудио-дорожки из видео файла с использованием ffmpeg.
     * @param inPath Путь к входному аудиофайлу.
     * @param outPath Путь к выходному аудиофайлу.
     */
    fun mux(inPath: File, outPath: File)
}