package ru.nnedition.ymdownloader

import oshi.PlatformEnum
import oshi.SystemInfo
import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.YandexMusicDownloader
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.ffmpeg.FileFfmpegProvider
import ru.nnedition.ymdownloader.api.ffmpeg.LinuxFfmpegProvider
import ru.nnedition.ymdownloader.api.link.LinkParser
import ru.nnedition.ymdownloader.api.link.LinkType
import java.util.Scanner
import kotlin.system.exitProcess

object Main {
    val config = TomlConfig()
    lateinit var ymClient: YandexMusicClient
    lateinit var downloader: YandexMusicDownloader

    @JvmStatic
    fun main(args: Array<String>) {
        var ffmpegProvider: FfmpegProvider? = null

        try {
            ffmpegProvider = try {
                FileFfmpegProvider(config.ffmpegPath)
            } catch (e: Exception) {
                when (SystemInfo.getCurrentPlatform()) {
                    PlatformEnum.LINUX -> LinuxFfmpegProvider()
                    else -> throw Exception("Неизвестная платформа: ${SystemInfo.getCurrentPlatform()}.")
                }
            }
        } catch (e: Exception) {
            println(e.message)

            val input = Scanner(System.`in`)

            while (ffmpegProvider == null) {
                println("Пожалуйста, укажите путь к ffmpeg или \"stop\", чтобы остановить программу: ")

                val text = input.nextLine()

                if (text == "stop") {
                    println("Остановка программы...")
                    exitProcess(0)
                }

                try {
                    ffmpegProvider = FileFfmpegProvider(text)
                    config.ffmpegPath = text
                    config.save()
                    println("Успешно! Путь к ffmpeg сохранен.")
                } catch (e: Exception) {
                    println("Ошибка при инициализации ffmpeg: ${e.message}. Попробуйте еще раз.")
                }
            }

            input.close()
        }

        ymClient = YandexMusicClient.create(config.token)

        downloader = YandexMusicDownloader(
            config,
            ymClient,
            ffmpegProvider
        )

        val input = Scanner(System.`in`)
        while (true) {
            print("Введите ссылку или \"stop\", чтобы остановить программу: ")

            val text = input.nextLine()

            if (text == "stop") {
                println("Остановка программы...")
                break
            }

            val info = LinkParser.parseUrl(text)

            if (info == null) {
                println("Ссылка не распознана, возможно вы ввели что-то не так :(")
                continue
            }

            runCatching {
                when (info.type) {
                    LinkType.ALBUM -> downloader.downloadAlbum(info.id, config)
                    LinkType.ARTIST -> downloader.downloadArtist(info.id, config)
                    LinkType.TRACK -> downloader.downloadTrack(info.id, config)
                }
            }.onFailure { it.printStackTrace() }
        }

        input.close()

        println("Спасибо за использование программы :>")
    }
}