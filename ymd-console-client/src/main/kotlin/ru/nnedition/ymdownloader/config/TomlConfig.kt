package ru.nnedition.ymdownloader.config

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.objects.Quality
import java.io.File

data class TomlConfig(
    private val fileName: String = "config.toml",

    override var token: String = "",
    override var quality: Quality = Quality.LOSSLESS,
    override var sleep: Int = 5,
    var ffmpegPath: String = "",
    override var outPath: String = "music/%author_name%/%year% - %album_title%",
    override var trackTemplate: String = "%author_name% — %track_title%",
    override var writeTrackCovers: Boolean = false,
    override var writeAlbumCovers: Boolean = true,
    override var fileReplacements: Map<String, String> = emptyMap(),
) : IConfiguration {
    init {
        val configFile = File(fileName)
        if (!configFile.exists()) {
            save()
        } else {
            load()
            save()
        }
    }

    fun save() {
        TomlWriter().write(getConfigMap(), File(fileName))
    }

    private fun load() {
        val toml = Toml().read(File(fileName))

        this.token = toml.getString("token") ?: this.token
        this.quality = toml.getLong("quality")?.toInt()?.let {
            Quality.fromInt(it)
        } ?: this.quality
        this.sleep = toml.getLong("sleep")?.toInt() ?: this.sleep
        this.ffmpegPath = toml.getString("ffmpeg_path") ?: this.ffmpegPath
        this.outPath = toml.getString("out_path") ?: this.outPath
        this.trackTemplate = toml.getString("track_template") ?: this.trackTemplate
        this.writeTrackCovers = toml.getBoolean("write_track_covers") ?: this.writeTrackCovers
        this.writeAlbumCovers = toml.getBoolean("write_album_covers") ?: this.writeAlbumCovers

        toml.getTable("file_replacements").toMap()?.also { replacementsRaw ->
            val replacements = linkedMapOf<String, String>()
            replacementsRaw.forEach { (key, value) ->
                replacements[key.removePrefix("\"").removeSuffix("\"")] = value.toString()
            }
            fileReplacements = replacements
        }
    }

    private fun getConfigMap(): Map<String, Any> = mapOf(
        "token" to token,
        "quality" to quality.num,
        "sleep" to sleep,
        "ffmpeg_path" to ffmpegPath,
        "out_path" to outPath,
        "track_template" to trackTemplate,
        "write_track_covers" to writeTrackCovers,
        "write_album_covers" to writeAlbumCovers,
        "file_replacements" to fileReplacements,
    )
}