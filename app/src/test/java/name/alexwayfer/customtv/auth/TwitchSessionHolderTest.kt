package name.alexwayfer.customtv.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchSessionHolderTest {
    private val oldAccount = TwitchAccount(login = "old", displayName = "Old", avatarUrl = null)
    private val newAccount = TwitchAccount(login = "new", displayName = "New", avatarUrl = null)
    private val oldSession = TwitchSession("old-access", "old-refresh", expiresAtMillis = 0L, account = oldAccount)
    private val newSession = TwitchSession("new-access", "new-refresh", expiresAtMillis = 0L, account = newAccount)

    private class Storage(var saved: TwitchSession?) {
        var refreshes = 0
    }

    private fun holder(storage: Storage, refreshed: TwitchTokens? = null) = TwitchSessionHolder(
        read = { storage.saved },
        write = { storage.saved = it },
        erase = { storage.saved = null },
        refreshTokens = {
            storage.refreshes += 1
            refreshed ?: error("no refresh expected")
        },
    )

    @Test
    fun savedSessionShowsItsAccountOnStart() {
        val sessions = holder(Storage(oldSession))
        assertEquals(oldAccount, sessions.account.value)
        assertEquals(oldSession, sessions.session)
    }

    @Test
    fun restoreStartedBeforeNewLoginCannotClearIt() = runBlocking {
        val storage = Storage(oldSession)
        val sessions = holder(storage)
        val restore = sessions.generation()
        sessions.startGeneration {}
        val login = sessions.generation()
        sessions.publish(newSession, newAccount, login)
        sessions.showAccount(newAccount, login)
        var cleared = false
        sessions.clearIfCurrent(restore) { cleared = true }
        assertFalse(cleared)
        assertEquals(newSession, storage.saved)
        assertEquals(newAccount, sessions.account.value)
    }

    @Test
    fun restoreStartedBeforeLogoutCannotSaveOrShowItsAccount() = runBlocking {
        val storage = Storage(oldSession)
        val sessions = holder(storage)
        val restore = sessions.generation()
        sessions.clear {}
        assertNull(sessions.publish(oldSession, oldAccount, restore))
        sessions.showAccount(oldAccount, restore)
        assertNull(storage.saved)
        assertNull(sessions.account.value)
    }

    @Test
    fun currentRestoreClearsRefusedSessionAndReportsItHadOne() = runBlocking {
        val storage = Storage(oldSession)
        val sessions = holder(storage)
        var hadSession: Boolean? = null
        sessions.clearIfCurrent(sessions.generation()) { hadSession = it }
        assertEquals(true, hadSession)
        assertNull(storage.saved)
        assertNull(sessions.account.value)
    }

    @Test
    fun stateChangeRunsOnlyWhileGenerationIsCurrent() {
        val sessions = holder(Storage(oldSession))
        val started = sessions.generation()
        var ran = 0
        sessions.ifCurrent(started) { ran += 1 }
        sessions.startGeneration {}
        sessions.ifCurrent(started) { ran += 1 }
        assertEquals(1, ran)
    }

    @Test
    fun refusedTokenAlreadyReplacedElsewhereIsNotRefreshedAgain() = runBlocking {
        val storage = Storage(oldSession)
        val sessions = holder(storage)
        // Another caller, such as the stream start check, saved a newer token.
        storage.saved = oldSession.copy(accessToken = "other-access", refreshToken = "other-refresh")
        val result = sessions.refreshRejected(oldSession, sessions.generation())
        assertEquals("other-access", result?.accessToken)
        assertEquals(0, storage.refreshes)
    }

    @Test
    fun refusedTokenIsRefreshedAndSaved() = runBlocking {
        val storage = Storage(oldSession)
        val sessions = holder(storage, TwitchTokens("fresh-access", "fresh-refresh", expiresInSeconds = 3600))
        val result = sessions.refreshRejected(oldSession, sessions.generation())
        assertEquals("fresh-access", result?.accessToken)
        assertEquals("fresh-refresh", storage.saved?.refreshToken)
        assertEquals(1, storage.refreshes)
        assertEquals("fresh-access", sessions.session?.accessToken)
    }
}
