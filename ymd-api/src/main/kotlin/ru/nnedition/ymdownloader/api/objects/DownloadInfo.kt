package ru.nnedition.ymdownloader.api.objects

data class DownloadInfo(
    val url: String,
    val codec: String,
    val bitrate: Int,
    val key: String,
    val gain: Boolean,
    val preview: Boolean
)