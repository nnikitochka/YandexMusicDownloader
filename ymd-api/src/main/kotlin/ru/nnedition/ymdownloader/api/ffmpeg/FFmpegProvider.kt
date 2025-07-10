package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

interface FFmpegProvider {
    fun mux(inPath: File, outPath: File)
}