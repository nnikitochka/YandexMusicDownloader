package ru.nnedition.ymdownloader.api.ffmpeg

import java.io.File

class LinuxFFmpegProvider(
    ffmpegFile: File
) : FileFFmpegProvider(ffmpegFile) {
    constructor(
        ffmpegPath: String = "/usr/bin/ffmpeg"
    ) : this(File(ffmpegPath))
}