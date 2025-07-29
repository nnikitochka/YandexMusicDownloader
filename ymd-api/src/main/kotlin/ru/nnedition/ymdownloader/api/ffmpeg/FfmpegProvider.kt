package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

/**
 * Интерфейс для работы с ffmpeg.
 * Предоставляет методы для выполнения mux (перекодирования) аудиофайлов.
 */
interface FfmpegProvider {
    /**
     * Выполняет извлечение аудио-дорожки из видео файла с использованием ffmpeg.
     * @param input Входной видеофайл.
     * @param output Выходной аудиофайл.
     */
    fun mux(input: File, output: File)

    /**
     * Выполняет конвертацию с использованием ffmpeg.
     * @param input Входной аудиофайл.
     * @param output Выходной аудиофайл.
     */
    fun convert(input: File, output: File)
}