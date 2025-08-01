package ru.nnedition.ymdownloader.api.utils

import com.google.gson.Gson
import java.io.File

object GenreTranslator {
    private val gson = Gson()

    var translationsFile = File("genres.json")
    private val genres: MutableMap<String, String> = HashMap()

    fun loadTranslations() {
        if (!this.translationsFile.exists()) {
            val input = this.javaClass.classLoader.getResourceAsStream("genres.json")

            require(input != null) { "genres.json не найден!" }

            this.translationsFile.writeBytes(input.readBytes())
        }

        val translations = gson.fromJson(this.translationsFile.readText(), Map::class.java)
            ?.map { it.key.toString() to it.value.toString() }?.toMap()

        require(translations != null) { "Ошибка чтения файла!" }

        this.genres.putAll(translations)
    }

    fun foldTranslations() {
        if (!this.translationsFile.exists()) {
            val input = this.javaClass.classLoader.getResourceAsStream("genres.json")

            require(input != null) { "genres.json не найден!" }

            this.translationsFile.writeBytes(input.readBytes())
            return
        }

        val json = this.gson.toJson(this.genres)

        this.translationsFile.writeText(json)
    }

    fun saveTranslation(translation: Pair<String, String>) {
        genres.put(translation.first, translation.second)
    }

    fun translate(genre: String) = this.genres[genre]
}