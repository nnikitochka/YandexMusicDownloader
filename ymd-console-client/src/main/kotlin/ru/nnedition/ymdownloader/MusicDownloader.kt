package ru.nnedition.ymdownloader

import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.download.AbstractMusicDownloader
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.link.LinkInfo
import ru.nnedition.ymdownloader.api.link.LinkType
import ru.nnedition.ymdownloader.api.objects.LyricInfo
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import ru.nnedition.ymdownloader.api.utils.AudioTagWriter
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.api.utils.createDir
import ru.nnedition.ymdownloader.audio.AudioPlayer
import ru.nnedition.ymdownloader.audio.AudioType
import ru.nnedition.ymdownloader.terminal.context.GenreSelectContext
import ru.nnedition.ymdownloader.terminal.context.GenreTranslateContext
import java.io.File
import java.lang.Thread.currentThread
import java.util.concurrent.Executors
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

    val toDownloadQueue = LinkedBlockingQueue<DownloadItem>()
    val downloadQueue = LinkedBlockingQueue<DownloadItem>()

    val thread = thread(
        isDaemon = false,
        name = "TerminalRunner",
        priority = 1
    ) {
        while (!currentThread().isInterrupted) {
            val item = this.downloadQueue.take()
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

            var successDownload = false
            repeat(3) { attempt ->
                try {
                    downloadTrack(info, if (shouldMux) toMuxFile else finalFile)
                    successDownload = true
                    return@repeat
                } catch (e: Exception) {
                    logger.error("Ошибка при ${attempt+1} попытке загрузки трека ${track.fullTitle}: ${e.message}")
                }
            }

            if (!successDownload) {
                if (shouldMux)
                    toMuxFile.delete()
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

            AudioTagWriter.write(
                file = finalFile,
                track = track,
                genre = item.genre,
                cover = this.ymClient.getCoverData(track.coverUri, false, false, "300x300"),
            )

            item.lyricInfo?.let { lyricInfo ->
                File(path, "${finalFile.nameWithoutExtension}.lrc").takeIf { !it.exists() }?.let { lyricFile ->
                    downloadLyric(lyricInfo, lyricFile)
                }
            }

            println("Загрузка ${if (track.album.isSingle()) "сингла" else "трека" } \"${track.fullTitle}\" окончена.")

            if (this.downloadQueue.isEmpty())
                println("Загрузка завершена.")
        }
    }

    val processExecutor = Executors.newSingleThreadExecutor()

    fun download(info: LinkInfo) {
        this.processExecutor.execute {
            runCatching {
                when (info.type) {
                    LinkType.ARTIST -> downloadArtist(info.id)
                    LinkType.ALBUM -> downloadAlbum(info.id)
                    LinkType.TRACK -> downloadTrack(info.id)
                }

                Launcher.terminal.terminal.writer().also {
                    it.print("\r\u001B[2K")
                    it.print("Обработка треков окончена. Всего добавлено в очередь: ${this.toDownloadQueue.size}.")
                    it.flush()
                }

                this.downloadQueue.addAll(this.toDownloadQueue)
                this.toDownloadQueue.clear()
            }.onFailure { it.printStackTrace() }
        }
    }

    override fun downloadArtist(artist: ArtistMetaResult, config: IConfiguration) {
        artist.albums.forEach { album ->
            downloadAlbum(album.id, config)
        }
    }

    override fun downloadAlbum(album: Album, config: IConfiguration) {
        if (!album.available) {
            logger.error("${if (album.isSingle()) "Сингл" else "Альбом"} \"${album.title}\" (${album.publisher.name}) недоступен")
            return
        }

        album.tracks.forEach { track ->
            downloadTrack(track, config)
        }
    }

    override fun downloadTrack(track: Track, config: IConfiguration) {
        if (!track.available) {
            logger.error("Трек \"${track.fullTitle}\" (${track.publisher.name} - ${track.album.title}) недоступен")
            return
        }

        track.album = this.ymClient.getAlbum(track.albums[0].id)

        @Suppress("ControlFlowWithEmptyBody")
        val genre = (track.genre ?: track.album.genre)?.let { genre ->
            GenreTranslator.translate(genre) ?: let {
                Launcher.terminal.context = GenreTranslateContext(genre)
                logger.warn("Найден неизвестный жанр: \"$genre\"")
                AudioPlayer.play(AudioType.NOTIFICATION)
                while (GenreTranslator.translate(genre) == null) {}
                GenreTranslator.translate(genre)
            }
        } ?: run {
            val context = GenreSelectContext()
            Launcher.terminal.context = context
            logger.warn("Жанр трека \"${track.title}\" не найден. Введите его вручную:")
            AudioPlayer.play(AudioType.NOTIFICATION)
            while (context.genre == null) {}
            context.genre!!
        }

        val item = DownloadItem(
            config,
            track,
            genre,
            ymClient.getLyricInfo(track.id.toString()),
        )

        this.toDownloadQueue.put(item)

        for ((index, track) in this.toDownloadQueue.withIndex()) {
            Launcher.terminal.terminal.writer().also {
                it.print("\r\u001B[2K")
                it.print("Обработка треков: ${track.track.fullTitle} (${index + 1})")
                it.flush()
            }
        }
    }

    class DownloadItem(
        val config: IConfiguration,
        val track: Track,
        val genre: String,
        val lyricInfo: LyricInfo?,
    )
}