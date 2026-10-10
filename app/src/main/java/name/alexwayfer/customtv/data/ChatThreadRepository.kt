package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.ChatThreadEntry
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class ChatThreadRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun load(messageId: String): List<ChatThreadEntry>? {
        if (messageId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("query", THREAD_QUERY)
                    .put("variables", JSONObject().put("id", messageId))
                val request = twitchGqlRequest(payload.toString()).build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    Diagnostics.reportGql(TAG, "thread", response.code, body)
                    val code = response.code
                    if (code !in 200..299) {
                        if (code in 400..499) {
                            AppLog.w(TAG, "thread rejected HTTP $code")
                        } else {
                            AppLog.w(TAG, "thread failed HTTP $code")
                        }
                        return@withContext null
                    }
                    parseChatThread(body)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "thread failed ${error.javaClass.simpleName}")
                null
            }
        }
    }

    private companion object {
        const val TAG = "ChatThread"
        const val THREAD_QUERY = $$"""
            query($id:ID!){
              message(id:$id){
                id
                sentAt
                deletedAt
                content{text}
                sender{login displayName}
                replies{nodes{
                  id
                  sentAt
                  deletedAt
                  content{text}
                  sender{login displayName}
                  parentMessage{id}
                  replies{nodes{
                    id
                    sentAt
                    deletedAt
                    content{text}
                    sender{login displayName}
                    parentMessage{id}
                  }}
                }}
              }
            }
        """
    }
}
