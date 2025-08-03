package ru.nnedition.ymdownloader.api.downloader

import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import ru.nnedition.ymdownloader.api.utils.AudioTagWriter
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.api.utils.createDir
import java.io.File

class YandexMusicDownloader(
    override var config: IConfiguration,
    override val ymClient: YandexMusicClient,
    val ffmpeg: FfmpegProvider
) : AbstractMusicDownloader(ymClient) {
    constructor(config: IConfiguration, ffmpeg: FfmpegProvider) :
            this(config, YandexMusicClient.create(config.token), ffmpeg)

    override fun downloadArtist(artist: ArtistMetaResult, config: IConfiguration) {
        logger.info("Обработка артиста \"${artist.artist.name}\"...")

        artist.albums.forEach { album ->
            downloadAlbum(album.id, config)
        }
    }

    override fun downloadAlbum(album: Album, config: IConfiguration) {
        if (!album.available)
            throw Exception("Альбом \"${album.title}\" недоступен")

        logger.info("Обработка альбома \"${album.title}\"...")

        album.tracks[0].forEach { track ->
            downloadTrack(track, config)
        }
    }

    override fun downloadTrack(track: Track, config: IConfiguration) {
        if (!track.available)
            throw Exception("Трек \"${track.title}\" недоступен")

        logger.info("Обработка трека \"${track.title}\"...")

        val info = this.ymClient.getFileInfo(track.id.toString(), config.quality.toString())

        val format = if (info.codec.contains("-")) {
            val parts = info.codec.split("-")
            if (parts[0] == "he") parts[1]
            else parts[0]
        }
        else info.codec

        val album = track.albums[0]
        val publisher = track.artists[0]

        val path = File(config.outPath.parsePathPlaceholders(publisher, album, track)).createDir()

        val fileName = config.trackTemplate.parseFilePlaceholders(publisher, album, track)

        var finalFile = File(path, "$fileName.$format")

        if (finalFile.exists()) {
            logger.info("Трек \"${track.title}\" уже существует.")
            return
        }

        val outputFile = File(path, "$fileName.mp4")

        try {
            downloadTrack(info, outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            e.printStackTrace()
            return
        }

        try {
            this.ffmpeg.mux(outputFile, finalFile)
            outputFile.delete()
        } catch (e: Exception) {
            outputFile.delete()
            finalFile.delete()
            e.printStackTrace()
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

        val genre = (track.genre ?: album.genre)?.let { genre ->
            GenreTranslator.translate(genre) ?: let {
                logger.warn("Найден неизвестный жанр: \"$genre\"")
                genre
            }
        }

        AudioTagWriter.write(finalFile, cover, track, album, genre)
    }
}