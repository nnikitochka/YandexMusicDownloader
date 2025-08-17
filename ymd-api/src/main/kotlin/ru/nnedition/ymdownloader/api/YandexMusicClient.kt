package ru.nnedition.ymdownloader.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import ru.nnedition.ymdownloader.api.config.IConfiguration
import ru.nnedition.ymdownloader.api.objects.DownloadInfo
import ru.nnedition.ymdownloader.api.objects.Track
import ru.nnedition.ymdownloader.api.objects.UserInfo
import ru.nnedition.ymdownloader.api.objects.album.Album
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMeta
import ru.nnedition.ymdownloader.api.objects.artist.ArtistMetaResult
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class YandexMusicClient private constructor(
    private val client: OkHttpClient,
    private val oauthToken: String,
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
        private val gson = GsonBuilder()
            .registerTypeAdapter(ArtistMeta::class.java, ArtistMeta.Deserializer())
            .create()

        private const val API_URL = "https://api.music.yandex.net"
        private const val YANDEX_USER_AGENT = "YandexMusicDesktopAppWindows/5.54.0"
        private const val SECRET = "kzqU4XhfCaY6B6JTHODeq5"

        fun validateToken(token: String): Result<Boolean> {
            return try {
                val request = Request.Builder()
                    .url("https://api.music.yandex.net/account/status")
                    .get()
                    .addHeader("Authorization", "OAuth $token")
                    .build()

                val response = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build().newCall(request).execute()

                response.body?.close()

                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun create(config: IConfiguration) = create(config.token)
        fun create(token: String): YandexMusicClient {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
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
            )

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
                throw okio.IOException("Unexpected response code: ${response.code}")
            }

            val json = response.body?.string()
                ?: throw okio.IOException("Response body is null")

            return gson.fromJson(
                JsonParser.parseString(json).asJsonObject["result"].asJsonObject["downloadInfo"],
                DownloadInfo::class.java
            )
        }
    }

    val covers: MutableMap<String, ByteArray> = HashMap()
    fun getCoverData(
        url: String,
        original: Boolean,
        withRange: Boolean,
        // Максимально доступное разрешение
        quality: String = "800x800"
    ): ByteArray {
        val fullUrl = "https://${url.replace("/%%", if (original) "/orig" else "/$quality")}"

        return covers.getOrPut(fullUrl) {
            val request = Request.Builder()
                .url(fullUrl)
                .apply { if (withRange) addHeader("Range", "bytes=0-") }
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    throw okio.IOException("HTTP error: ${response.code}")
                response.body?.bytes() ?: throw okio.IOException("Empty response body")
            }
        }
    }
}