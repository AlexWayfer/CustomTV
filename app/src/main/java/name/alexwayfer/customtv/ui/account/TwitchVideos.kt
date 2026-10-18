package name.alexwayfer.customtv.ui.account

import java.io.IOException
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelProfileParser
import name.alexwayfer.customtv.data.GqlRetryPolicy
import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.gqlErrorWithin
import name.alexwayfer.customtv.data.gqlFailureCause
import name.alexwayfer.customtv.data.hasTransientGqlError
import name.alexwayfer.customtv.data.instantMillisOrNull
import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import name.alexwayfer.customtv.data.trimmedText
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
 * channels its streamer suggests on the channel's home, in the streamer's order. [chaptersFailed] holds
 * the recordings whose chapters did not load; they have no entry in [chapters].
 */
internal data class ChannelRecordingsPage(
    val videos: List<TwitchVideo>,
    val chapters: Map<String, VodCategories>,
    val suggestedChannels: List<ChannelProfile> = emptyList(),
    val chaptersFailed: Set<String> = emptySet(),
)

/**
 * Loads a channel's recordings, their chapters, and its suggested channels in one public GQL
 * request, so they show without a login. The chapters come in their own alias: Twitch fails on them
 * far more often than on the recordings, and then fails the whole field it was asked inside.
 */
internal class ChannelRecordingsRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10.seconds)
        .readTimeout(10.seconds)
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
                val request = twitchGqlRequest(payload.toString(), RecordingsRetry).build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    Diagnostics.reportGql(TAG, "videos", response.code, body)
                    when (response.code) {
                        in HTTP_SUCCESS -> parseChannelRecordings(body).also { page ->
                            when {
                                page == null -> AppLog.w(TAG, "videos unreadable: ${gqlFailureCause(body)}")
                                page.videos.isEmpty() -> AppLog.i(TAG, "videos empty for channel $channelLogin")
                                page.chaptersFailed.isNotEmpty() -> AppLog.w(
                                    TAG,
                                    "chapters failed for ${page.chaptersFailed.size} of ${page.videos.size}: " +
                                        gqlFailureCause(body),
                                )
                            }
                        }
                        in HTTP_CLIENT_ERROR -> {
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
        const val RECORDINGS_ARGUMENTS = "first:20,type:ARCHIVE,sort:TIME"
        const val RECORDINGS_QUERY =
            $$"query($login:String!){user(login:$login){" +
                "videos($RECORDINGS_ARGUMENTS){edges{node{" +
                "id title previewThumbnailURL(width:640,height:360) createdAt publishedAt viewCount lengthSeconds" +
                "}}}" +
                "chapters:videos($RECORDINGS_ARGUMENTS){edges{node{id $VIDEO_CHAPTER_FIELDS}}}" +
                "channel{home{shelves{streamerShelf(first:15){edges{node{" +
                "id login displayName profileImageURL(width:300) stream{title createdAt viewersCount game{name}}" +
                "}}}}}}}}"

        val RecordingsRetry = GqlRetryPolicy(::recordingsRetried)
    }
}

/** The request goes again only when Twitch's server failure took the recordings; chapters are asked apart. */
internal fun recordingsRetried(body: String): Boolean =
    hasTransientGqlError(body) && parseChannelRecordings(body) == null

/**
 * A channel that does not exist has no recordings. An answer without data, or one whose errors
 * left out the videos, could not be read.
 */
internal fun parseChannelRecordings(body: String): ChannelRecordingsPage? {
    val json = body.toJsonObjectOrNull() ?: return null
    val data = json.optJSONObject("data") ?: return null
    val user = data.optJSONObject("user")
    val edges = user?.optJSONObject("videos")?.optJSONArray("edges")
    if (edges == null) {
        val failed = (json.optJSONArray("errors")?.length() ?: 0) > 0
        return if (failed) null else ChannelRecordingsPage(emptyList(), emptyMap())
    }
    val videos = edges.objects().mapNotNull { it.optJSONObject("node")?.let(::parseTwitchVideo) }
    val chapters = recordingChapters(json, user)
    return ChannelRecordingsPage(
        videos = videos,
        chapters = chapters,
        suggestedChannels = suggestedChannels(user),
        chaptersFailed = videos.map { it.id }.filterNot(chapters::containsKey).toSet(),
    )
}

internal fun parseTwitchVideo(node: JSONObject): TwitchVideo? {
    val id = node.trimmedText("id") ?: return null
    val publishedAt = instant(node, "publishedAt")
    return TwitchVideo(
        id = id,
        title = node.trimmedText("title").orEmpty(),
        thumbnailUrl = node.trimmedText("previewThumbnailURL"),
        startedAtMillis = instant(node, "createdAt") ?: publishedAt,
        publishedAtMillis = publishedAt,
        viewCount = node.optLong("viewCount").coerceAtLeast(0L),
        durationSeconds = node.optLong("lengthSeconds").coerceAtLeast(0L),
    )
}

/** The chapters of each recording in the `chapters` alias that an error did not reach, by video id. */
private fun recordingChapters(json: JSONObject, user: JSONObject): Map<String, VodCategories> {
    val edges = user.optJSONObject("chapters")?.optJSONArray("edges") ?: return emptyMap()
    return (0 until edges.length()).mapNotNull { index ->
        val node = edges.optJSONObject(index)?.optJSONObject("node")
            ?.takeUnless { gqlErrorWithin(json, listOf("user", "chapters", "edges", index)) }
        val id = node?.trimmedText("id")
        val categories = node?.let(::parseVodCategoriesNode)
        if (id == null || categories == null) null else id to categories
    }.toMap()
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
        val login = node.trimmedText("login") ?: return@mapNotNull null
        ChannelProfileParser.parseUser(node, login).profile
    }
}

private fun instant(json: JSONObject, key: String): Long? =
    json.trimmedText(key)?.let(::instantMillisOrNull)

internal fun twitchVideoDurationLabel(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3_600L
    val minutes = (safe % 3_600L) / 60L
    val remainingSeconds = safe % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, remainingSeconds)
    } else {
        "%d:%02d".format(Locale.ROOT, minutes, remainingSeconds)
    }
}

private const val TAG = "TwitchVideos"
