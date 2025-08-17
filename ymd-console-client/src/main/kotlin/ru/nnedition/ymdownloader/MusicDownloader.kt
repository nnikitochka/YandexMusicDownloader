package ru.nnedition.ymdownloader

import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.download.AbstractMusicDownloader
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.link.LinkInfo
import ru.nnedition.ymdownloader.api.link.LinkType
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import ru.nnedition.ymdownloader.api.utils.AudioTagWriter
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.api.utils.createDir
import ru.nnedition.ymdownloader.terminal.context.GenreSelectContext
import ru.nnedition.ymdownloader.terminal.context.GenreTranslateContext
import java.io.File
import java.lang.Thread.currentThread
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

@Suppress("HasPlatformType")
class MusicDownloader(
    override var config: IConfiguration,
    override val ymClient: YandexMusicClient,
    val ffmpeg: FfmpegProvider
) : AbstractMusicDownloader(ymClient) {
    @Volatile
    var paused = false

    private val queue = LinkedBlockingQueue<DownloadItem>()

    val thread = thread(
        isDaemon = false,
        name = "TerminalRunner",
        priority = 1
    ) {
        while (!currentThread().isInterrupted) {
            val item = this.queue.take()
            @Suppress("ControlFlowWithEmptyBody")
            while (this.paused) {}

            val track = item.track
            val config = item.config

            val info = this.ymClient.getFileInfo(track.id.toString(), config.quality.toString())

            val format = if (info.codec.contains("-")) {
                val parts = info.codec.split("-")
                if (parts[0] == "he") parts[1]
                else parts[0]
            }
            else info.codec

            val shouldMux = info.codec.contains("mp4")

            val path = File(config.outPath.parsePathPlaceholders(track)).createDir()

            val fileName = config.trackTemplate.parseFilePlaceholders(track)

            var finalFile = File(path, "$fileName.$format")

            if (finalFile.exists()) {
                logger.info("Трек \"${track.fullTitle}\" уже существует.")
                continue
            }

            val toMuxFile = File(path, "$fileName.mp4")

            try {
                downloadTrack(info, if (shouldMux) toMuxFile else finalFile)
            } catch (e: Exception) {
                toMuxFile.delete()
                e.printStackTrace()
                continue
            }

            if (shouldMux) {
                try {
                    this.ffmpeg.mux(toMuxFile, finalFile)
                    toMuxFile.delete()
                } catch (e: Exception) {
                    toMuxFile.delete()
                    finalFile.delete()
                    e.printStackTrace()
                    continue
                }
            }

            if (format == "aac") {
                val interimFile = File(path, "$fileName.flac")

                if (interimFile.exists()) {
                    logger.info("Трек \"$fileName\" уже существует.")
                    finalFile.delete()
                    continue
                }

                this.ffmpeg.convert(finalFile, interimFile)
                finalFile.delete()
                finalFile = interimFile
            }

            @Suppress("ControlFlowWithEmptyBody")
            val genre = (track.genre ?: track.album.genre)?.let { genre ->
                GenreTranslator.translate(genre) ?: let {
                    logger.warn("Найден неизвестный жанр: \"$genre\"")
                    Launcher.terminal.context = GenreTranslateContext(genre)
                    while (GenreTranslator.translate(genre) == null) {}
                    GenreTranslator.translate(genre)
                }
            } ?: run {
                logger.warn("Жанр трека \"${track.title}\" не найден. Введите его вручную:")
                val context = GenreSelectContext()
                Launcher.terminal.context = context
                while (context.genre == null) {}
                context.genre!!
            }

            AudioTagWriter.write(
                file = finalFile,
                track = track,
                genre = genre,
                cover = this.ymClient.getCoverData(track.coverUri, false, false, "300x300"),
            )
        }
    }

    fun download(info: LinkInfo) {
        runCatching {
            when (info.type) {
                LinkType.ARTIST -> downloadArtist(info.id)
                LinkType.ALBUM -> downloadAlbum(info.id)
                LinkType.TRACK -> downloadTrack(info.id)
            }

            println("Скачивание завершено.")
        }.onFailure { it.printStackTrace() }
    }

    override fun downloadArtist(artist: ArtistMetaResult, config: IConfiguration) {
        logger.info("Обработка артиста \"${artist.artist.name}\"...")

        artist.albums.forEach { album ->
            downloadAlbum(album.id, config)
        }
    }

    override fun downloadAlbum(album: Album, config: IConfiguration) {
        if (!album.available) {
            logger.error("${if (album.isSingle()) "Сингл" else "Альбом"} \"${album.title}\" недоступен")
            return
        }

        logger.info("Обработка ${if (album.isSingle()) "сингла" else "альбома" } \"${album.title}\"...")

        album.tracks.forEach { track ->
            downloadTrack(track, config)
        }
    }

    override fun downloadTrack(track: Track, config: IConfiguration) {
        if (!track.available) {
            logger.error("Трек \"${track.fullTitle}\" недоступен")
            return
        }

        track.album = this.ymClient.getAlbum(track.albums[0].id)

        if (!track.album.isSingle())
            logger.info("Обработка трека \"${track.fullTitle}\"...")

        this.queue.put(DownloadItem(config, track))
    }

    class DownloadItem(
        val config: IConfiguration,
        val track: Track,
    )
}