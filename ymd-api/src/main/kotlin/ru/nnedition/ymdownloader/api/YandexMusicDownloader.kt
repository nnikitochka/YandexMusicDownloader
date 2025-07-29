package ru.nnedition.ymdownloader.api

import nn.edition.yalogger.logger
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.Artist
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta
import ru.nnedition.ymdownloader.api.utils.AudioTagWriter
import ru.nnedition.ymdownloader.api.utils.createDir
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class YandexMusicDownloader(
    val config: IConfiguration,
    val ymClient: YandexMusicClient,
    val ffmpeg: FfmpegProvider
) {
    constructor(config: IConfiguration, ffmpeg: FfmpegProvider) : this(config, YandexMusicClient.create(config.token), ffmpeg)

    val logger = logger(this::class)

    fun downloadArtist(artistId: Long, config: IConfiguration) {
        val artist = this.ymClient.getArtist(artistId)

        this.logger.info("Обработка артиста \"${artist.artist.name}\"...")

        artist.albums.forEach { album ->
            downloadAlbum(album.id, config)
        }
    }

    fun downloadAlbum(albumId: Long, config: IConfiguration) {
        downloadAlbum(this.ymClient.getAlbum(albumId), config)
    }

    fun downloadAlbum(album: Album, config: IConfiguration) {
        if (!album.available)
            throw Exception("Альбом \"${album.title}\" недоступен")

        this.logger.info("Обработка альбома \"${album.title}\"...")

        album.tracks[0].forEach { track ->
            downloadTrack(track, album, album.artists[0], config)
        }
    }

    fun downloadTrack(trackId: Long, config: IConfiguration) {
        val track = this.ymClient.getTrack(trackId)

        if (!track.available)
            throw Exception("Трек \"${track.title}\" недоступен")

        downloadTrack(track, track.albums[0], track.artists[0], config)
    }

    fun downloadTrack(
        track: Track, album: Album, publisher: Artist, config: IConfiguration
    ) = downloadTrack(track, album, ArtistMeta(publisher.id, publisher.name), config)

    fun downloadTrack(
        track: Track, album: Album, publisher: ArtistMeta, config: IConfiguration
    ) {
        if (!track.available)
            throw Exception("Трек \"${track.title}\" недоступен")

        this.logger.info("Обработка трека \"${track.title}\"...")

        val info = this.ymClient.getFileInfo(track.id.toString(), config.quality.toString())

        val format = if (info.codec.contains("-")) {
            val parts = info.codec.split("-")
            if (parts[0] == "he") parts[1]
            else parts[0]
        }
        else info.codec

        val path = File(config.outPath.parsePathPlaceholders(publisher, album, track)).createDir()

        val fileName = config.trackTemplate.parsePlaceholders(publisher, album, track)

        var finalFile = File(path, "$fileName.$format")

        if (finalFile.exists()) {
            logger.info("Трек \"$fileName\" уже существует.")
            return
        }

        val outputFile = File(path, "$fileName.mp4")

        try {
            downloadTrack(info, outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            return
        }

        try {
            this.ffmpeg.mux(outputFile, finalFile)
            outputFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            finalFile.delete()
            return
        }

        if (format == "aac") {
            val interimFile = File(path, "$fileName.flac")

            if (interimFile.exists()) {
                logger.info("Трек \"$fileName\" уже существует.")
                finalFile.delete()
                return
            }

            this.ffmpeg.convert(finalFile, interimFile)
            finalFile.delete()
            finalFile = interimFile
        }

        val cover = this.ymClient.getCoverData(track.coverUri, false, false, "300x300")

        AudioTagWriter.write(
            finalFile,
            cover,
            track,
            album
        )
    }

    fun downloadTrack(info: DownloadInfo, outputFile: File) {
        val url = URL(info.url)
        val connect = url.openConnection() as HttpURLConnection
        connect.requestMethod = "GET"
        connect.connectTimeout = 20_000
        connect.readTimeout = 20_000

        if (connect.responseCode == HttpURLConnection.HTTP_OK) {
            FileOutputStream(outputFile).use { output ->
                output.write(decryptTrack(connect.inputStream.readAllBytes(), info.key))
            }
            connect.disconnect()
        } else {
            connect.disconnect()
            throw Exception("Ошибка при загрузке файла: ${connect.responseCode} ${connect.responseMessage}")
        }
    }

    companion object {
        fun String.parsePlaceholders(
            publisher: ArtistMeta,
            album: Album,
            track: Track,
            transform: (String) -> String = { it.fixPath() }
        ): String = this.replace("%author_name%", transform(publisher.name))
                .replace("%album_title%", transform(album.title))
                .replace("%track_title%", transform(track.title))

        fun String.parsePathPlaceholders(publisher: ArtistMeta, album: Album, track: Track): String =
            parsePlaceholders(publisher, album, track) { it.fixPath() }

        private fun String.fixPath() = replace("/", "\\")

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
}