package ru.nnedition.ymdownloader.terminal

import nn.edition.yalogger.TerminalWriter

interface Terminal : TerminalWriter {
    fun start()

    fun close()
}