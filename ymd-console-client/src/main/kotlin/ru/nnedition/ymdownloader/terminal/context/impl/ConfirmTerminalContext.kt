package ru.nnedition.ymdownloader.terminal.context.impl

abstract class ConfirmTerminalContext : TerminalContext {
    override val prompt = "[Д/н]: "

    companion object {
        private val confirmTexts = listOf("д", "да", "y", "ye", "yes")

        fun isConfirm(text: String) = confirmTexts.contains(text)
    }
}