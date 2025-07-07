package ru.nnedition.ymdownloader.api.objects.album

import ru.nnedition.ymdownloader.api.objects.artist.Artist

open class AlbumMeta(
    open val id: Long,
    open val title: String,
    open val year: Int,
    open val genre: String,
    open val artists: List<Artist>,
    open val available: Boolean,
)