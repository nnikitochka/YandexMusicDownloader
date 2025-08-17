package ru.nnedition.ymdownloader.terminal.jline

import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import ru.nnedition.ymdownloader.Launcher
import ru.nnedition.ymdownloader.api.link.LinkParser
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.terminal.context.CloseRequestContext
import ru.nnedition.ymdownloader.terminal.context.GenreSaveConfirmContext
import ru.nnedition.ymdownloader.terminal.context.GenreSelectContext
import ru.nnedition.ymdownloader.terminal.context.GenreTranslateContext
import ru.nnedition.ymdownloader.terminal.context.RunningContext
import ru.nnedition.ymdownloader.terminal.context.impl.ConfirmTerminalContext

class JLineTerminalRunner(
    val terminal: JLineTerminal
) : Thread() {
    val jReader = this.terminal.lineReader
    val jTerminal = this.jReader.terminal!!

    init {
        isDaemon = false
        name = "TerminalRunner"
        priority = 1
    }

    @Volatile
    private var needContextUpdate = false
    fun updateContext() {
        this.needContextUpdate = true
        this.jTerminal.raise(Terminal.Signal.INT)
    }

    override fun run() {
        var line: String

        while (!currentThread().isInterrupted) {
            try {
                line = this.jReader.readLine(this.terminal.context.prompt)
            } catch (e: UserInterruptException) {
                if (this.needContextUpdate) {
                    this.needContextUpdate = false

                    // Очистка строки и возврат курсора в начало
                    this.jTerminal.writer().print("\r\u001B[K")
                    this.jTerminal.writer().flush()
                    continue
                }

                println("Вы действительно хотите выключить приложение?")
                this.terminal.context = CloseRequestContext()
                continue
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }

            line = line.trim()

            if (line.isEmpty()) continue

            when (val context = this.terminal.context) {
                is RunningContext -> {
                    if (line == "stop" || line == "exit" || line == "close") {
                        this.terminal.context = CloseRequestContext()
                        continue
                    }
                    else if (line == "pause") {
                        Launcher.downloader.paused = if (!Launcher.downloader.paused) {
                            println("Остановка загрузки...")
                            true
                        } else {
                            println("Возобновление загрузки...")
                            false
                        }
                        continue
                    }
                    else if (line == "status") {
                        println("В очереди на загрузку ${Launcher.downloader.toDownloadQueue.size} треков.")
                        println("В очереди ${Launcher.downloader.downloadQueue.size} треков.")
                        if (Launcher.downloader.paused)
                            println("Загрузка приостановлена.")
                        continue
                    }

                    val info = LinkParser.parseUrl(line) ?: run {
                        println("Ссылка не распознана, возможно вы ввели что-то не так :(")
                        continue
                    }

                    Launcher.downloader.download(info)
                }

                is GenreSelectContext -> {
                    context.genre = line
                    this.terminal.context = RunningContext()
                }
                is GenreTranslateContext -> {
                    println("Перевод: \"$line\". Подтвердите сохранение.")
                    this.terminal.context = GenreSaveConfirmContext(context.genre, line)
                }
                is GenreSaveConfirmContext -> {
                    if (ConfirmTerminalContext.isConfirm(line)) {
                        GenreTranslator.saveTranslation(context.genre, context.translation)
                        this.terminal.context = RunningContext()
                        continue
                    }

                    this.terminal.context = GenreTranslateContext(context.genre)
                }

                is CloseRequestContext -> {
                    if (ConfirmTerminalContext.isConfirm(line))
                        break

                    println("Вы отменили закрытие.")
                    this.terminal.context = RunningContext()

                }
            }
        }

        Launcher.shutdown()
    }
}