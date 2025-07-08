package ru.nnedition.ymdownloader.api.objects

import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta

data class Track(
    val id: Long,
    val title: String,
    val available: Boolean,
    val artists: List<ArtistMeta>,
    val albums: List<Album>,
    val coverUri: String,
    val lyricsAvailable: Boolean,
    val genre: String?,
)