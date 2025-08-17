package ru.nnedition.ymdownloader.api.download

import nn.edition.yalogger.logger
import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

abstract class AbstractMusicDownloader(
    open val ymClient: YandexMusicClient
) : IMusicDownloader {
    override fun downloadArtist(artistId: Long, config: IConfiguration) =
        downloadArtist(this.ymClient.getArtist(artistId), config)
    abstract fun downloadArtist(artist: ArtistMetaResult, config: IConfiguration = this.config)

    override fun downloadAlbum(albumId: Long, config: IConfiguration) =
        downloadAlbum(this.ymClient.getAlbum(albumId), config)
    abstract fun downloadAlbum(album: Album, config: IConfiguration = this.config)

    override fun downloadTrack(trackId: Long, config: IConfiguration) =
        downloadTrack(this.ymClient.getTrack(trackId), config)
    abstract fun downloadTrack(track: Track, config: IConfiguration = this.config)

    companion object {
        val logger = logger(AbstractMusicDownloader::class.java)

        fun String.parsePathPlaceholders(track: Track): String =
            pathPlaceholders(track).entries.fold(this) { currentText, (key, value) ->
                currentText.replace(key, value.fixPath())
            }

        fun String.parseFilePlaceholders(track: Track) =
            filePlaceholders(track).entries.fold(this) { currentText, (key, value) ->
                currentText.replace(key, value.fixPath())
            }

        fun placeholders(track: Track) = mapOf(
            "%author_name%" to track.publisher.name,
            "%album_title%" to track.album.fullTitle,
            "%track_title%" to track.fullTitle,
        )

        fun pathPlaceholders(track: Track) = placeholders(track)

        fun filePlaceholders(track: Track) =
            placeholders(track).toMutableMap().also { places ->
                track.num.let { places["%track_num%"] = if (it.toInt() in 1..9) "0${it}" else it }
            }

        private fun String.fixPath() = this.replace("/", "\\")

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

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")!!

        fun decryptTrack(encData: ByteArray, key: String): ByteArray {
            val keyBytes = key.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

            if (keyBytes.size != 16)
                throw IllegalArgumentException("Key must be 16 bytes")

            val nonce = ByteArray(16) { 0 }

            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(nonce)

            this.cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            return this.cipher.doFinal(encData)
        }
    }
}