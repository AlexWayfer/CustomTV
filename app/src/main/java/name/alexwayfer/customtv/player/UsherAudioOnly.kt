package name.alexwayfer.customtv.player

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException
import name.alexwayfer.customtv.data.HTTP_REDIRECT
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.Request

/**
 * Loads the player's usher master playlist with the audio-only rendition allowed, so the player can switch to sound
 * only in the background. Everything else in the player's own request and answer stays as it was.
 */
internal object UsherAudioOnly {
    /**
     * The answer for [request] when it is a master playlist, or null to let the WebView load it. Any thread.
     * [arrival] is taken before the request and told when an answer that counts as loaded comes.
     */
    fun intercept(request: WebResourceRequest, arrival: () -> () -> Unit): WebResourceResponse? {
        if (request.method != "GET") return null
        val url = usherUrlWithAudioOnly(request.url.toString()) ?: return null
        val arrived = arrival()
        return try {
            val builder = Request.Builder().url(url)
            request.requestHeaders.forEach { (name, value) ->
                // OkHttp unpacks gzip only when it asks for it itself.
                if (!name.equals("Accept-Encoding", ignoreCase = true)) builder.header(name, value)
            }
            val response = sharedHttpClient.newCall(builder.build()).execute()
            // A WebResourceResponse refuses redirect codes and an empty reason; OkHttp follows redirects anyway.
            if (response.code in HTTP_REDIRECT) {
                response.close()
                return null
            }
            AppLog.i(PLAYER_LOG_TAG, "Playlist with sound only allowed, HTTP ${response.code}")
            if (PlayerPageReloads.playlistAnswerCounts(response.code)) arrived()
            Diagnostics.reportHttp(
                "UsherAudioOnly",
                "usher master playlist",
                response.code,
                "",
                PLAYER_SHOWN_PLAYLIST_CODES,
            )
            val contentType = response.header("Content-Type") ?: "application/vnd.apple.mpegurl"
            val headers = response.headers.toMultimap()
                .filterKeys { name -> name.lowercase() !in DROPPED_HEADERS }
                .mapValues { (_, values) -> values.joinToString(", ") }
            WebResourceResponse(
                contentType.substringBefore(';').trim(),
                "utf-8",
                response.code,
                response.message.ifBlank { "OK" },
                headers,
                response.body.byteStream(),
            )
        } catch (error: IOException) {
            AppLog.w(PLAYER_LOG_TAG, "Playlist with sound only failed: ${error.javaClass.simpleName}")
            null
        }
    }

    // The body is already unpacked and its length differs; the type goes in separately.
    private val DROPPED_HEADERS = setOf("content-encoding", "content-length", "content-type")
}
