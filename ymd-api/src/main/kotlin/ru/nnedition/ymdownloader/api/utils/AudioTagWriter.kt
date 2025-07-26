package ru.nnedition.ymdownloader.api.utils

import nn.edition.yalogger.logger
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.FlacInfoReader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import java.io.File
import java.util.logging.Level

object AudioTagWriter {
    val logger = logger(this::class)

    var enableLogs: Boolean = false
        set(value) {
            FlacInfoReader.logger.level = Level.OFF
            field = value
        }
    init {
        enableLogs = false
    }

    fun write(
        file: File,
        coverFile: File?,
        track: Track,
        album: Album,
    ) {
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, track.title)
            tag.setField(FieldKey.ALBUM, album.title)
            tag.setField(FieldKey.YEAR, album.year.toString())
            tag.setField(FieldKey.ARTIST, album.artists.joinToString(", ") { it.name })

            val genre = track.genre ?: album.genre
            val translatedGenre = GenreTranslator.translate(genre) ?: let {
                logger.warn("Найден неизвестный жанр: \"$genre\"")
                genre
            }

            tag.setField(FieldKey.GENRE, translatedGenre)

            coverFile?.let {
                val artwork = ArtworkFactory.createArtworkFromFile(coverFile)
                artwork.pictureType = 0
                artwork.description = "Album cover"

                tag.setField(artwork)
            }

            audioFile.commit()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}