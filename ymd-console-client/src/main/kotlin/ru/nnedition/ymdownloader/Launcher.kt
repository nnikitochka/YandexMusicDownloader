package ru.nnedition.ymdownloader

import nn.edition.yalogger.LoggerFactory
import nn.edition.yalogger.logger
import oshi.PlatformEnum
import oshi.SystemInfo
import ru.nnedition.ymdownloader.api.YandexMusicClient
import ru.nnedition.ymdownloader.api.ffmpeg.FfmpegProvider
import ru.nnedition.ymdownloader.api.ffmpeg.FileFfmpegProvider
import ru.nnedition.ymdownloader.api.ffmpeg.LinuxFfmpegProvider
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.audio.AudioPlayer
import ru.nnedition.ymdownloader.audio.AudioType
import ru.nnedition.ymdownloader.config.TomlConfig
import ru.nnedition.ymdownloader.terminal.jline.JLineTerminal
import java.util.Scanner
import kotlin.system.exitProcess

object Launcher {
    val logger = logger(this::class.java)

    val config = TomlConfig()
    lateinit var ymClient: YandexMusicClient
    lateinit var downloader: MusicDownloader

    val terminal = JLineTerminal()

    @JvmStatic
    fun main(args: Array<String>) {
        LoggerFactory.logFormat = "{message}"

        AudioPlayer.preloadAudio()

        val ffmpegProvider = getFfmpegProvider()

        val isTokenValid = YandexMusicClient.validateToken(this.config.token)

        if (isTokenValid.getOrNull() != true) {
            isTokenValid.exceptionOrNull()?.printStackTrace()
            this.logger.error("Токен невалиден или не указан. Пожалуйста, укажите корректный токен в файле конфигурации.")
            exitProcess(1)
        }

        this.logger.info("Успешная валидация токена.")

        this.ymClient = YandexMusicClient.create(this.config.token)

        this.logger.info("Успешная авторизация.")

        this.downloader = MusicDownloader(
            this.config,
            this.ymClient,
            ffmpegProvider
        )

        this.logger.info("Инициализация загрузчика прошла успешно.")

        GenreTranslator.loadTranslations()

        println("Введите ссылку или \"stop\", чтобы остановить программу: ")
        LoggerFactory.terminalWriter = this.terminal
        this.terminal.start()

        Runtime.getRuntime().addShutdownHook(Thread { onShutdown() })
    }

    fun shutdown() {
        onShutdown()
        exitProcess(0)
    }

    fun onShutdown() {
        this.terminal.close()
        GenreTranslator.foldTranslations()
        println("Спасибо за использование YandexMusicDownloader :>")
    }

    fun getFfmpegProvider(): FfmpegProvider {
        var provider: FfmpegProvider? = null

        try {
            provider = try {
                FileFfmpegProvider(this.config.ffmpegPath)
            } catch (e: Exception) {
                when (SystemInfo.getCurrentPlatform()) {
                    PlatformEnum.LINUX -> LinuxFfmpegProvider()
                    else -> throw Exception("Неизвестная платформа: ${SystemInfo.getCurrentPlatform()}.")
                }
            }
        } catch (e: Exception) {
            this.logger.error(e.message)
            AudioPlayer.play(AudioType.NOTIFICATION)

            val input = Scanner(System.`in`)

            while (provider == null) {
                println("Пожалуйста, укажите путь к ffmpeg или \"stop\", чтобы остановить программу: ")

                val text = input.nextLine()

                if (text == "stop") {
                    println("Остановка программы...")
                    shutdown()
                }

                try {
                    provider = FileFfmpegProvider(text)
                    this.config.ffmpegPath = text
                    this.config.save()
                    println("Успешно! Путь к ffmpeg сохранен.")
                } catch (e: Exception) {
                    println("Ошибка при инициализации ffmpeg: ${e.message}. Попробуйте еще раз.")
                }
            }

            input.close()
        }

        return provider
    }
}