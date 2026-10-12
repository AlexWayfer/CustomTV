package name.alexwayfer.customtv.chat

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

internal fun socketTaskAllowed(closedByUser: Boolean, executorShutdown: Boolean): Boolean {
    return !closedByUser && !executorShutdown
}

/** A client for a socket that stays open and waits for pushes: no read timeout, a ping keeps it alive. */
internal fun pushSocketHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .pingInterval(30.seconds)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .build()
