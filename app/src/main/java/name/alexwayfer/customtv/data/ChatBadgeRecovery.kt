package name.alexwayfer.customtv.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import name.alexwayfer.customtv.diagnostics.AppLog

internal const val CHAT_BADGES_TAG = "ChatBadges"

/** Publishes partial lists while temporary failures recover, and stops once no request needs another try. */
internal suspend fun ChatBadgeLoader.keepLoaded(login: String, publish: (ChannelBadgeUrls) -> Unit) {
    val key = login.lowercase()
    var attempt = 1
    while (true) {
        currentCoroutineContext().ensureActive()
        val result = loadChannelOnce(key)
        currentCoroutineContext().ensureActive()
        publish(result.urls)
        if (!result.retryable) return
        val pause = EmoteHttp.backoff(attempt)
        AppLog.i(CHAT_BADGES_TAG, "retry $key in ${pause.inWholeMilliseconds}ms")
        delay(pause)
        if (attempt < Int.MAX_VALUE) attempt++
    }
}
