package ru.nnedition.ymdownloader.api.utils

import java.io.File

fun File.createDir() = this.also { it.mkdirs() }