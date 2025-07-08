package ru.nnedition.ymdownloader.api.client

import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import com.google.gson.Gson
import com.google.gson.JsonParser
import ru.nnedition.ymdownloader.api.objects.UserInfo
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import ru.nnedition.ymdownloader.api.config.Config
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class YandexMusicClient private constructor(
    private val client: OkHttpClient,
    private val oauthToken: String,
    var login: String
) {
    fun getUserInfo(): UserInfo {
        val request = Request.Builder()
            .url("$API_URL/account/about")
            .addHeader("Authorization", oauthToken)
            .build()

        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: error("Empty response")
            gson.fromJson(JsonParser.parseString(json).asJsonObject["result"], UserInfo::class.java)
        }
    }

    companion object {
        private val gson = Gson()

        private const val API_URL = "https://api.music.yandex.net"
        private const val YANDEX_USER_AGENT = "YandexMusicDesktopAppWindows/5.54.0"
        private const val SECRET = "kzqU4XhfCaY6B6JTHODeq5"

        fun create(config: Config) = create(config.token)
        fun create(token: String): YandexMusicClient {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val requestWithUserAgent = originalRequest.newBuilder()
                        .header("User-Agent", YANDEX_USER_AGENT)
                        .build()
                    chain.proceed(requestWithUserAgent)
                }.build()

            val oauthToken = "OAuth $token"

            val yandexClient = YandexMusicClient(
                client = client,
                oauthToken = oauthToken,
                login = ""
            )

            val userInfo = yandexClient.getUserInfo()
            if (!userInfo.hasPlus)
                throw IllegalStateException("active plus subscription required")

            yandexClient.login = userInfo.login

            return yandexClient
        }
    }


    fun getArtist(artistId: Long): ArtistMetaResult {
        val request = Request.Builder()
            .url("$API_URL/artists/$artistId/")
            .addHeader("Authorization", oauthToken)
            .addHeader("X-Yandex-Music-Client", YANDEX_USER_AGENT)
            .build()

        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: error("Empty response")
            gson.fromJson(JsonParser.parseString(json).asJsonObject["result"], ArtistMetaResult::class.java)
        }
    }


    fun getAlbum(albumId: Long): Album {
        val request = Request.Builder()
            .url("$API_URL/albums/$albumId/with-tracks")
            .addHeader("Authorization", oauthToken)
            .build()

        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: error("Empty response")
            gson.fromJson(JsonParser.parseString(json).asJsonObject["result"], Album::class.java)
        }
    }


    fun getTrack(trackId: Long): Track {
        val request = Request.Builder()
            .url("$API_URL/tracks/$trackId")
            .addHeader("Authorization", oauthToken)
            .build()

        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: error("Empty response")
            gson.fromJson(JsonParser.parseString(json).asJsonObject["result"], Array<Track>::class.java)[0]
        }
    }

    @Throws(Exception::class)
    private fun createSignature(ts: String, trackId: String, quality: String): String {
        val msg = "${ts}${trackId}${quality}flacaache-aacmp3flac-mp4aac-mp4he-aac-mp4encraw"

        val secretKeySpec = SecretKeySpec(SECRET.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)

        val hmacBytes = mac.doFinal(msg.toByteArray())
        val base64Encoded = Base64.getEncoder().encodeToString(hmacBytes)

        return base64Encoded.substring(0, base64Encoded.length - 1)
    }

    private fun getUnixTimestamp(): String {
        return Instant.now().epochSecond.toString()
    }

    @Throws(IOException::class, Exception::class)
    fun getFileInfo(trackId: String, quality: String): DownloadInfo {
        val url = "$API_URL/get-file-info"

        val ts = getUnixTimestamp()
        val signature = createSignature(ts, trackId, quality)

        val httpUrl = url.toHttpUrl().newBuilder()
            .addQueryParameter("ts", ts)
            .addQueryParameter("trackId", trackId)
            .addQueryParameter("quality", quality)
            .addQueryParameter("codecs", "flac,aac,he-aac,mp3,flac-mp4,aac-mp4,he-aac-mp4")
            .addQueryParameter("transports", "encraw")
            .addQueryParameter("sign", signature)
            .build()

        val request = Request.Builder()
            .url(httpUrl)
            .addHeader("Authorization", oauthToken)
            .addHeader("X-Yandex-Music-Client", YANDEX_USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}")
            }

            val json = response.body?.string()
                ?: throw IOException("Response body is null")

            return gson.fromJson(
                JsonParser.parseString(json).asJsonObject["result"].asJsonObject["downloadInfo"],
                DownloadInfo::class.java
            )
        }
    }

    fun getCoverData(
        url: String,
        original: Boolean,
        withRange: Boolean,
        quality: String = "800x800"
    ): ByteArray {
        val toReplace = if (original) "/orig" else "/$quality"
        val replacedUrl = url.replace("/%%", toReplace)
        val fullUrl = "https://$replacedUrl"

        val request = Request.Builder()
            .url(fullUrl)

        if (withRange)
            request.addHeader("Range", "bytes=0-")

        val response = client.newCall(request.build()).execute()

        if (!response.isSuccessful)
            throw IOException("HTTP error: ${response.code}")

        response.use { resp ->
            return resp.body?.bytes() ?: throw IOException("Empty response body")
        }
    }
}