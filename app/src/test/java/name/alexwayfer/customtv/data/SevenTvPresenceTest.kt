package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SevenTvPresenceTest {
    @Test
    fun aPresenceNamesTheTwitchChannel() {
        val body = sevenTvPresenceBody("71092938")

        assertEquals(setOf("kind", "data"), body.keys().asSequence().toSet())
        assertEquals(1, body.getInt("kind"))
        assertEquals("TWITCH", body.getJSONObject("data").getString("platform"))
        assertEquals("71092938", body.getJSONObject("data").getString("id"))
    }

    @Test
    fun aPresenceGoesOutFirstAndThenOnlyAfterTheGap() {
        assertTrue(sevenTvPresenceDue(lastSentAtMillis = null, nowMillis = 1_000L))
        assertFalse(sevenTvPresenceDue(lastSentAtMillis = 1_000L, nowMillis = 10_999L))
        assertTrue(sevenTvPresenceDue(lastSentAtMillis = 1_000L, nowMillis = 11_000L))
    }
}
