package ru.nnedition.ymdownloader.api.link

object LinkParser {
    fun parseUrl(url: String): LinkInfo? {
        // Удаление параметров ссылки :>
        val cleanUrl = url.split("?")[0]

        return when {
            cleanUrl.contains("/artist/") -> {
                val artistId = extractId(cleanUrl, "/artist/")
                artistId?.let { LinkInfo(LinkType.ARTIST, it.toLong()) }
            }

            cleanUrl.contains("/album/") -> {
                if (cleanUrl.contains("/track/")) {
                    val albumId = extractId(cleanUrl, "/album/")
                    val trackId = extractId(cleanUrl, "/track/")
                    if (albumId != null && trackId != null) {
                        LinkInfo(LinkType.TRACK, trackId.toLong())
                    } else null
                } else {
                    val albumId = extractId(cleanUrl, "/album/")
                    albumId?.let { LinkInfo(LinkType.ALBUM, it.toLong()) }
                }
            }
            else -> null
        }
    }

    private fun extractId(url: String, pattern: String): String? {
        val startIndex = url.indexOf(pattern)
        if (startIndex == -1) return null

        val idStart = startIndex + pattern.length
        val remaining = url.substring(idStart)

        // Поиск конца ID (до следующего слеша или конца строки)
        val endIndex = remaining.indexOf('/')
        return if (endIndex == -1) {
            remaining.takeIf { it.isNotEmpty() && it.all { char -> char.isDigit() } }
        } else {
            remaining.substring(0, endIndex).takeIf { it.isNotEmpty() && it.all { char -> char.isDigit() } }
        }
    }
}