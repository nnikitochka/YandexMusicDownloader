package ru.nnedition.ymdownloader.terminal.context

import ru.nnedition.ymdownloader.terminal.context.impl.TerminalContext

class GenreTranslateContext(
    val genre: String,
) : TerminalContext {
    override val prompt = "Введите перевод жанра: "
}