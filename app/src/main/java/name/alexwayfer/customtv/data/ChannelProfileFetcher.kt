package name.alexwayfer.customtv.data

import okhttp3.CacheControl
import org.json.JSONObject
import name.alexwayfer.customtv.diagnostics.Diagnostics
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Loads channel profiles from Twitch GQL through the public client, past any cache. */
internal object ChannelProfileFetcher {
    private val http = sharedHttpClient.newBuilder()
        .cache(null)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** The profile of [login], with its custom rewards; null when Twitch has no such channel. */
    fun one(login: String): ChannelProfile? {
        val payload = JSONObject()
            .put("query", PROFILE_QUERY)
            .put("variables", JSONObject().put("login", login))
        val body = executeGql(payload)
        val parsed = ChannelProfileParser.parse(body, login) ?: return null
        ChannelCustomRewards.remember(parsed.profile.login, parsed.customRewards)
        return parsed.profile
    }

    /** The profiles of [ids] in one query, without the details only the full profile query loads. */
    fun many(ids: List<String>): List<ChannelProfile> {
        val variables = JSONObject()
        val declarations = ids.indices.joinToString(",") { index -> $$"$id$$index:ID!" }
        val users = ids.mapIndexed { index, id ->
            variables.put("id$index", id)
            $$"user$$index:user(id:$id$$index){$$HOME_PROFILE_FIELDS}"
        }
        val payload = JSONObject()
            .put("query", "query($declarations){${users.joinToString("")}}")
            .put("variables", variables)
        val data = executeGql(payload).toJsonObjectOrNull()?.optJSONObject("data") ?: return emptyList()
        return ids.indices.mapNotNull { index ->
            val user = data.optJSONObject("user$index") ?: return@mapNotNull null
            val login = user.text("login") ?: return@mapNotNull null
            ChannelProfileParser.parseUser(user, login).profile
        }
    }

    private fun executeGql(payload: JSONObject): String {
        val request = twitchGqlRequest(payload.toString())
            .header("Client-Request-Id", UUID.randomUUID().toString())
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            Diagnostics.reportGql(CHANNEL_AVATAR_TAG, "channel profile", response.code, body)
            if (!response.isSuccessful) {
                throw IOException("Twitch GQL ${response.code}")
            }
            return body
        }
    }

    /** The category's name, and its ID and box art, with which the panel under the stream opens it in Explore. */
    private const val CATEGORY_FIELDS = "id name boxArtURL(width:144,height:192)"
    private const val PROFILE_QUERY =$$"query($login:String!){user(login:$login){" +
        "id login displayName profileImageURL(width:150) primaryColorHex " +
        "stream{title createdAt archiveVideo{id} viewersCount collaborationViewersCount " +
        "game{$CATEGORY_FIELDS} freeformTags{name}} " +
        "broadcastSettings{title game{$CATEGORY_FIELDS}} " +
        "channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}} " +
        "communityPointsSettings{image{url} defaultImage{url} " +
        "automaticRewards{type cost defaultCost backgroundColor} " +
        "customRewards{id title prompt cost backgroundColor image{url} defaultImage{url}}}} " +
        "chatSettings{rules}}}"
    private const val HOME_PROFILE_FIELDS =
        "id login displayName profileImageURL(width:150) primaryColorHex " +
            "stream{title createdAt archiveVideo{id} viewersCount collaborationViewersCount game{$CATEGORY_FIELDS}} " +
            "broadcastSettings{title game{$CATEGORY_FIELDS}} " +
            "channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}}}"
}

/**
 * A profile from the short query keeps the details only the full one loads, such as the channel's colors and chat
 * rules, from the [previous] profile of the channel.
 */
internal fun profileWithKeptDetails(fetched: ChannelProfile, previous: ChannelProfile?): ChannelProfile {
    if (previous == null) return fetched
    return fetched.copy(
        highlightColorHex = previous.highlightColorHex ?: fetched.highlightColorHex,
        primaryColorHex = fetched.primaryColorHex ?: previous.primaryColorHex,
        highlightRewardCost = previous.highlightRewardCost ?: fetched.highlightRewardCost,
        channelPointsIconUrl = previous.channelPointsIconUrl,
        tags = fetched.tags.ifEmpty { previous.tags },
        chatRules = fetched.chatRules ?: previous.chatRules,
    )
}
