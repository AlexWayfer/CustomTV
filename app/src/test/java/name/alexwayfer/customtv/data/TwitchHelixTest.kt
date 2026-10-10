package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchHelixTest {
    @Test
    fun helixRequestCarriesTheUserTokenAndClientId() {
        val request = helixRequest("https://api.twitch.tv/helix/users", "token", "client").build()

        assertEquals("https://api.twitch.tv/helix/users", request.url.toString())
        assertEquals("Bearer token", request.header("Authorization"))
        assertEquals("client", request.header("Client-Id"))
        assertEquals("GET", request.method)
    }
}
