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
    override var keepCovers: Boolean = false,
    override var writeCovers: Boolean = true,
    override var trackTemplate: String = "%author_name% — %track_title%",
    override var outPath: String = "music/%author_name%/%album_title%",
    var ffmpegPath: String = ""
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
            Quality.Companion.fromInt(it)
        } ?: this.quality
        this.keepCovers = toml.getBoolean("keep_covers") ?: this.keepCovers
        this.outPath = toml.getString("out_path") ?: this.outPath
        this.sleep = toml.getLong("sleep")?.toInt() ?: this.sleep
        this.writeCovers = toml.getBoolean("write_covers") ?: this.writeCovers
        this.trackTemplate = toml.getString("track_template") ?: this.trackTemplate
        this.ffmpegPath = toml.getString("ffmpeg_path") ?: this.ffmpegPath
    }

    private fun getConfigMap(): Map<String, Any> = mapOf(
        "quality" to quality.num,
        "keep_covers" to keepCovers,
        "out_path" to outPath,
        "token" to token,
        "sleep" to sleep,
        "write_covers" to writeCovers,
        "track_template" to trackTemplate,
        "ffmpeg_path" to ffmpegPath
    )
}