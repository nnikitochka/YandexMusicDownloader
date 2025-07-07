package ru.nnedition.ymdownloader.api.objects

import ru.nnedition.ymdownloader.api.objects.artist.Artist

data class Track(
    val id: Long,
    val title: String,
    val available: Boolean,
    val artists: List<Artist>,
    val coverUri: String,
    val genre: String?,
)