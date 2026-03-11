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
     * Путь, по которому будут сохраняться треки.
     */
    var outPath: String

    /**
     * Шаблон для имён файлов треков.
     */
    var trackTemplate: String

    /**
     * Надо ли записывать обложки треков в теги треков?
     */
    var writeTrackCovers: Boolean

    /**
     * Надо ли сохранять обложки альбомов?
     */
    var writeAlbumCovers: Boolean

    /**
     * Название файла сохранённых обложек альбомов.
     */
    var albumCoverFileName: String

    /**
     * Символы, которые будут заменены при сохранении треков.
     * Ключ - символ для замены, значение - символ, на который будет происходить замена.
     */
    var fileReplacements: Map<String, String>
}