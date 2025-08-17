package ru.nnedition.ymdownloader.api.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File

object GenreTranslator {
    private val gson = Gson()
    private val gsonFormatter = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeSpecialFloatingPointValues()
        .create()

    fun formatJson(input: String): String = gsonFormatter.toJson(JsonParser.parseString(input))

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

        this.translationsFile.writeText(formatJson(json))
    }

    fun saveTranslation(translation: Pair<String, String>) =
        saveTranslation(translation.first, translation.second)
    fun saveTranslation(genre: String, translation: String) {
        genres[genre] = translation
    }

    fun translate(genre: String) = this.genres[genre]
}