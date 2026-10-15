package name.alexwayfer.customtv.data

import okhttp3.OkHttpClient

/**
 * The base for every request/response client: they share one connection pool and dispatcher, so
 * calls to the same host reuse connections. Each caller sets its own timeouts with `newBuilder()`.
 * WebSocket clients keep their own client, because an open socket stays for the whole session.
 * Every client built from it sends a GQL read again when Twitch's server failed on it ([GqlTransientRetry]).
 */
internal val sharedHttpClient: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(GqlTransientRetry)
    .build()
