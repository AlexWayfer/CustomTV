package name.alexwayfer.customtv.data

import okhttp3.Request

/** A Helix request with the user token and the app client id that every Helix endpoint requires. */
internal fun helixRequest(url: String, accessToken: String, clientId: String): Request.Builder =
    Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .header("Client-Id", clientId)
