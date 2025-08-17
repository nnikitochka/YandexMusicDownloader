package ru.nnedition.ymdownloader.api.download

import ru.nnedition.ymdownloader.api.config.IConfiguration

interface IMusicDownloader {
    var config: IConfiguration

    fun downloadArtist(artistId: Long, config: IConfiguration = this.config)

    fun downloadAlbum(albumId: Long, config: IConfiguration = this.config)

    fun downloadTrack(trackId: Long, config: IConfiguration = this.config)
}