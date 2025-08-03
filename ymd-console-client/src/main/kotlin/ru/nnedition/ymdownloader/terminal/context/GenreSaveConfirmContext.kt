package ru.nnedition.ymdownloader.terminal.context

import ru.nnedition.ymdownloader.terminal.context.impl.ConfirmTerminalContext

class GenreSaveConfirmContext(
    val genre: String,
    val translation: String,
) : ConfirmTerminalContext()