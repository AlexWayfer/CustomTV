package name.alexwayfer.customtv.player

import android.content.res.AssetManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException
import java.net.HttpURLConnection.HTTP_OK
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request

/** Applies the buffer cleanup correction before the player worker executes. */
internal class TwitchWorkerInterceptor(private val http: OkHttpClient = sharedHttpClient) {
    // A worker asset is content-addressed. Keep its corrected body across page reloads in this player.
    private var cached: WorkerScriptResponse? = null

    fun intercept(request: WebResourceRequest, assets: AssetManager): WebResourceResponse? {
        val url = request.url.toString()
        if (request.method != "GET" || !isTwitchWorkerScript(url)) return null
        val script = synchronized(this) {
            cached?.takeIf { it.url == url } ?: download(request, assets)?.also { cached = it }
        } ?: return null
        return WebResourceResponse(
            "application/javascript", "utf-8", HTTP_OK, "OK", script.headers, script.body.inputStream(),
        )
    }

    private fun download(request: WebResourceRequest, assets: AssetManager): WorkerScriptResponse? = try {
        val builder = Request.Builder().url(request.url.toString())
        request.requestHeaders.forEach { (name, value) ->
            // Read a complete decoded body even when the WebView has a different cached representation.
            if (name.lowercase() !in DROPPED_REQUEST_HEADERS) builder.header(name, value)
        }
        http.newCall(builder.build()).execute().use { response ->
            if (response.code != HTTP_OK || !isTwitchWorkerScript(response.request.url.toString())) {
                AppLog.w(PLAYER_LOG_TAG, "Worker buffer cleanup load failed: HTTP ${response.code}")
                return null
            }
            val bytes = response.peekBody(MAX_WORKER_SCRIPT_BYTES + 1).bytes()
            if (bytes.size > MAX_WORKER_SCRIPT_BYTES) {
                AppLog.w(PLAYER_LOG_TAG, "Worker buffer cleanup load failed: script exceeds size limit")
                return null
            }
            val charset = response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val source = bytes.toString(charset)
            val cleanup = assets.open("player/worker-buffer-cleanup.js").bufferedReader().use { it.readText() }
            val patched = patchTwitchWorkerScript(source, cleanup)
            if (patched == null) {
                AppLog.w(PLAYER_LOG_TAG, "Worker buffer cleanup not applied: SDK removal function not recognized")
            } else {
                AppLog.i(PLAYER_LOG_TAG, "Worker buffer cleanup applied")
            }
            val headers = response.headers.toMultimap()
                .filterKeys { it.lowercase() !in DROPPED_RESPONSE_HEADERS }
                .mapValues { (_, values) -> values.joinToString(", ") } + ("Cache-Control" to "no-store")
            WorkerScriptResponse(
                request.url.toString(), (patched ?: source).toByteArray(Charsets.UTF_8), headers,
            )
        }
    } catch (error: IOException) {
        AppLog.w(PLAYER_LOG_TAG, "Worker buffer cleanup load failed: ${error.javaClass.simpleName}")
        null
    }

    private class WorkerScriptResponse(val url: String, val body: ByteArray, val headers: Map<String, String>)

    private companion object {
        const val MAX_WORKER_SCRIPT_BYTES = 1024 * 1024L
        val DROPPED_REQUEST_HEADERS = setOf(
            "accept-encoding", "if-none-match", "if-modified-since", "if-range", "range",
        )
        val DROPPED_RESPONSE_HEADERS = setOf(
            "content-encoding", "content-length", "content-type", "content-range", "accept-ranges", "transfer-encoding",
            "etag", "last-modified", "content-md5", "digest", "cache-control",
        )
    }
}
