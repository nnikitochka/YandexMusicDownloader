package ru.nnedition.ymdownloader.api.objects

data class DownloadInfo(
    val quality: Quality,
    val codec: String,
    val bitrate: Int,
    val key: String,
    val url: String,
)