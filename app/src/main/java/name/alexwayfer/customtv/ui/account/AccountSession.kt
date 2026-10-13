package name.alexwayfer.customtv.ui.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.auth.TWITCH_AUTH_TAG
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.TwitchSession
import name.alexwayfer.customtv.auth.TwitchSessionHolder
import name.alexwayfer.customtv.auth.TwitchTokens
import name.alexwayfer.customtv.auth.ValidatedScopes
import name.alexwayfer.customtv.auth.followListCanLoad
import name.alexwayfer.customtv.auth.freshSavedFollowAccess
import name.alexwayfer.customtv.auth.profileAfterRefresh
import name.alexwayfer.customtv.auth.rejectedAuthClearsSession
import name.alexwayfer.customtv.auth.savedSessionNeedsNewLogin
import name.alexwayfer.customtv.auth.twitchLoginScopes
import name.alexwayfer.customtv.data.ProfileDetails
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.data.TwitchSessionStore
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException

/**
 * What the session reads and writes: the saved session, Twitch's answers about the token, the account and its
 * permissions, and the profile's details.
 */
internal data class AccountSessionIo(
    val read: () -> TwitchSession?,
    val write: (TwitchSession) -> Unit,
    val erase: () -> Unit,
    val refreshTokens: suspend (refreshToken: String) -> TwitchTokens,
    val account: suspend (accessToken: String) -> TwitchAccount,
    val validatedScopes: suspend (accessToken: String) -> ValidatedScopes,
    val profileDetails: suspend (login: String) -> ProfileDetails?,
)

/** [AccountSessionIo] through Twitch and the session file. */
internal fun accountSessionIo(client: TwitchAuthClient, store: TwitchSessionStore): AccountSessionIo = AccountSessionIo(
    read = store::read,
    write = store::write,
    erase = store::clear,
    refreshTokens = client::refresh,
    account = client::account,
    validatedScopes = client::validatedScopes,
    profileDetails = ProfileDetailsRepository::load,
)

/**
 * The saved Twitch session: restored at start, its token refreshed before use, its permissions checked, and dropped
 * when Twitch refuses it. Its I/O is functions, so unit tests run without Android or Twitch.
 */
internal class AccountSession(
    scope: CoroutineScope,
    private val io: AccountSessionIo,
    val relogin: ReloginPrompts,
) {
    val sessions = TwitchSessionHolder(io.read, io.write, io.erase, io.refreshTokens)
    private val _followsReady = MutableStateFlow(false)
    val followsReady: StateFlow<Boolean> = _followsReady
    private var restoreJob: Job? = null

    init {
        val saved = sessions.session
        _followsReady.value = followListCanLoad(saved?.grantedScopes)
        if (saved != null && savedSessionNeedsNewLogin(saved.grantedScopes, twitchLoginScopes())) {
            relogin.promptForNewScopes()
            AppLog.i(TWITCH_AUTH_TAG, "token scopes differ")
        }
        restoreJob = scope.launch { restore(saved) }
    }

    /** Stops the restore of the saved session and starts a new generation for the login that replaces it. */
    fun beginLogin() {
        restoreJob?.cancel()
        sessions.startGeneration { _followsReady.value = false }
    }

    /** Drops the saved session, and its restore still in flight; [onClear] runs under the holder's lock. */
    fun clear(onClear: () -> Unit = {}) {
        sessions.clear {
            onClear()
            _followsReady.value = false
        }
        restoreJob?.cancel()
    }

    private suspend fun restore(saved: TwitchSession?) {
        if (saved == null) return
        val generation = sessions.generation()
        try {
            val account = loadAccount(saved, generation) ?: return
            sessions.showAccount(account, generation)
        } catch (error: TwitchAuthException) {
            AppLog.w(TWITCH_AUTH_TAG, "restore failed HTTP ${error.httpCode}")
            if (!rejectedAuthClearsSession(error.httpCode)) return
            dropRejectedSession(generation)
        } catch (error: IOException) {
            AppLog.w(TWITCH_AUTH_TAG, "restore failed", error)
        }
    }

    fun freshSavedAccess(): TwitchUserAccess? {
        val session = sessions.session ?: return null
        val (token, userId) = freshSavedFollowAccess(
            session.accessToken,
            session.account.userId,
            session.expiresAtMillis,
            System.currentTimeMillis(),
        ) ?: return null
        return TwitchUserAccess(accessToken = token, userId = userId)
    }

    /**
     * The access for a request. It does not wait for the restore at start to read the account and check the token's
     * permissions: a refresh the restore runs is shared through the refresh lock.
     */
    suspend fun userAccess(): TwitchUserAccess? {
        val generation = sessions.generation()
        val current = sessions.session?.let { refreshedSession(it, generation) }
        val userId = current?.let { it.account.userId ?: loadUserId(it, generation) }
        return if (current != null && userId != null) {
            TwitchUserAccess(accessToken = current.accessToken, userId = userId)
        } else {
            null
        }
    }

    /** [session] with a token that is not about to expire; null when the refresh failed. */
    private suspend fun refreshedSession(session: TwitchSession, generation: Int): TwitchSession? = try {
        sessions.refreshIfDue(session, generation)
    } catch (error: TwitchAuthException) {
        AppLog.w(TWITCH_AUTH_TAG, "access refresh failed HTTP ${error.httpCode}")
        if (rejectedAuthClearsSession(error.httpCode)) dropRejectedSession(generation)
        null
    } catch (error: IOException) {
        AppLog.w(TWITCH_AUTH_TAG, "access refresh failed ${error.javaClass.simpleName}")
        null
    }

    private suspend fun loadUserId(session: TwitchSession, generation: Int): String? {
        return try {
            val fresh = io.account(session.accessToken)
            val merged = profileAfterRefresh(session.account, fresh, null)
            sessions.publish(session, merged, generation)?.userId
        } catch (error: TwitchAuthException) {
            AppLog.w(TWITCH_AUTH_TAG, "user id failed HTTP ${error.httpCode}")
            if (rejectedAuthClearsSession(error.httpCode)) dropRejectedSession(generation)
            null
        } catch (error: IOException) {
            AppLog.w(TWITCH_AUTH_TAG, "user id failed ${error.javaClass.simpleName}")
            null
        }
    }

    suspend fun loadAccount(
        session: TwitchSession,
        generation: Int,
        deviceGrant: Boolean = false,
    ): TwitchAccount? {
        val current = sessions.refreshIfDue(session, generation, deviceGrant) ?: return null
        val account = try {
            val fresh = io.account(current.accessToken)
            sessions.publish(
                current,
                profileAfterRefresh(current.account, fresh, io.profileDetails(fresh.login)),
                generation,
            )
        } catch (error: TwitchAuthException) {
            if (error.httpCode != 401) throw error
            AppLog.i(TWITCH_AUTH_TAG, "access rejected, refreshing")
            val refreshed = sessions.refreshRejected(current, generation) ?: return null
            val fresh = io.account(refreshed.accessToken)
            sessions.publish(
                refreshed,
                profileAfterRefresh(refreshed.account, fresh, io.profileDetails(fresh.login)),
                generation,
            )
        }
        if (account == null) return null
        return if (keepSessionScopes(generation)) account else null
    }

    private suspend fun keepSessionScopes(generation: Int): Boolean {
        val session = sessions.session ?: return false
        return when (val validated = io.validatedScopes(session.accessToken)) {
            is ValidatedScopes.Known -> {
                if (savedSessionNeedsNewLogin(validated.scopes, twitchLoginScopes())) {
                    AppLog.i(TWITCH_AUTH_TAG, "token scopes differ")
                    if (session.grantedScopes != validated.scopes) {
                        sessions.publish(session.copy(grantedScopes = validated.scopes), session.account, generation)
                    }
                    keepSessionDespiteScopes(session.copy(grantedScopes = validated.scopes), generation)
                    true
                } else {
                    relogin.clearPermissions()
                    if (session.grantedScopes != validated.scopes) {
                        sessions.publish(session.copy(grantedScopes = validated.scopes), session.account, generation)
                    }
                    sessions.ifCurrent(generation) { _followsReady.value = true }
                    true
                }
            }
            ValidatedScopes.Unavailable -> true
            ValidatedScopes.Rejected -> {
                dropRejectedSession(generation)
                false
            }
        }
    }

    private fun keepSessionDespiteScopes(session: TwitchSession, generation: Int) {
        relogin.promptForNewScopes()
        if (followListCanLoad(session.grantedScopes)) {
            sessions.ifCurrent(generation) { _followsReady.value = true }
        }
    }

    private suspend fun dropRejectedSession(generation: Int) {
        sessions.clearIfCurrent(generation) { hadSession ->
            _followsReady.value = false
            relogin.clearPermissions()
            relogin.sessionRejected(hadSession)
        }
    }
}
