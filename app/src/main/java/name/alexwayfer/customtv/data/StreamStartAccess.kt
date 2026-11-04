package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.accessExpiryMillis
import name.alexwayfer.customtv.auth.freshSavedFollowAccess
import name.alexwayfer.customtv.auth.rejectedAuthClearsSession
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException

/**
 * [onRejected] runs when Twitch refuses the saved refresh token, which only a new login fixes. [rejectedAccessToken]
 * is an access token Twitch has just refused, which is refreshed even before it expires.
 */
internal suspend fun streamStartAccess(
    context: Context,
    clientId: String,
    onRejected: () -> Unit = {},
    rejectedAccessToken: String? = null,
): StreamStartSession? {
    return withTwitchSessionRefreshLock {
        val store = TwitchSessionStore(context)
        val session = store.read() ?: return@withTwitchSessionRefreshLock null
        val fresh = freshSavedFollowAccess(
            session.accessToken,
            session.account.userId,
            session.expiresAtMillis,
            System.currentTimeMillis(),
            rejectedAccessToken,
        )
        if (fresh != null) return@withTwitchSessionRefreshLock StreamStartSession(fresh.first, fresh.second)
        try {
            val tokens = TwitchAuthClient(clientId).refresh(session.refreshToken)
            val updated = session.copy(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresAtMillis = accessExpiryMillis(System.currentTimeMillis(), tokens.expiresInSeconds),
                grantedScopes = tokens.grantedScopes ?: session.grantedScopes,
            )
            store.write(updated)
            AppLog.i(STREAM_START_TAG, "token refreshed")
            val userId = updated.account.userId ?: return@withTwitchSessionRefreshLock null
            StreamStartSession(updated.accessToken, userId)
        } catch (error: TwitchAuthException) {
            if (rejectedAuthClearsSession(error.httpCode)) {
                AppLog.w(STREAM_START_TAG, "token rejected HTTP ${error.httpCode}")
                onRejected()
            } else {
                AppLog.w(STREAM_START_TAG, "token failed HTTP ${error.httpCode}")
            }
            null
        } catch (error: IOException) {
            AppLog.w(STREAM_START_TAG, "token failed ${error.javaClass.simpleName}")
            null
        }
    }
}

/** The log tag of the stream start and change alerts. */
internal const val STREAM_START_TAG = "StreamStart"
