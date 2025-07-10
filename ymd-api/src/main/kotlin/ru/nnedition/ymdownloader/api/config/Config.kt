package ru.nnedition.ymdownloader.api.config

import ru.nnedition.ymdownloader.api.objects.Quality

open class Config(
    // Токен для доступа к API Яндекс Музыки
    open var token: String,
    // Качество скачиваемых треков
    open var quality: Quality,
    // Сохранять ли обложки альбомов и исполнителей?
    open var keepCovers: Boolean,
    // Сохранять ли обложки треков?
    open var writeCovers: Boolean,
    // Путь, по которому будут сохраняться треки
    open var outPath: String,
    // Задержка между каждой обработкой трека, чтобы предотвратить потенциальное ограничение скорости.
    open var sleep: Boolean,
    open var albumTemplate: String,
    open var trackTemplate: String,
    open var getOriginalCovers: Boolean,
    open var writeLyrics: Boolean,
    open var ffmpegPath: String,
)