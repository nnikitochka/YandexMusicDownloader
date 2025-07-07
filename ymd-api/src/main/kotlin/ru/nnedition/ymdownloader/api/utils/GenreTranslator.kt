package ru.nnedition.ymdownloader.api.utils

object GenreTranslator {
    private val genres = mapOf(
        "alternative" to "Альтернатива",
        "pop" to "Поп",
        "ruspop" to "Русский поп",
        "rock" to "Рок",
        "rusrock" to "Русский рок",
        "metal" to "Метал",
        "jazz" to "Джаз",
        "hip-hop" to "Хип-хоп",
        "electronic" to "Электронная музыка",
        "classical" to "Классика",
        "country" to "Кантри",
        "blues" to "Блюз",
        "videogame" to "Видеоигры",
        "classicmetal" to "Классический метал",
    )

    fun translate(genre: String) = genres[genre]
}