package ru.nnedition.lrclib

data class LyricMeta(
    var artists: MutableList<String> = mutableListOf(),
    var title: String? = null,
    var album: String? = null,
    var writers: MutableList<String> = mutableListOf(),
    var offset: Long? = null,
    var other: MutableMap<String, String> = mutableMapOf(),
)