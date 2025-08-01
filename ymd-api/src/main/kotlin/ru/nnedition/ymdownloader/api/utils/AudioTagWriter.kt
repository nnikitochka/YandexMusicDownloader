package ru.nnedition.ymdownloader.api.utils

import nn.edition.yalogger.logger
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.FlacInfoReader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
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
        cover: ByteArray?,
        track: Track,
        album: Album,
        genre: String?,
    ) {
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, track.title)
            tag.setField(FieldKey.ALBUM, album.title)
            tag.setField(FieldKey.YEAR, album.year.toString())
            tag.setField(FieldKey.ARTIST, track.artists.joinToString(", ") { it.name })

            genre?.let {
                tag.setField(FieldKey.GENRE, it)
            }

            cover?.let {
                val artwork = StandardArtwork()
                artwork.binaryData = cover
                artwork.pictureType = 3
                artwork.description = "Обложка альбома"

                tag.setField(artwork)
            }

            audioFile.commit()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}