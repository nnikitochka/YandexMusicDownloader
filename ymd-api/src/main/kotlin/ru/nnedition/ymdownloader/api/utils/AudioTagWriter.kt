package ru.nnedition.ymdownloader.api.utils

import nn.edition.yalogger.logger
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.FlacInfoReader
import org.jaudiotagger.audio.mp3.MP3FileReader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import ru.nnedition.ymdownloader.api.objects.Track
import java.io.File
import java.util.logging.Level

object AudioTagWriter {
    val logger = logger(this::class)

    var enableLogs: Boolean = false
        set(value) {
            FlacInfoReader.logger.level = Level.OFF
            MP3FileReader.logger.level = Level.OFF
            field = value
        }
    init {
        enableLogs = false
    }

    fun write(
        file: File,
        track: Track,
        artists: List<String>,
        genre: String?,
        cover: ByteArray?,
    ) {
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, track.title)
            track.version?.let { version ->
                tag.setField(FieldKey.VERSION, version)
            }

            val album = track.album
            val albumTitle = buildString {
                append(album.title)
                album.version?.let { append(" (${it})") }
            }
            tag.setField(FieldKey.ALBUM, albumTitle)
            tag.setField(FieldKey.YEAR, album.releaseDateFormatted ?: album.year.toString())

            while (tag.hasField(FieldKey.ARTIST)) {
                tag.deleteField(FieldKey.ARTIST)
            }
            artists.forEach { artist ->
                tag.addField(FieldKey.ARTIST, artist)
            }
            tag.setField(FieldKey.ALBUM_ARTIST, album.publisher.name)

            tag.setField(FieldKey.TRACK, track.num)
            tag.setField(FieldKey.TRACK_TOTAL, album.tracks.size.toString())

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