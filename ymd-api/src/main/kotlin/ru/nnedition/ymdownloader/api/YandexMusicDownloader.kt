package ru.nnedition.ymdownloader.api

import nn.edition.yalogger.logger
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import ru.nnedition.ymdownloader.api.config.Config
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.Artist
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.api.utils.createDir
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class YandexMusicDownloader(
    val config: Config,
    val ymClient: YandexMusicClient,
    val ffmpeg: FfmpegProvider
) {
    constructor(config: Config, ffmpeg: FfmpegProvider) : this(config, YandexMusicClient.create(config.token), ffmpeg)

    val logger = logger(this::class)

    fun downloadArtist(artistId: Long, config: Config) {
        val artist = ymClient.getArtist(artistId)
        logger.info("Starting processing artist \"${artist.artist.name}\"")
        artist.albums.forEach { album ->
            downloadAlbum(album.id, config)
        }
    }

    fun downloadAlbum(albumId: Long, config: Config) {
        val album = ymClient.getAlbum(albumId)

        if (!album.available)
            throw Exception("Album \"$albumId\" not available")

        logger.info("Starting processing album \"${album.title}\"")

        album.tracks[0].forEach { track ->
            downloadTrack(track, album, album.artists[0], config)
        }
    }

    fun downloadAlbum(album: Album) {
        album.tracks[0].forEach { track ->
            downloadTrack(track, album, album.artists[0], config)
        }
    }

    fun downloadTrack(trackId: Long, config: Config) {
        val track = ymClient.getTrack(trackId)

        if (!track.available)
            throw Exception("Track \"$trackId\" not available")

        downloadTrack(track, track.albums[0], track.artists[0], config)
    }

    fun downloadTrack(
        track: Track, album: Album, publisher: Artist, config: Config
    ) = downloadTrack(track, album, ArtistMeta(publisher.id, publisher.name), config)

    fun downloadTrack(
        track: Track, album: Album, publisher: ArtistMeta, config: Config
    ) {
        if (!track.available) return

        logger.info("Starting processing track \"${track.title}\"")

        val info = ymClient.getFileInfo(track.id.toString(), config.quality.toString())

        val allPath = File(config.outPath).createDir()
        val artistPath = File(allPath, publisher.name).createDir()
        val albumPath = File(artistPath, album.title).createDir()

        val fileName = "${publisher.name} — ${track.title}"

        val finalFile = File(albumPath, "$fileName.flac")

        if (finalFile.exists()) {
            logger.info("Track \"$fileName\" already exists.")
            return
        }

        val outputFile = File(albumPath, "$fileName.mp4")

        val url = URL(info.url)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {

            FileOutputStream(outputFile).use { output ->
                output.write(decryptTrack(connection.inputStream.readAllBytes(), info.key))
            }

        } else {
            connection.disconnect()
            throw Exception("Ошибка при загрузке файла: ${connection.responseCode} ${connection.responseMessage}")
        }

        try {
            ffmpeg.mux(outputFile, finalFile)
            outputFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        try {
            val audioFile = AudioFileIO.read(finalFile)
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

            val coverFile = File(albumPath, "$fileName.jpeg")
            FileOutputStream(coverFile).use { output ->
                output.write(ymClient.getCoverData(track.coverUri, false, false, "300x300"))
            }

            val artwork = ArtworkFactory.createArtworkFromFile(coverFile)
            artwork.pictureType = 0
            artwork.description = "Album cover"

            tag.setField(artwork)

            audioFile.commit()

            coverFile.delete()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun decryptTrack(encData: ByteArray, key: String): ByteArray {
        val keyBytes = key.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        if (keyBytes.size != 16)
            throw IllegalArgumentException("Key must be 16 bytes")

        val nonce = ByteArray(16) { 0 }

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(nonce)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        return cipher.doFinal(encData)
    }
}