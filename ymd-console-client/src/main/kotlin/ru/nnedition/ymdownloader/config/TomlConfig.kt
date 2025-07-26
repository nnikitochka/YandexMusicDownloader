package ru.nnedition.ymdownloader.config

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import ru.nnedition.ymdownloader.api.config.Config
import ru.nnedition.ymdownloader.api.objects.Quality
import java.io.File

data class TomlConfig(private val fileName: String = "config.toml") : Config(
    token = "",
    quality = Quality.LOSSLESS,
    keepCovers =  true,
    outPath = "music/",
    sleep = false,
    writeCovers = true,
    albumTemplate = "{album_artist} - {album_title}",
    trackTemplate = "{track_num_pad}. {title}",
    getOriginalCovers = false,
    writeLyrics = true,
    ffmpegPath = "",
) {
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
        this.sleep = toml.getBoolean("sleep") ?: this.sleep
        this.writeCovers = toml.getBoolean("write_covers") ?: this.writeCovers
        this.albumTemplate = toml.getString("album_template") ?: this.albumTemplate
        this.trackTemplate = toml.getString("track_template") ?: this.trackTemplate
        this.getOriginalCovers = toml.getBoolean("get_original_covers") ?: this.getOriginalCovers
        this.writeLyrics = toml.getBoolean("write_lyrics") ?: this.writeLyrics
        this.ffmpegPath = toml.getString("ffmpeg_path") ?: this.ffmpegPath
    }

    private fun getConfigMap(): Map<String, Any> = mapOf(
        "quality" to quality.num,
        "keep_covers" to keepCovers,
        "out_path" to outPath,
        "token" to token,
        "sleep" to sleep,
        "write_covers" to writeCovers,
        "album_template" to albumTemplate,
        "track_template" to trackTemplate,
        "get_original_covers" to getOriginalCovers,
        "write_lyrics" to writeLyrics,
        "ffmpeg_path" to ffmpegPath
    )
}