package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException

/** A public list without account headers, used when Twitch cannot answer without its experimental login. */
internal suspend fun loadPublicChatModerators(
    channelLogin: String,
): List<ChatModerator>? = withContext(Dispatchers.IO) {
    val url = "https://api.ivr.fi/v2/twitch/modvip".toHttpUrl().newBuilder().addPathSegment(channelLogin).build()
    val request = Request.Builder().url(url).build()
    AppLog.i(TAG, "loading public moderators for $channelLogin")
    try {
        sharedHttpClient.newCall(request).executeAsync().use { response ->
            val body = response.body.string()
            Diagnostics.reportHttp(TAG, "ivr twitch/modvip", response.code, "", setOf(HTTP_NOT_FOUND))
            if (!response.isSuccessful) {
                AppLog.w(TAG, "public moderators failed HTTP ${response.code}")
                return@withContext null
            }
            parsePublicChatModerators(body).also {
                if (it == null) AppLog.w(TAG, "public moderators unreadable")
            }
        }
    } catch (error: IOException) {
        AppLog.w(TAG, "public moderators failed ${error.javaClass.simpleName}")
        null
    }
}

private const val TAG = "ChatModerators"
