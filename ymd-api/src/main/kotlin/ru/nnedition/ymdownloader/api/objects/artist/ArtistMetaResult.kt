package ru.nnedition.ymdownloader.api.objects.artist

import ru.nnedition.ymdownloader.api.objects.album.AlbumMeta

class ArtistMetaResult(
    val artist: ArtistMeta,
    val albums: List<AlbumMeta>
)