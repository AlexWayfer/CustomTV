package name.alexwayfer.customtv.ui.account

import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelProfileParser
import name.alexwayfer.customtv.data.gqlFailureCause
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.twitchGqlRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
import org.json.JSONObject

internal data class TwitchVideo(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val startedAtMillis: Long?,
    val publishedAtMillis: Long?,
    val viewCount: Long,
    val durationSeconds: Long,
)

/**
 * A channel's recent past broadcasts, newest first, with each one's chapters by video id, and the
 * channels its streamer suggests on the channel's home, in the streamer's order.
 */
internal data class ChannelRecordingsPage(
    val videos: List<TwitchVideo>,
    val chapters: Map<String, VodCategories>,
    val suggestedChannels: List<ChannelProfile> = emptyList(),
)

/**
 * Loads a channel's recordings, their chapters, and its suggested channels in one public GQL
 * request, so they show without a login.
 */
internal class ChannelRecordingsRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    /** Null when Twitch did not answer or refused the request. */
    suspend fun load(channelLogin: String): ChannelRecordingsPage? {
        if (channelLogin.isBlank()) {
            AppLog.w(TAG, "videos skipped: no channel login")
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("query", RECORDINGS_QUERY)
                    .put("variables", JSONObject().put("login", channelLogin))
                val request = twitchGqlRequest(payload.toString()).build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    Diagnostics.reportGql(TAG, "videos", response.code, body)
                    when (response.code) {
                        in 200..299 -> parseChannelRecordings(body).also { page ->
                            when {
                                page == null -> AppLog.w(TAG, "videos unreadable: ${gqlFailureCause(body)}")
                                page.videos.isEmpty() -> AppLog.i(TAG, "videos empty for channel $channelLogin")
                            }
                        }
                        in 400..499 -> {
                            AppLog.w(TAG, "videos rejected HTTP ${response.code}")
                            null
                        }
                        else -> {
                            AppLog.w(TAG, "videos failed HTTP ${response.code}")
                            null
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "videos failed ${error.javaClass.simpleName}")
                null
            }
        }
    }

    private companion object {
        const val RECORDINGS_QUERY =
            $$"query($login:String!){user(login:$login){videos(first:20,type:ARCHIVE,sort:TIME){edges{node{" +
                "id title previewThumbnailURL(width:640,height:360) createdAt publishedAt viewCount lengthSeconds " +
                "$VIDEO_CHAPTER_FIELDS}}}" +
                "channel{home{shelves{streamerShelf(first:15){edges{node{" +
                "id login displayName profileImageURL(width:300) stream{title createdAt viewersCount game{name}}" +
                "}}}}}}}}"
    }
}

/**
 * A channel that does not exist has no recordings. An answer without data, or one whose errors
 * left out the videos, could not be read.
 */
internal fun parseChannelRecordings(body: String): ChannelRecordingsPage? {
    val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val data = json.optJSONObject("data") ?: return null
    val user = data.optJSONObject("user")
    val edges = user?.optJSONObject("videos")?.optJSONArray("edges")
    if (edges == null) {
        val failed = (json.optJSONArray("errors")?.length() ?: 0) > 0
        return if (failed) null else ChannelRecordingsPage(emptyList(), emptyMap())
    }
    val videos = mutableListOf<TwitchVideo>()
    val chapters = mutableMapOf<String, VodCategories>()
    for (index in 0 until edges.length()) {
        val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
        val id = text(node, "id") ?: continue
        val publishedAt = instant(node, "publishedAt")
        videos += TwitchVideo(
            id = id,
            title = text(node, "title").orEmpty(),
            thumbnailUrl = text(node, "previewThumbnailURL"),
            startedAtMillis = instant(node, "createdAt") ?: publishedAt,
            publishedAtMillis = publishedAt,
            viewCount = node.optLong("viewCount").coerceAtLeast(0L),
            durationSeconds = node.optLong("lengthSeconds").coerceAtLeast(0L),
        )
        chapters[id] = parseVodCategoriesNode(node)
    }
    return ChannelRecordingsPage(videos, chapters, suggestedChannels(user))
}

/** The streamer's suggested channels; a shelf that is missing or failed suggests none. */
private fun suggestedChannels(user: JSONObject?): List<ChannelProfile> {
    val edges = user?.optJSONObject("channel")
        ?.optJSONObject("home")
        ?.optJSONObject("shelves")
        ?.optJSONObject("streamerShelf")
        ?.optJSONArray("edges")
        ?: return emptyList()
    return (0 until edges.length()).mapNotNull { index ->
        val node = edges.optJSONObject(index)?.optJSONObject("node") ?: return@mapNotNull null
        val login = text(node, "login") ?: return@mapNotNull null
        ChannelProfileParser.parseUser(node, login).profile
    }
}

private fun text(json: JSONObject, key: String): String? =
    json.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }

private fun instant(json: JSONObject, key: String): Long? =
    text(json, key)?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

internal fun twitchVideoDurationLabel(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3_600L
    val minutes = (safe % 3_600L) / 60L
    val remainingSeconds = safe % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%d:%02d".format(minutes, remainingSeconds)
    }
}

private const val TAG = "TwitchVideos"
