package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.PinnedChat
import name.alexwayfer.customtv.chat.PinnedChatParser
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

object PinnedChatRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(channelId: String): PinnedChat? = withContext(Dispatchers.IO) {
        val id = channelId.trim()
        if (id.isEmpty()) return@withContext null
        val payload = JSONObject()
            .put("operationName", "GetPinnedChat")
            .put(
                "variables",
                JSONObject()
                    .put("channelID", id)
                    .put("count", 1),
            )
            .put(
                "extensions",
                JSONObject().put(
                    "persistedQuery",
                    JSONObject()
                        .put("version", 1)
                        .put("sha256Hash", PINNED_CHAT_HASH),
                ),
            )
        val request = twitchGqlRequest(payload.toString())
            .header("Client-Request-Id", UUID.randomUUID().toString())
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            Diagnostics.reportGql("PinnedChat", "pinned chat", response.code, body)
            if (!response.isSuccessful) {
                throw IOException("Twitch GQL ${response.code}")
            }
            PinnedChatParser.parseGqlBody(body)
        }
    }

    private const val PINNED_CHAT_HASH =
        "2d099d4c9b6af80a07d8440140c4f3dbb04d516b35c401aab7ce8f60765308d5"
}
