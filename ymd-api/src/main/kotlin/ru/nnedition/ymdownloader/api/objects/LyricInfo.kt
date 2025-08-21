package ru.nnedition.ymdownloader.api.objects

data class LyricInfo(
    val downloadUrl: String,
    val lyricId: Int,
    val externalLyricId: String,
    val writers: List<String>,
)