package ru.nnedition.ymdownloader.terminal.jline

import ru.nnedition.ymdownloader.terminal.Terminal
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.Terminal as JTerminal
import org.jline.utils.InfoCmp
import ru.nnedition.ymdownloader.terminal.context.RunningContext
import ru.nnedition.ymdownloader.terminal.context.impl.TerminalContext
import java.nio.charset.StandardCharsets

@Suppress("LeakingThis", "MemberVisibilityCanBePrivate")
open class JLineTerminal : Terminal {
    @Volatile
    var context: TerminalContext = RunningContext()
        set(value) {
            field = value
            runner.updateContext()
        }

    private lateinit var runner: JLineTerminalRunner

    val terminal: JTerminal = TerminalBuilder.builder()
        .system(true)
        .dumb(true)
        .encoding(StandardCharsets.UTF_8)
        .streams(System.`in`, System.out)
        .build()

    val history = DefaultHistory()

    val lineReader: LineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .history(history)
//        .completer(CommandCompleter())
        .build()

    override fun start() {
        runner = JLineTerminalRunner(this)
        runner.start()
    }

    override fun close() {
        runner.interrupt()
        terminal.close()
    }

    override fun write(message: String) {
        terminal.puts(InfoCmp.Capability.carriage_return)
        terminal.writer().println(message)
        redraw()
    }

    private fun redraw() {
        if (lineReader.isReading) {
            lineReader.callWidget("redraw-line")
            lineReader.callWidget("redisplay")
        }
    }
}