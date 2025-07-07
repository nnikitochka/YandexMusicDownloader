package ru.nnedition.ymdownloader.api.config

import ru.nnedition.ymdownloader.api.objects.Quality

open class Config(
    var token: String,
    var quality: Quality,
    var keepCovers: Boolean,
    var outPath: String,
    var sleep: Boolean,
    var writeCovers: Boolean,
    var albumTemplate: String,
    var trackTemplate: String,
    var getOriginalCovers: Boolean,
    var writeLyrics: Boolean,
    var useFfmpegEnvVar: Boolean,
)