package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

abstract class FFmpegProvider {
    abstract fun mux(inPath: File, outPath: File)
}