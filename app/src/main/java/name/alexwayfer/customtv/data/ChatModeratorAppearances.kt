package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.coroutines.executeAsync
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Colors and channel badges for every moderator, including those not seen writing in this chat. No login needed. */
internal suspend fun loadModeratorAppearances(
    userIds: List<String>,
    channelId: String?,
    channelLogin: String,
): Map<String, ChatModeratorAppearance> = withContext(Dispatchers.IO) {
    val appearances = mutableMapOf<String, ChatModeratorAppearance>()
    for (batch in userIds.distinct().chunked(MODERATOR_APPEARANCE_BATCH_SIZE)) {
        val variables = JSONObject().put("ids", JSONArray(batch))
            .put("channelId", channelId ?: JSONObject.NULL)
            .put("channelLogin", if (channelId == null) channelLogin else JSONObject.NULL)
        val payload = JSONObject().put("query", APPEARANCES_QUERY).put("variables", variables)
        try {
            val request = twitchGqlRequest(payload.toString()).build()
            sharedHttpClient.newCall(request).executeAsync().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql(TAG, "moderator appearances", response.code, body)
                val parsed = if (response.isSuccessful) parseModeratorAppearances(body) else null
                if (parsed != null) {
                    appearances.putAll(parsed)
                } else {
                    AppLog.w(TAG, "moderator appearances unavailable HTTP ${response.code}: ${gqlFailureCause(body)}")
                }
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "moderator appearances failed ${error.javaClass.simpleName}")
        }
    }
    appearances
}

private const val TAG = "ChatModerators"
private const val MODERATOR_APPEARANCE_BATCH_SIZE = 100
private const val APPEARANCES_QUERY =
    $$"query($ids:[ID!]!,$channelId:ID,$channelLogin:String){users(ids:$ids){id chatColor " +
        $$"displayBadges(channelID:$channelId,channelLogin:$channelLogin){setID version imageURL(size:NORMAL)}}}"
