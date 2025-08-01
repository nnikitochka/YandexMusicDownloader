package ru.nnedition.ymdownloader.api.downloader

import nn.edition.yalogger.logger
import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta
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

        fun String.parsePathPlaceholders(publisher: ArtistMeta, album: Album, track: Track): String = pathPlaceholders(publisher, album, track)
            .entries.fold(this) { currentText, (key, value) ->
                currentText.replace(key, value.fixPath())
            }

        fun String.parseFilePlaceholders(publisher: ArtistMeta, album: Album, track: Track) = filePlaceholders(publisher, album, track)
            .entries.fold(this) { currentText, (key, value) ->
                currentText.replace(key, value)
            }

        fun placeholders(publisher: ArtistMeta, album: Album, track: Track) = mapOf(
            "%author_name%" to publisher.name,
            "%album_title%" to album.title,
            "%track_title%" to track.title,
        )

        fun pathPlaceholders(publisher: ArtistMeta, album: Album, track: Track) = placeholders(publisher, album, track)

        fun filePlaceholders(publisher: ArtistMeta, album: Album, track: Track) =
            placeholders(publisher, album, track).toMutableMap().also {
                it.put("%track_num%", album.tracks[0].indexOf(track).toString())
            }

        private fun String.fixPath() = replace("/", "\\")

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