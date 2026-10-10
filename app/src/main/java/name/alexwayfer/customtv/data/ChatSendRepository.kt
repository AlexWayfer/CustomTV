package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class ChatSendRepository(
    private val clientId: String,
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun send(
        accessToken: String,
        broadcasterId: String,
        senderId: String,
        message: String,
        replyParentMessageId: String? = null,
    ): ChatSendResult {
        val text = chatMessageToSend(message) ?: return ChatSendResult.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("broadcaster_id", broadcasterId)
                    .put("sender_id", senderId)
                    .put("message", text)
                if (!replyParentMessageId.isNullOrBlank()) {
                    payload.put("reply_parent_message_id", replyParentMessageId)
                }
                val request = helixRequest(SEND_URL, accessToken, clientId)
                    .post(payload.toString().toRequestBody(JSON))
                    .build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    val result = parseChatSendResponse(response.code, body)
                    when (result) {
                        ChatSendResult.Sent -> Unit
                        is ChatSendResult.Dropped -> AppLog.w(TAG, "chat send dropped ${result.code}")
                        is ChatSendResult.Rejected -> {
                            AppLog.w(TAG, "chat send rejected HTTP ${response.code}")
                            // The user reads 403 (not allowed to chat there) and 429 (their own pace); neither needs a fix.
                            Diagnostics.reportHttp(
                                TAG,
                                "helix/chat/messages",
                                response.code,
                                twitchAuthMessage(body),
                                setOf(401, 403, 429),
                            )
                        }
                        ChatSendResult.Unavailable -> AppLog.w(TAG, "chat send failed HTTP ${response.code}")
                    }
                    result
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "chat send failed ${error.javaClass.simpleName}")
                ChatSendResult.Unavailable
            }
        }
    }

    private companion object {
        const val TAG = "ChatSend"
        const val SEND_URL = "https://api.twitch.tv/helix/chat/messages"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
