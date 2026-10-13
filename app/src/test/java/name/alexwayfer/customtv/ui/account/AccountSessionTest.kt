package name.alexwayfer.customtv.ui.account

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.TwitchSession
import name.alexwayfer.customtv.auth.TwitchTokens
import name.alexwayfer.customtv.auth.ValidatedScopes
import name.alexwayfer.customtv.auth.twitchLoginScopes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class AccountSessionTest {
    private val scopes = twitchLoginScopes().split(' ').filter { it.isNotBlank() }.toSet()
    private val account = TwitchAccount(login = "viewer", displayName = "Viewer", avatarUrl = null, userId = "1")

    /** The saved file and Twitch, answering as each test sets them. */
    private inner class FakeTwitch(var stored: TwitchSession?) {
        var refreshes = 0
        var refresh: () -> TwitchTokens = { TwitchTokens("new-access", "new-refresh", expiresInSeconds = 14_400) }
        var profile: (accessToken: String) -> TwitchAccount = { account }
        var validated: ValidatedScopes = ValidatedScopes.Known(scopes)
        /** Holds the account's answer until completed, as a slow network would. */
        var accountAnswer: CompletableDeferred<Unit>? = null

        fun io() = AccountSessionIo(
            read = { stored },
            write = { stored = it },
            erase = { stored = null },
            refreshTokens = {
                refreshes++
                refresh()
            },
            account = { token ->
                accountAnswer?.await()
                profile(token)
            },
            validatedScopes = { validated },
            profileDetails = { null },
        )
    }

    private val relogin = ReloginPrompts()

    private fun saved(expiresAtMillis: Long = System.currentTimeMillis() + HOUR_MILLIS) =
        TwitchSession("old-access", "old-refresh", expiresAtMillis, account, scopes)

    /** The session after its restore at start has finished. */
    private fun restored(twitch: FakeTwitch): AccountSession = runBlocking { restore(twitch) }

    private suspend fun restore(twitch: FakeTwitch): AccountSession =
        coroutineScope { AccountSession(this, twitch.io(), relogin) }

    @Test
    fun aSavedSessionComesBackWithItsAccountAndFollows() {
        val twitch = FakeTwitch(saved())
        val session = restored(twitch)

        assertEquals(account, session.sessions.account.value)
        assertTrue(session.followsReady.value)
        assertEquals(0, twitch.refreshes)
    }

    @Test
    fun aRefusedAccessTokenIsRefreshedAndTheAccountReadAgain() {
        val twitch = FakeTwitch(saved())
        twitch.profile = { token -> if (token == "old-access") throw TwitchAuthException(401) else account }
        val session = restored(twitch)

        assertEquals(1, twitch.refreshes)
        assertEquals("new-access", twitch.stored?.accessToken)
        assertEquals(account, session.sessions.account.value)
    }

    @Test
    fun aServerFailureOnTheRefreshKeepsTheSession() {
        val twitch = FakeTwitch(saved(expiresAtMillis = 0L))
        twitch.refresh = { throw TwitchAuthException(503) }
        val session = restored(twitch)

        assertEquals("old-access", twitch.stored?.accessToken)
        assertEquals(account, session.sessions.account.value)
        assertFalse(relogin.forSession.value)
    }

    @Test
    fun aRefusedRefreshDropsTheSessionAndAsksToLogInAgain() {
        val twitch = FakeTwitch(saved(expiresAtMillis = 0L))
        twitch.refresh = { throw TwitchAuthException(400) }
        val session = restored(twitch)

        assertNull(twitch.stored)
        assertNull(session.sessions.account.value)
        assertFalse(session.followsReady.value)
        assertTrue(relogin.forSession.value)
    }

    @Test
    fun aNetworkFailureKeepsTheSession() {
        val twitch = FakeTwitch(saved())
        twitch.profile = { throw IOException() }
        val session = restored(twitch)

        assertNotNull(twitch.stored)
        assertEquals(account, session.sessions.account.value)
    }

    @Test
    fun aTokenTwitchRefusesOnValidationDropsTheSession() {
        val twitch = FakeTwitch(saved())
        twitch.validated = ValidatedScopes.Rejected
        val session = restored(twitch)

        assertNull(twitch.stored)
        assertNull(session.sessions.account.value)
    }

    @Test
    fun anUnansweredValidationKeepsTheSession() {
        val twitch = FakeTwitch(saved())
        twitch.validated = ValidatedScopes.Unavailable
        val session = restored(twitch)

        assertEquals(account, session.sessions.account.value)
        assertNotNull(twitch.stored)
    }

    @Test
    fun aLoginLeftWithoutAPermissionAsksForItAndKeepsTheSession() {
        val twitch = FakeTwitch(saved())
        twitch.validated = ValidatedScopes.Known(scopes - "user:write:chat")
        val session = restored(twitch)

        assertTrue(relogin.forPermissions.value)
        assertEquals(account, session.sessions.account.value)
        assertEquals(scopes - "user:write:chat", twitch.stored?.grantedScopes)
    }

    @Test
    fun accessForARequestKeepsTheSessionOnAServerFailureAndDropsItOnARefusal() = runBlocking {
        val twitch = FakeTwitch(saved(expiresAtMillis = 0L))
        // The restore at start meets no network and leaves the session as it was.
        twitch.refresh = { throw IOException() }
        val session = restore(twitch)

        twitch.refresh = { throw TwitchAuthException(500) }
        assertNull(session.userAccess())
        assertNotNull(twitch.stored)
        assertEquals(account, session.sessions.account.value)

        twitch.refresh = { throw TwitchAuthException(401) }
        assertNull(session.userAccess())
        assertNull(twitch.stored)
        assertNull(session.sessions.account.value)
    }

    @Test
    fun accessForARequestRefreshesATokenAboutToExpire() = runBlocking {
        val twitch = FakeTwitch(saved(expiresAtMillis = 0L))
        twitch.refresh = { throw IOException() }
        val session = restore(twitch)

        twitch.refresh = { TwitchTokens("new-access", "new-refresh", expiresInSeconds = 14_400) }
        val access = session.userAccess()

        assertEquals("new-access", access?.accessToken)
        assertEquals("1", access?.userId)
        assertEquals("new-access", twitch.stored?.accessToken)
    }

    @Test
    fun accessForARequestDoesNotWaitForTheRestoreToReadTheAccount() = runBlocking {
        val twitch = FakeTwitch(saved())
        val answer = CompletableDeferred<Unit>()
        twitch.accountAnswer = answer
        coroutineScope {
            val session = AccountSession(this, twitch.io(), relogin)
            try {
                val access = withTimeout(1.seconds) { session.userAccess() }
                assertEquals("old-access", access?.accessToken)
                assertEquals("1", access?.userId)
            } finally {
                answer.complete(Unit)
            }
        }
    }

    private companion object {
        const val HOUR_MILLIS = 3_600_000L
    }
}
