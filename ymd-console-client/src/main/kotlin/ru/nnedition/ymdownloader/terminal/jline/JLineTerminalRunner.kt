package ru.nnedition.ymdownloader.terminal.jline

import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import ru.nnedition.ymdownloader.Launcher
import ru.nnedition.ymdownloader.api.link.LinkParser
import ru.nnedition.ymdownloader.api.utils.GenreTranslator
import ru.nnedition.ymdownloader.terminal.context.CloseRequestContext
import ru.nnedition.ymdownloader.terminal.context.GenreSaveConfirmContext
import ru.nnedition.ymdownloader.terminal.context.GenreSelectContext
import ru.nnedition.ymdownloader.terminal.context.RunningContext

class JLineTerminalRunner(
    val terminal: JLineTerminal
) : Thread() {
    val jTerminal = this.terminal.lineReader.terminal!!

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
                line = this.terminal.lineReader.readLine(this.terminal.context.prompt)
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

            val context = this.terminal.context
            when (context) {
                is RunningContext -> {
                    if (line == "stop" || line == "exit" || line == "close") {
                        this.terminal.context = CloseRequestContext()
                        continue
                    }
                    else if (line == "pause") {
                        println("Пока не прописал(")
                        continue
                    }

                    val info = LinkParser.parseUrl(line) ?: run {
                        println("Ссылка не распознана, возможно вы ввели что-то не так :(")
                        continue
                    }

                    Launcher.downloader.download(info)
                }

                is GenreSelectContext -> {
                    println("Перевод: \"$line\". Подтвердите сохранение.")
                    this.terminal.context = GenreSaveConfirmContext(context.genre, line)
                }
                is GenreSaveConfirmContext -> {
                    if (line == "д" || line == "да" || line == "y" || line === "ye" || line == "yes") {
                        GenreTranslator.saveTranslation(context.genre, context.translation)
                        this.terminal.context = RunningContext()
                        continue
                    }

                    this.terminal.context = GenreSelectContext(context.genre)
                }

                is CloseRequestContext -> {
                    if (line == "д" || line == "да" || line == "y" || line === "ye" || line == "yes") {
                        Launcher.shutdown()
                        break
                    }

                    println("Вы отменили закрытие.")
                    this.terminal.context = RunningContext()

                }
            }
        }
    }
}