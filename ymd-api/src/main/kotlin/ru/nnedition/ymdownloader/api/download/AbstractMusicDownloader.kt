package ru.nnedition.ymdownloader.api.download

import nn.edition.yalogger.logger
import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import ru.nnedition.ymdownloader.api.objects.LyricInfo
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

    fun downloadAlbumCover(url: String, path: File, fileName: String) {
        val coverFile = File(path, "$fileName.jpg")
        if (coverFile.exists()) return

        FileOutputStream(coverFile).use { output ->
            val data = this.ymClient.getCoverData(url, true, false, useCache = false)

            output.write(data)
        }
    }

    companion object {
        val logger = logger(AbstractMusicDownloader::class.java)

        fun String.parsePathPlaceholders(track: Track, config: IConfiguration): String =
            pathPlaceholders(track).entries.fold(this) { currentText, (key, value) ->
                if (currentText.contains(key))
                    currentText.replace(
                        key,
                        value.applyFileReplacements(config)
                    )
                else currentText
            }

        fun String.parseFilePlaceholders(track: Track, config: IConfiguration) =
            filePlaceholders(track).entries.fold(this) { currentText, (key, value) ->
                if (currentText.contains(key))
                    currentText.replace(
                        key,
                        value.applyFileReplacements(config)
                    )
                else currentText
            }

        fun placeholders(track: Track) = mapOf(
            "%author_name%" to track.publisher.name,
            "%album_title%" to track.album.fullTitle,
            "%year%" to track.album.year.toString(),
            "%track_title%" to track.fullTitle,
        )

        fun pathPlaceholders(track: Track) = placeholders(track)

        fun filePlaceholders(track: Track) =
            placeholders(track).toMutableMap().also { places ->
                track.num.let { places["%track_num%"] = if (it.toInt() in 1..9) "0${it}" else it }
            }

        fun String.applyFileReplacements(config: IConfiguration): String {
            var result = this.replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "_")
            for ((from, to) in config.fileReplacements) {
                result = result.replace(from, to)
            }
            return result
        }

        fun downloadTrack(info: DownloadInfo, outputFile: File) {
            FileOutputStream(outputFile).use { output ->
                val bytes = download(info.url)
                val decrypted = decryptTrack(bytes, info.key)

                output.write(decrypted)
            }
        }

        fun downloadLyric(info: LyricInfo, outputFile: File) {
            FileOutputStream(outputFile).use { output ->
                val bytes = download(info.downloadUrl)

                output.write(bytes)
            }
        }

        fun download(url: String): ByteArray = download(URL(url))
        fun download(url: URL): ByteArray {
            val connect = url.openConnection() as HttpURLConnection
            connect.requestMethod = "GET"
            connect.connectTimeout = 20_000
            connect.readTimeout = 20_000

            if (connect.responseCode == HttpURLConnection.HTTP_OK) {
                val bytes = connect.inputStream.readAllBytes()
                connect.disconnect()
                return bytes
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