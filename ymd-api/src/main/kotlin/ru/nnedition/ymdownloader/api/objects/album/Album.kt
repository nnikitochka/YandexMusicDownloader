package ru.nnedition.ymdownloader.api.objects.album

import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta

data class Album(
    val id: Long,
    val title: String,
    val type: String?,
    val version: String?,
    val year: Int,
    val coverUri: String,
    val genre: String?,
    val artists: List<ArtistMeta>,
    val available: Boolean,
    val volumes: List<List<Track>>,
) {
    val tracks: List<Track>
        get() = volumes[0]

    val fullTitle: String
        get() = buildString {
            append(title)
            version?.let { append(" (${version})") }
            if (isSingle()) append(" - сингл")
        }

    fun isSingle() = type == "single"
}