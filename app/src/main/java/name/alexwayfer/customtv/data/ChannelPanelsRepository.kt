package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** What loading a channel's About panels gave. */
internal sealed interface ChannelPanelsLoad {
    data class Loaded(val panels: ChannelPanels) : ChannelPanelsLoad

    /** Twitch answered without the panels, as it does on some networks. */
    data object LeftOut : ChannelPanelsLoad

    data object Failed : ChannelPanelsLoad
}

/**
 * Channels' About panels, loaded when the About tab opens and kept in memory by user id for the rest of the run,
 * so the tab opens at once the next time.
 */
internal object ChannelPanelsRepository {
    private const val TAG = "ChannelPanels"
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val kept = ConcurrentHashMap<String, ChannelPanels>()

    fun cached(userId: String): ChannelPanels? = kept[userId]

    suspend fun load(userId: String): ChannelPanelsLoad = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("query", CHANNEL_PANELS_QUERY)
                .put("variables", JSONObject().put("id", userId))
            val body = http.newCall(twitchGqlRequest(payload.toString()).build()).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(TAG, "channel panels", response.code, text)
                if (!response.isSuccessful) {
                    AppLog.w(TAG, "channel panels failed HTTP ${response.code}")
                    return@withContext ChannelPanelsLoad.Failed
                }
                text
            }
            val panels = parseChannelPanels(body)
            if (panels == null) {
                AppLog.w(TAG, "channel panels left out: ${gqlFailureCause(body)}")
                return@withContext ChannelPanelsLoad.LeftOut
            }
            kept[userId] = panels
            ChannelPanelsLoad.Loaded(panels)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(TAG, "channel panels failed: ${error.javaClass.simpleName}")
            ChannelPanelsLoad.Failed
        }
    }
}
