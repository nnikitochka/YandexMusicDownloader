package ru.nnedition.ymdownloader.terminal.context

import ru.nnedition.ymdownloader.terminal.context.impl.TerminalContext

class RunningContext : TerminalContext {
    override val prompt = "> "
}