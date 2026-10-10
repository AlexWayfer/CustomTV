package name.alexwayfer.customtv.player

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.Request

/**
 * Loads the player's usher master playlist with the audio-only rendition allowed, so the player can switch to sound
 * only in the background. Everything else in the player's own request and answer stays as it was.
 */
internal object UsherAudioOnly {
    /** The answer for [request] when it is a master playlist, or null to let the WebView load it. Any thread. */
    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (request.method != "GET") return null
        val url = usherUrlWithAudioOnly(request.url.toString()) ?: return null
        return try {
            val builder = Request.Builder().url(url)
            request.requestHeaders.forEach { (name, value) ->
                // OkHttp unpacks gzip only when it asks for it itself.
                if (!name.equals("Accept-Encoding", ignoreCase = true)) builder.header(name, value)
            }
            val response = sharedHttpClient.newCall(builder.build()).execute()
            // A WebResourceResponse refuses redirect codes and an empty reason; OkHttp follows redirects anyway.
            if (response.code in 300..399) {
                response.close()
                return null
            }
            AppLog.i(PLAYER_LOG_TAG, "Playlist with sound only allowed, HTTP ${response.code}")
            // 403 and 404 are routine: a subscriber-only or ended stream; the player shows those itself.
            Diagnostics.reportHttp("UsherAudioOnly", "usher master playlist", response.code, "", setOf(403, 404))
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
