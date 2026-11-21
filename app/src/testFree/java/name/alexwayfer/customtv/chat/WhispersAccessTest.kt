package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.auth.TWITCH_LOGIN_SCOPES
import name.alexwayfer.customtv.auth.savedSessionNeedsNewLogin
import name.alexwayfer.customtv.auth.twitchLoginScopes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhispersAccessTest {
    @Test
    fun theFreeBuildHidesWhispers() {
        assertFalse(whispersNavEnabled())
    }

    @Test
    fun normalFreeLoginAddsIrcAndOlderTokensNeedTheNewPermissions() {
        val previous = TWITCH_LOGIN_SCOPES.split(' ').toSet()
        val requested = twitchLoginScopes()

        assertEquals(previous + setOf("chat:read", "chat:edit"), requested.split(' ').toSet())
        assertTrue(savedSessionNeedsNewLogin(previous, requested))
        assertFalse(savedSessionNeedsNewLogin(requested.split(' ').toSet(), requested))
    }
}
