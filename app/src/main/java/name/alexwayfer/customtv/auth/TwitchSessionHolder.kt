package name.alexwayfer.customtv.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.withTwitchSessionRefreshLock
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * The saved Twitch session and the account on screen.
 *
 * Each login, logout, or dropped session starts a new generation. Work that began in an older
 * generation cannot write: the generation check and every write happen under one lock, so a
 * late restore cannot clear or overwrite a login that finished in between. Callers pass their
 * own state changes as `inLock` to make them part of the same step.
 *
 * Storage and the token request are functions, so unit tests can run without Android or Twitch.
 */
internal class TwitchSessionHolder(
    private val read: () -> TwitchSession?,
    private val write: (TwitchSession) -> Unit,
    private val erase: () -> Unit,
    private val refreshTokens: suspend (refreshToken: String) -> TwitchTokens,
) {
    private val lock = Any()
    private var generation = 0
    private var current: TwitchSession? = read()
    private val _account = MutableStateFlow(current?.account)
    val account: StateFlow<TwitchAccount?> = _account

    val session: TwitchSession?
        get() = synchronized(lock) { current }

    fun generation(): Int = synchronized(lock) { generation }

    /** Drops the results of work that has already started, such as a restore before a new login. */
    fun startGeneration(inLock: () -> Unit) {
        synchronized(lock) {
            generation += 1
            inLock()
        }
    }

    /** Logs out: forgets the saved session and the account. */
    fun clear(inLock: () -> Unit) {
        synchronized(lock) {
            generation += 1
            erase()
            current = null
            _account.value = null
            inLock()
        }
    }

    /** Runs [block] only while [generation] is still current. */
    fun ifCurrent(generation: Int, block: () -> Unit) {
        synchronized(lock) {
            if (sessionWriteAllowed(generation, this.generation)) block()
        }
    }

    /** Shows [account] once its login or restore is still current. */
    fun showAccount(account: TwitchAccount, generation: Int, inLock: () -> Unit = {}) {
        ifCurrent(generation) {
            _account.value = account
            inLock()
        }
    }

    /** Forgets a session Twitch refused. [inLock] learns whether a session was saved. */
    suspend fun clearIfCurrent(generation: Int, inLock: (hadSession: Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                if (!sessionWriteAllowed(generation, this@TwitchSessionHolder.generation)) return@withContext
                val hadSession = current != null
                erase()
                current = null
                _account.value = null
                inLock(hadSession)
            }
        }
    }

    /** Saves [session] with [account]. Null when a newer generation has started. */
    suspend fun publish(session: TwitchSession, account: TwitchAccount, generation: Int): TwitchAccount? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                if (!sessionWriteAllowed(generation, this@TwitchSessionHolder.generation)) return@withContext null
                val stored = session.copy(account = account)
                write(stored)
                current = stored
                account
            }
        }
    }

    /** The session with a fresh access token, refreshed first when it is about to expire. */
    suspend fun refreshIfDue(
        session: TwitchSession,
        generation: Int,
        deviceGrant: Boolean = false,
    ): TwitchSession? {
        return withTwitchSessionRefreshLock {
            val latest = latestStored(session, deviceGrant)
            if (!shouldRefreshAccessToken(latest.expiresAtMillis, System.currentTimeMillis())) {
                latest
            } else {
                AppLog.i(TAG, "refreshing access token")
                refreshAndPublish(latest, generation)
            }
        }
    }

    /**
     * Twitch refused [rejected]'s access token. Another caller may have refreshed it already;
     * otherwise this refreshes it now.
     */
    suspend fun refreshRejected(rejected: TwitchSession, generation: Int): TwitchSession? {
        return withTwitchSessionRefreshLock {
            val latest = latestStored(rejected)
            if (latest.accessToken != rejected.accessToken) latest else refreshAndPublish(latest, generation)
        }
    }

    private suspend fun refreshAndPublish(session: TwitchSession, generation: Int): TwitchSession? {
        val tokens = refreshTokens(session.refreshToken)
        val refreshed = session.copy(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresAtMillis = accessExpiryMillis(System.currentTimeMillis(), tokens.expiresInSeconds),
            grantedScopes = tokens.grantedScopes ?: session.grantedScopes,
        )
        val account = publish(refreshed, refreshed.account, generation) ?: return null
        return refreshed.copy(account = account)
    }

    private fun latestStored(requested: TwitchSession, deviceGrant: Boolean = false): TwitchSession {
        val memory = session
        val persisted = read()
        return latestSessionForRefresh(requested, memory, persisted, deviceGrant)
    }

    private companion object {
        const val TAG = "TwitchAuth"
    }
}
