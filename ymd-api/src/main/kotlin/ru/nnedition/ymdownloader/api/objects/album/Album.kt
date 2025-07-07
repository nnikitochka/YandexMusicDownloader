package ru.nnedition.ymdownloader.api.objects.album

import com.google.gson.annotations.SerializedName
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta

data class Album(
    val id: Long,
    val title: String,
    val year: Int,
    val coverUri: String,
    val genre: String = "none",
    val artists: List<ArtistMeta>,
    val available: Boolean,
    @SerializedName("volumes")
    val tracks: List<List<Track>>,
)// : AlbumMeta(id, title, year, genre, artists)