package ru.nnedition.ymdownloader.terminal.context

import ru.nnedition.ymdownloader.terminal.context.impl.TerminalContext

class GenreSelectContext : TerminalContext {
    override val prompt = "Введите жанр: "

    @Volatile
    var genre: String? = null
}