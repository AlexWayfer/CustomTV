package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** The badges Twitch shows beside a user's name in one channel, with the image for each `set/version`. */
internal class DisplayedChatBadges(val badges: List<ChatBadge>, val imageUrls: Map<String, String>)

internal object OwnChatBadgesRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Public data, so it loads through the public client; null when the request failed. */
    suspend fun load(userId: String, channelId: String): DisplayedChatBadges? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put(
                    "query",
                    $$"query($id:ID!,$channel:ID!){user(id:$id){displayBadges(channelID:$channel){setID version imageURL(size:NORMAL)}}}",
                )
                .put("variables", JSONObject().put("id", userId).put("channel", channelId))
            val body = http.newCall(twitchGqlRequest(payload.toString()).build()).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(TAG, "display badges", response.code, text)
                if (!response.isSuccessful) {
                    AppLog.w(TAG, "load failed HTTP ${response.code}")
                    return@withContext null
                }
                text
            }
            parseDisplayedChatBadges(body)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(TAG, "load failed ${error.javaClass.simpleName}")
            null
        }
    }

    private const val TAG = "OwnChatBadges"
}

internal fun parseDisplayedChatBadges(body: String): DisplayedChatBadges? {
    val array = JSONObject(body).optJSONObject("data")?.optJSONObject("user")?.optJSONArray("displayBadges")
        ?: return null
    val badges = ArrayList<ChatBadge>(array.length())
    val urls = LinkedHashMap<String, String>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val setId = item.optString("setID").takeIf { it.isNotBlank() && it != "null" } ?: continue
        val badge = ChatBadge(setId, item.optString("version"))
        badges += badge
        item.optString("imageURL").takeIf { it.isNotBlank() && it != "null" }?.let { urls[badge.key] = it }
    }
    return DisplayedChatBadges(badges, urls)
}
