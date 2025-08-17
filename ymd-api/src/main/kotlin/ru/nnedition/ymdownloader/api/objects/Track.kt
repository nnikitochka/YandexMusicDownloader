package ru.nnedition.ymdownloader.api.objects

import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta

data class Track(
    val id: Long,
    val title: String,
    val version: String?,
    val available: Boolean,
    val artists: List<ArtistMeta>,
    val albums: List<Album>,
    val coverUri: String,
    val lyricsAvailable: Boolean,
    val genre: String?,
) {
    private lateinit var _album: Album
    // Взаимодействие с данным полем без его ручной инициализации невозможно!
    var album: Album = _album
        set(value) {
            if (::_album.isInitialized) return
            _album = value
            field = value
        }
        get() = _album

    val num: String
        get() = (album.tracks.indexOf(album.tracks.find { it.id == id })+1).toString()

    val publisher: ArtistMeta
        get() = artists[0]

    val fullTitle: String
        get() = buildString {
            append(title)
            version?.let { append(" ($it)") }
        }
}