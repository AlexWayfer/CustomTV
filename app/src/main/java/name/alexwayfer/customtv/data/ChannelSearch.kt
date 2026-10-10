package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.channel.isValidChannelLogin
import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val MIN_QUERY_LENGTH = 2

data class ChannelSearchHit(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val isLive: Boolean,
    /** The channel's user ID when the search gave it; a login typed without a match has none. */
    val userId: String? = null,
)

fun channelSearchQuery(raw: String): String? {
    val normalized = normalizeChannelInput(raw)
    return normalized.takeIf { it.length >= MIN_QUERY_LENGTH }
}

fun channelSearchExtras(
    historyMatches: List<String>,
    remote: List<ChannelSearchHit>,
): List<ChannelSearchHit> {
    val seen = historyMatches.mapTo(HashSet()) { it.lowercase() }
    return remote.filter { it.login.lowercase() !in seen }
}

fun parseChannelSearchSuggestions(body: String): List<ChannelSearchHit> {
    val edges = JSONObject(body)
        .optJSONObject("data")
        ?.optJSONObject("searchSuggestions")
        ?.optJSONArray("edges")
        ?: return emptyList()
    return buildList {
        for (index in 0 until edges.length()) {
            val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
            val content = node.optJSONObject("content") ?: continue
            if (content.optString("__typename") != "SearchSuggestionChannel") continue
            val login = content.optString("login").trim().lowercase()
            if (!isValidChannelLogin(login)) continue
            val displayName = node.optString("text").trim().ifBlank { login }
            val avatarUrl = content.optString("profileImageURL").takeIf { url ->
                url.isNotBlank() && url != "null"
            }
            add(
                ChannelSearchHit(
                    login = login,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    isLive = content.optBoolean("isLive"),
                    userId = content.optString("id").trim().takeIf { it.isNotEmpty() && it != "null" },
                ),
            )
        }
    }
}

object ChannelSearchRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun suggest(query: String): List<ChannelSearchHit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("query", SUGGESTIONS_QUERY)
                .put("variables", JSONObject().put("query", query))
            val request = twitchGqlRequest(payload.toString()).build()
            val body = http.newCall(request).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(TAG, "search", response.code, text)
                when (response.code) {
                    in 200..299 -> text
                    in 400..499 -> {
                        AppLog.w(TAG, "search rejected HTTP ${response.code}")
                        return@withContext emptyList()
                    }
                    else -> {
                        AppLog.w(TAG, "search failed HTTP ${response.code}")
                        return@withContext emptyList()
                    }
                }
            }
            parseChannelSearchSuggestions(body)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppLog.w(TAG, "search failed: ${error.javaClass.simpleName}")
            emptyList()
        }
    }

    private const val TAG = "ChannelSearch"
    // Without withOfflineChannelContent, Twitch leaves the content of an offline channel null, so only live
    // channels would be suggested.
    private const val SUGGESTIONS_QUERY =
        $$"query($query:String!){searchSuggestions(queryFragment:$query,withOfflineChannelContent:true)" +
            "{edges{node{text content{" +
            "__typename ... on SearchSuggestionChannel{id login profileImageURL(width:70) isLive}" +
            "}}}}}"
}
