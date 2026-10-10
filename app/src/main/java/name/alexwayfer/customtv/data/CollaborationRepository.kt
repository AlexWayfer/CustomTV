package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class CollaborationRepository(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun load(broadcasterId: String, currentLogin: String? = null): CollaborationResult {
        if (broadcasterId.isBlank()) return CollaborationResult.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("query", COLLABORATION_QUERY)
                    .put("variables", JSONObject().put("channelID", broadcasterId))
                val request = twitchGqlRequest(payload.toString()).build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    Diagnostics.reportGql(TAG, "collaboration", response.code, body)
                    val code = response.code
                    if (code in 400..499) {
                        AppLog.w(TAG, "collaboration rejected HTTP $code")
                        return@withContext CollaborationResult.Unavailable
                    }
                    if (code !in 200..299) {
                        AppLog.w(TAG, "collaboration failed HTTP $code")
                        return@withContext CollaborationResult.Unavailable
                    }
                    val channels = parseCollaborationChannels(body, currentLogin)
                        ?: return@withContext CollaborationResult.Unavailable
                    CollaborationResult.Ready(channels)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "collaboration failed ${error.javaClass.simpleName}")
                CollaborationResult.Unavailable
            }
        }
    }

    private companion object {
        const val TAG = "Collaboration"
        const val COLLABORATION_QUERY = $$"""
            query($channelID:ID!){
              channel(id:$channelID){
                collaboration{
                  id
                  collaborators{
                    role
                    status
                    user{id login displayName profileImageURL(width:70) stream{viewersCount}}
                  }
                }
              }
            }
        """
    }
}

internal sealed interface CollaborationResult {
    data class Ready(val channels: List<CollaborationChannel>) : CollaborationResult
    data object Unavailable : CollaborationResult
}
