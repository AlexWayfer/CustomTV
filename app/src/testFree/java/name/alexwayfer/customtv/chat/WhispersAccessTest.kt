package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.auth.TWITCH_LOGIN_SCOPES
import name.alexwayfer.customtv.auth.twitchLoginScopes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WhispersAccessTest {
    @Test
    fun theFreeBuildHidesWhispers() {
        assertFalse(whispersNavEnabled())
        assertEquals(TWITCH_LOGIN_SCOPES, twitchLoginScopes())
    }
}
