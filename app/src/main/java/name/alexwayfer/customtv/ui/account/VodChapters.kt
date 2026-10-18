package name.alexwayfer.customtv.ui.account

import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.data.gqlAliasBatches
import name.alexwayfer.customtv.data.gqlErrorWithin
import name.alexwayfer.customtv.data.gqlFailureCause
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

/** A chapter from [positionSeconds] on, in its [category]; [categoryId] opens it, when the chapter has a game. */
internal data class VodChapter(
    val positionSeconds: Double,
    val category: String,
    val boxArtUrl: String? = null,
    val categoryId: String? = null,
)

internal data class VodCategories(
    val chapters: List<VodChapter>,
    val fallbackCategory: String?,
    val fallbackBoxArtUrl: String? = null,
    val fallbackCategoryId: String? = null,
)

/**
 * The category at [positionSeconds]: its chapter's, or the recording's own when it has no chapters. Its ID, which
 * opens it in Explore, is null for a chapter without a game.
 */
internal fun vodChapterCategoryAt(categories: VodCategories, positionSeconds: Double): TwitchCategory? {
    val ordered = categories.chapters
        .filter { it.category.isNotBlank() }
        .sortedBy { it.positionSeconds }
    if (ordered.isEmpty()) {
        val name = categories.fallbackCategory?.takeIf { it.isNotBlank() } ?: return null
        return TwitchCategory(name, categories.fallbackBoxArtUrl, categories.fallbackCategoryId)
    }
    val at = positionSeconds.coerceAtLeast(0.0)
    var current = ordered.first()
    for (chapter in ordered) {
        if (chapter.positionSeconds > at) break
        current = chapter
    }
    return TwitchCategory(current.category, current.boxArtUrl, current.categoryId)
}

/**
 * The chapters of each of [videoIds], asked with [vodChaptersQuery] in their order. A recording that is
 * missing or that an error reached is left out: its chapters did not load, which is not the same as none.
 */
internal fun parseVodCategoriesBatch(body: String, videoIds: List<String>): Map<String, VodCategories> {
    val json = body.toJsonObjectOrNull() ?: return emptyMap()
    val data = json.optJSONObject("data") ?: return emptyMap()
    return videoIds.withIndex().mapNotNull { (index, id) ->
        val alias = vodChaptersAlias(index)
        data.optJSONObject(alias)
            ?.takeUnless { gqlErrorWithin(json, listOf(alias)) }
            ?.let(::parseVodCategoriesNode)
            ?.let { id to it }
    }.toMap()
}

/**
 * The chapters of one `video` node that asked for [VIDEO_CHAPTER_FIELDS]. A recording without chapters
 * answers an empty list; without the list Twitch failed on them, so they are unknown, not empty.
 */
internal fun parseVodCategoriesNode(video: JSONObject): VodCategories? {
    val fallback = video.optJSONObject("game")
    val edges = video.optJSONObject("moments")?.optJSONArray("edges") ?: return null
    val chapters = edges.objects().mapNotNull { it.optJSONObject("node")?.let(::parseVodChapter) }
    return VodCategories(
        chapters = chapters,
        fallbackCategory = gameName(fallback),
        fallbackBoxArtUrl = boxArt(fallback),
        fallbackCategoryId = fallback?.trimmedText("id"),
    )
}

/** A chapter named by its game, or by its description when it has none; one without either is dropped. */
private fun parseVodChapter(node: JSONObject): VodChapter? {
    val game = node.optJSONObject("details")?.optJSONObject("game")
    val category = gameName(game) ?: node.trimmedText("description") ?: return null
    val positionSeconds = node.optLong("positionMilliseconds", 0L).coerceAtLeast(0L) / 1_000.0
    return VodChapter(
        positionSeconds = positionSeconds,
        category = category,
        boxArtUrl = boxArt(game),
        categoryId = game?.trimmedText("id"),
    )
}

private fun gameName(game: JSONObject?): String? = game?.trimmedText("displayName")

private fun boxArt(game: JSONObject?): String? = game?.text("boxArtURL")

internal class VodChaptersRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10.seconds)
        .readTimeout(10.seconds)
        .build(),
) {
    /** The chapters of [videoId]; null when they did not load. */
    suspend fun load(videoId: String): VodCategories? = loadMany(listOf(videoId))[videoId]

    /** The chapters of each of [videoIds] that loaded, asked a batch of aliases at a time. */
    suspend fun loadMany(videoIds: Collection<String>): Map<String, VodCategories> {
        val ids = videoIds.filter { it.isNotBlank() }.distinct()
        return gqlAliasBatches(ids).fold(emptyMap()) { loaded, batch ->
            val variables = JSONObject()
            batch.forEachIndexed { index, id -> variables.put(vodChaptersAlias(index), id) }
            val payload = JSONObject()
                .put("query", vodChaptersQuery(batch.size))
                .put("variables", variables)
            val body = post(payload) ?: return@fold loaded
            val parsed = parseVodCategoriesBatch(body, batch)
            val failed = batch.size - parsed.size
            if (failed > 0) AppLog.w(TAG, "chapters failed for $failed of ${batch.size}: ${gqlFailureCause(body)}")
            loaded + parsed
        }
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
    }
}

/** The alias and the variable of the recording at [index] in a [vodChaptersQuery]. */
internal fun vodChaptersAlias(index: Int): String = "v$index"

/** Asks the chapters of [count] recordings, each by its id under its [vodChaptersAlias]. */
internal fun vodChaptersQuery(count: Int): String {
    val aliases = (0 until count).map(::vodChaptersAlias)
    val variables = aliases.joinToString(",") { alias -> $$"$$$alias:ID!" }
    val videos = aliases.joinToString(" ") { alias -> $$"$$alias:video(id:$$$alias){$$VIDEO_CHAPTER_FIELDS}" }
    return "query($variables){$videos}"
}

/** Chapter fields of a `video` node. Box art is 3:4, the size Twitch serves for category tiles. */
internal const val VIDEO_CHAPTER_FIELDS =
    "game{id displayName boxArtURL(width:144,height:192)} " +
        "moments(momentRequestType:VIDEO_CHAPTER_MARKERS){edges{node{positionMilliseconds description " +
        "details{__typename ... on GameChangeMomentDetails{game{id displayName boxArtURL(width:144,height:192)}}}}}}"
