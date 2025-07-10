package ru.nnedition.ymdownloader

import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.YandexMusicDownloader
import ru.nnedition.ymdownloader.api.ffmpeg.LinuxFFmpegProvider
import ru.nnedition.ymdownloader.api.link.LinkParser
import ru.nnedition.ymdownloader.api.link.LinkType
import java.util.Scanner

object Main {
    val config = TomlConfig()
    val ymClient = YandexMusicClient.create(config.token)
    lateinit var downloader: YandexMusicDownloader

    @JvmStatic
    fun main(args: Array<String>) {
        downloader = YandexMusicDownloader(config, ymClient, LinuxFFmpegProvider())


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