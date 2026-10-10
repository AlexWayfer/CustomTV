package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.accessExpiryMillis
import name.alexwayfer.customtv.auth.freshSavedFollowAccess
import name.alexwayfer.customtv.auth.rejectedAuthClearsSession
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException

internal data class StreamStartSession(
    val accessToken: String,
    val userId: String,
)

/** [onRejected] runs when Twitch refuses the saved refresh token, which only a new login fixes. */
internal suspend fun streamStartAccess(
    context: Context,
    clientId: String,
    onRejected: () -> Unit = {},
): StreamStartSession? {
    return withTwitchSessionRefreshLock {
        val store = TwitchSessionStore(context)
        val session = store.read() ?: return@withTwitchSessionRefreshLock null
        val fresh = freshSavedFollowAccess(
            session.accessToken,
            session.account.userId,
            session.expiresAtMillis,
            System.currentTimeMillis(),
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
            val userId = updated.account.userId ?: return@withTwitchSessionRefreshLock null
            StreamStartSession(updated.accessToken, userId)
        } catch (error: TwitchAuthException) {
            if (rejectedAuthClearsSession(error.httpCode)) {
                AppLog.w(TAG, "token rejected HTTP ${error.httpCode}")
                onRejected()
            } else {
                AppLog.w(TAG, "token failed HTTP ${error.httpCode}")
            }
            null
        } catch (error: IOException) {
            AppLog.w(TAG, "token failed ${error.javaClass.simpleName}")
            null
        }
    }
}

private const val TAG = "StreamStart"
