package ru.nnedition.ymdownloader

import ru.nnedition.ymdownloader.api.client.YandexMusicClient
import ru.nnedition.ymdownloader.api.download.YandexMusicDownloader
import ru.nnedition.ymdownloader.api.link.LinkInfo
import ru.nnedition.ymdownloader.api.link.LinkParser
import ru.nnedition.ymdownloader.api.link.LinkType
import java.util.Scanner

object Main {
    val config = TomlConfig()
    val ymClient = YandexMusicClient.create(config.token)
    val downloader = YandexMusicDownloader(config, ymClient)

    @JvmStatic
    fun main(args: Array<String>) {

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