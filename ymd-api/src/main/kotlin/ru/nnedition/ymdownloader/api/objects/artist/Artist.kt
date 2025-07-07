package ru.nnedition.ymdownloader.api.objects.artist

import ru.nnedition.ymdownloader.api.objects.album.Album

data class Artist(
    val id: Long,
    val name: String,
    val albums: List<Album>,
)