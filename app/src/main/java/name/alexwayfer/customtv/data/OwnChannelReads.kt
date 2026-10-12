package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.diagnostics.HELIX_EXPECTED_CODES
import okhttp3.OkHttpClient
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/** Whether the signed-in user follows or subscribes to one channel. */
internal class OwnChannelReads(
    private val clientId: String,
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10.seconds)
        .readTimeout(10.seconds)
        .build(),
) {
    suspend fun ownChatterFollow(accessToken: String, userId: String, broadcasterId: String): ChatterFollow? =
        chatterFollowOf(readOwnFollow(accessToken, userId, broadcasterId))

    suspend fun readOwnSubscription(
        accessToken: String,
        userId: String,
        broadcasterId: String,
    ): UserSubscriptionRead {
        if (userId.isBlank() || broadcasterId.isBlank()) return UserSubscriptionRead.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val response = http.helixGet(userSubscriptionUrl(userId, broadcasterId), accessToken, clientId)
                val read = userSubscriptionRead(response.code, response.body)
                if (read == UserSubscriptionRead.Unavailable) {
                    if (response.code in HTTP_CLIENT_ERROR) {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "subscription rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            FOLLOWED_CHANNELS_TAG,
                            "helix/subscriptions/user",
                            response.code,
                            twitchAuthMessage(response.body),
                            HELIX_EXPECTED_CODES,
                        )
                    } else {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "subscription failed HTTP ${response.code}")
                    }
                }
                read
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(FOLLOWED_CHANNELS_TAG, "subscription failed ${error.javaClass.simpleName}")
                UserSubscriptionRead.Unavailable
            }
        }
    }

    suspend fun readOwnFollow(accessToken: String, userId: String, broadcasterId: String): OwnFollowRead {
        if (userId.isBlank() || broadcasterId.isBlank()) return OwnFollowRead.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val response = http.helixGet(ownFollowedAtUrl(userId, broadcasterId), accessToken, clientId)
                when (response.code) {
                    in HTTP_SUCCESS -> {
                        val atMillis = parseOwnFollowedAt(response.body, broadcasterId)
                        if (atMillis == null) OwnFollowRead.NotFollowing else OwnFollowRead.Following(atMillis)
                    }
                    in HTTP_CLIENT_ERROR -> {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "own follow rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            FOLLOWED_CHANNELS_TAG,
                            "helix/channels/followed",
                            response.code,
                            twitchAuthMessage(response.body),
                            HELIX_EXPECTED_CODES,
                        )
                        OwnFollowRead.Unavailable
                    }
                    else -> {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "own follow failed HTTP ${response.code}")
                        OwnFollowRead.Unavailable
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(FOLLOWED_CHANNELS_TAG, "own follow failed ${error.javaClass.simpleName}")
                OwnFollowRead.Unavailable
            }
        }
    }
}
