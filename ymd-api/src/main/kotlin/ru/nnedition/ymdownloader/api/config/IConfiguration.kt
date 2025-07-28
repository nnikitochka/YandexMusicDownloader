package ru.nnedition.ymdownloader.api.config

import ru.nnedition.ymdownloader.api.objects.Quality

interface IConfiguration {
    /**
     * Токен для доступа к API Яндекс Музыки.
     */
    var token: String

    /**
     * Целевое качество треков.
     */
    var quality: Quality

    /**
     * Задержка в секундах между каждой обработкой трека, чтобы предотвратить потенциальное ограничение скорости.
     */
    var sleep: Int

    /**
     * Надо ли сохранять обложки альбомов и исполнителей?
     */
    var keepCovers: Boolean

    /**
     * Надо ли записывать обложки треков в теги треков?
     */
    var writeCovers: Boolean

    /**
     * Шаблон для имён файлов треков.
     */
    var trackTemplate: String

    /**
     * Путь, по которому будут сохраняться треки.
     */
    var outPath: String
}