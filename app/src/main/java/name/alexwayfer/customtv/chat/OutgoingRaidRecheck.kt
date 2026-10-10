package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.data.twitchGqlRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** What Twitch GQL says about the channel's outgoing raid. */
internal sealed interface RaidLookup {
    data class Active(
        val id: String,
        val targetLogin: String,
        val targetDisplayName: String,
        val targetAvatarUrl: String?,
        val viewerCount: Int,
    ) : RaidLookup
    data object None : RaidLookup
    data object Failed : RaidLookup
}

/**
 * PubSub does not replay frames sent while its socket was down, so a raid that went or was canceled
 * then is only visible through GQL. Returns the raid to show after the lookup.
 */
internal fun recheckOutgoingRaid(current: OutgoingRaid?, lookup: RaidLookup, nowMillis: Long): OutgoingRaid? {
    if (current == null || current.leaving) return current
    return when (lookup) {
        RaidLookup.Failed -> current
        // Gone after the countdown ended: it went while the socket was down. Gone before: canceled.
        RaidLookup.None -> if (nowMillis >= current.goAtMillis) current.copy(leaving = true, goAtMillis = nowMillis) else null
        is RaidLookup.Active -> current.copy(
            id = lookup.id,
            targetLogin = lookup.targetLogin,
            targetDisplayName = lookup.targetDisplayName,
            targetAvatarUrl = lookup.targetAvatarUrl ?: current.targetAvatarUrl.takeIf { lookup.id == current.id },
            viewerCount = lookup.viewerCount,
        )
    }
}

internal fun parseRaidLookup(body: String): RaidLookup {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return RaidLookup.Failed
    val user = root.optJSONObject("data")?.optJSONObject("user") ?: return RaidLookup.Failed
    val raid = user.optJSONObject("raid") ?: return RaidLookup.None
    val target = raid.optJSONObject("targetChannel") ?: return RaidLookup.Failed
    val id = text(raid, "id") ?: return RaidLookup.Failed
    val login = text(target, "login")?.lowercase() ?: return RaidLookup.Failed
    return RaidLookup.Active(
        id = id,
        targetLogin = login,
        targetDisplayName = text(target, "displayName") ?: login,
        targetAvatarUrl = text(target, "profileImageURL"),
        viewerCount = raid.optInt("viewerCount", 0).coerceAtLeast(0),
    )
}

internal object OutgoingRaidLookup {
    private const val TAG = "Raid"
    private const val QUERY = $$"""
        query($id:ID!){
          user(id:$id){
            raid{id viewerCount targetChannel{login displayName profileImageURL(width:70)}}
          }
        }
    """
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun load(channelId: String): RaidLookup = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("query", QUERY)
                .put("variables", JSONObject().put("id", channelId))
            http.newCall(twitchGqlRequest(payload.toString()).build()).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql(TAG, "raid", response.code, body)
                if (response.code !in 200..299) {
                    AppLog.w(TAG, "raid lookup failed HTTP ${response.code}")
                    return@withContext RaidLookup.Failed
                }
                parseRaidLookup(body)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppLog.w(TAG, "raid lookup failed ${error.javaClass.simpleName}")
            RaidLookup.Failed
        }
    }
}

private fun text(obj: JSONObject, key: String): String? =
    obj.optString(key).takeIf { it.isNotBlank() && it != "null" }
