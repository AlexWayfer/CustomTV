package name.alexwayfer.customtv.links

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.gqlFailureCause
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import name.alexwayfer.customtv.data.trimmedText
import name.alexwayfer.customtv.data.twitchGqlRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.ui.account.TwitchVideo
import name.alexwayfer.customtv.ui.account.parseTwitchVideo
import org.json.JSONObject

/** A recording a twitch.tv link leads to, and the login of the channel it belongs to. */
internal data class LinkedVideo(val video: TwitchVideo, val channelLogin: String)

/** Loads the recording of a `/videos/<id>` link through public GQL, so it opens without a login. */
internal object LinkedVideos {
    /** Null when the recording is gone or hidden, or Twitch did not answer or refused the request. */
    suspend fun load(id: String): LinkedVideo? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("query", VIDEO_QUERY)
                .put("variables", JSONObject().put("id", id))
            val request = twitchGqlRequest(payload.toString()).build()
            sharedHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql(TWITCH_LINKS_TAG, "video", response.code, body)
                when (response.code) {
                    in HTTP_SUCCESS -> parseLinkedVideo(body).also { video ->
                        if (video == null) AppLog.w(TWITCH_LINKS_TAG, "video unreadable: ${gqlFailureCause(body)}")
                    }
                    in HTTP_CLIENT_ERROR -> {
                        AppLog.w(TWITCH_LINKS_TAG, "video rejected HTTP ${response.code}")
                        null
                    }
                    else -> {
                        AppLog.w(TWITCH_LINKS_TAG, "video failed HTTP ${response.code}")
                        null
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppLog.w(TWITCH_LINKS_TAG, "video failed ${error.javaClass.simpleName}")
            null
        }
    }

    private const val VIDEO_QUERY =
        $$"query($id:ID!){video(id:$id){" +
            "id title previewThumbnailURL(width:640,height:360) createdAt publishedAt viewCount lengthSeconds " +
            "owner{login}}}"
}

/** A recording that is gone, or whose channel is not named, cannot open. */
internal fun parseLinkedVideo(body: String): LinkedVideo? {
    val node = body.toJsonObjectOrNull()?.optJSONObject("data")?.optJSONObject("video") ?: return null
    val login = node.optJSONObject("owner")?.trimmedText("login") ?: return null
    val video = parseTwitchVideo(node) ?: return null
    return LinkedVideo(video, login)
}
