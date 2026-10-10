package name.alexwayfer.customtv.ui.account

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.twitchGqlRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
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
    for ((positionSeconds, category) in ordered) {
        if (positionSeconds > at) break
        current = category
    }
    return current
}

internal fun parseVodCategories(body: String): VodCategories? {
    val video = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("video")
        ?: return null
    return parseVodCategoriesNode(video)
}

/** The chapters of one `video` node that asked for [VIDEO_CHAPTER_FIELDS]. */
internal fun parseVodCategoriesNode(video: JSONObject): VodCategories {
    val fallback = video.optJSONObject("game")
    val edges = video.optJSONObject("moments")?.optJSONArray("edges")
    val chapters = buildList {
        if (edges == null) return@buildList
        for (index in 0 until edges.length()) {
            val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
            val game = node.optJSONObject("details")?.optJSONObject("game")
            val category = gameName(game)
                ?: node.optString("description").trim().takeIf { it.isNotEmpty() && it != "null" }
                ?: continue
            val positionSeconds = node.optLong("positionMilliseconds", 0L).coerceAtLeast(0L) / 1_000.0
            add(VodChapter(positionSeconds = positionSeconds, category = category, boxArtUrl = boxArt(game)))
        }
    }
    return VodCategories(
        chapters = chapters,
        fallbackCategory = gameName(fallback),
        fallbackBoxArtUrl = boxArt(fallback),
    )
}

private fun gameName(game: JSONObject?): String? =
    game?.optString("displayName")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != "null" }

private fun boxArt(game: JSONObject?): String? =
    game?.optString("boxArtURL")
        ?.takeIf { it.isNotBlank() && it != "null" }

internal class VodChaptersRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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
                    in 200..299 -> body
                    in 400..499 -> {
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
