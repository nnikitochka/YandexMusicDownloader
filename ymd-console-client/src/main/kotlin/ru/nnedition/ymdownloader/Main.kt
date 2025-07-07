package ru.nnedition.ymdownloader

import ru.nnedition.ymdownloader.api.client.YandexMusicClient
import ru.nnedition.ymdownloader.api.download.YandexMusicDownloader
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

            var text = input.nextLine()

            when {
                text == "stop" -> {
                    print("Остановка программы...")
                    break
                }
                text.startsWith("https://music.yandex.ru/") -> {
                    text = text.removePrefix("https://music.yandex.ru/")
                    when {
                        text.startsWith("artist/") -> {
                            val artistId = text.removePrefix("artist/").toLongOrNull()
                            if (artistId == null) continue

                            runCatching {
                                downloader.downloadArtist(artistId, config)
                            }.onFailure { it.printStackTrace() }
                        }

                        text.startsWith("album/") -> {
                            val albumId = text.removePrefix("album/").toLongOrNull()
                            if (albumId == null) continue

                            runCatching {
                                downloader.downloadAlbum(albumId, config)
                            }.onFailure { it.printStackTrace() }
                        }

                        text.startsWith("track/") -> {
                            val trackId = text.removePrefix("track/").toIntOrNull()
                            if (trackId == null) continue

                            println("Обработка трека: $text")
                        }
                        else -> {
                            println("Неизвестный тип ссылки: $text")
                        }
                    }
                }
                else -> println(text)
            }
        }
        input.close()
    }
}