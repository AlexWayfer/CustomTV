package name.alexwayfer.customtv.ui.account

import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.trimmedText
import name.alexwayfer.customtv.data.twitchGqlRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

internal data class VodChapter(
    val positionSeconds: Double,
    val category: String,
    val boxArtUrl: String? = null,
)

internal data class VodCategories(
    val chapters: List<VodChapter>,
    val fallbackCategory: String?,
    val fallbackBoxArtUrl: String? = null,
)

internal fun vodCategoryAt(categories: VodCategories, positionSeconds: Double): String? {
    val ordered = categories.chapters
        .filter { it.category.isNotBlank() }
        .sortedBy { it.positionSeconds }
    if (ordered.isEmpty()) return categories.fallbackCategory?.takeIf { it.isNotBlank() }
    val at = positionSeconds.coerceAtLeast(0.0)
    var current = ordered.first().category
    for ((startSeconds, category) in ordered) {
        if (startSeconds > at) break
        current = category
    }
    return current
}

internal fun parseVodCategories(body: String): VodCategories? {
    val video = body.toJsonObjectOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("video")
        ?: return null
    return parseVodCategoriesNode(video)
}

/** The chapters of one `video` node that asked for [VIDEO_CHAPTER_FIELDS]. */
internal fun parseVodCategoriesNode(video: JSONObject): VodCategories {
    val fallback = video.optJSONObject("game")
    val edges = video.optJSONObject("moments")?.optJSONArray("edges")
    val chapters = edges?.objects()?.mapNotNull { it.optJSONObject("node")?.let(::parseVodChapter) }.orEmpty()
    return VodCategories(
        chapters = chapters,
        fallbackCategory = gameName(fallback),
        fallbackBoxArtUrl = boxArt(fallback),
    )
}

/** A chapter named by its game, or by its description when it has none; one without either is dropped. */
private fun parseVodChapter(node: JSONObject): VodChapter? {
    val game = node.optJSONObject("details")?.optJSONObject("game")
    val category = gameName(game) ?: node.trimmedText("description") ?: return null
    val positionSeconds = node.optLong("positionMilliseconds", 0L).coerceAtLeast(0L) / 1_000.0
    return VodChapter(positionSeconds = positionSeconds, category = category, boxArtUrl = boxArt(game))
}

private fun gameName(game: JSONObject?): String? = game?.trimmedText("displayName")

private fun boxArt(game: JSONObject?): String? = game?.text("boxArtURL")

internal class VodChaptersRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10.seconds)
        .readTimeout(10.seconds)
        .build(),
) {
    suspend fun load(videoId: String): VodCategories? {
        if (videoId.isBlank()) return null
        val payload = JSONObject()
            .put("query", CHAPTERS_QUERY)
            .put("variables", JSONObject().put("id", videoId))
        return post(payload)?.let(::parseVodCategories)
    }

    private suspend fun post(payload: JSONObject): String? = withContext(Dispatchers.IO) {
        try {
            val request = twitchGqlRequest(payload.toString()).build()
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql(TAG, "chapters", response.code, body)
                when (response.code) {
                    in HTTP_SUCCESS -> body
                    in HTTP_CLIENT_ERROR -> {
                        AppLog.w(TAG, "chapters rejected HTTP ${response.code}")
                        null
                    }
                    else -> {
                        AppLog.w(TAG, "chapters failed HTTP ${response.code}")
                        null
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppLog.w(TAG, "chapters failed ${error.javaClass.simpleName}")
            null
        }
    }

    private companion object {
        const val TAG = "VodChapters"
        const val CHAPTERS_QUERY = $$"query($id:ID!){video(id:$id){$$VIDEO_CHAPTER_FIELDS}}"
    }
}

/** Chapter fields of a `video` node. Box art is 3:4, the size Twitch serves for category tiles. */
internal const val VIDEO_CHAPTER_FIELDS =
    "game{displayName boxArtURL(width:144,height:192)} " +
        "moments(momentRequestType:VIDEO_CHAPTER_MARKERS){edges{node{positionMilliseconds description " +
        "details{__typename ... on GameChangeMomentDetails{game{displayName boxArtURL(width:144,height:192)}}}}}}"
