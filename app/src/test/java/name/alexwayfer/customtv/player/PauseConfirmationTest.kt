package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseConfirmationTest {
    private val confirmation = PauseConfirmation()

    @Test
    fun `a playing report with no pause sent counts`() {
        assertFalse(confirmation.stale(playing = true))
    }

    @Test
    fun `a playing report right after the app's pause is stale`() {
        confirmation.sent(loading = false)

        assertTrue(confirmation.stale(playing = true))
    }

    @Test
    fun `playing reports stay stale until the page reports the pause`() {
        confirmation.sent(loading = false)
        confirmation.stale(playing = true)

        assertTrue(confirmation.stale(playing = true))
    }

    @Test
    fun `the page's paused report counts`() {
        confirmation.sent(loading = false)

        assertFalse(confirmation.stale(playing = false))
    }

    @Test
    fun `playing after the confirmed pause counts, as when the viewer plays on the page`() {
        confirmation.sent(loading = false)
        confirmation.stale(playing = false)

        assertFalse(confirmation.stale(playing = true))
        assertFalse(confirmation.startedOnItsOwn(playing = true))
    }

    @Test
    fun `the app's play ends the wait for the pause`() {
        confirmation.sent(loading = false)
        confirmation.clear()

        assertFalse(confirmation.stale(playing = true))
    }

    @Test
    fun `the page running the pause ends the wait for it`() {
        confirmation.sent(loading = false)
        confirmation.ran()

        assertFalse(confirmation.stale(playing = true))
    }

    @Test
    fun `a stream paused while loading that Twitch starts once loaded started on its own`() {
        confirmation.sent(loading = true)
        confirmation.ran()
        confirmation.stale(playing = false)

        assertFalse(confirmation.stale(playing = true))
        assertTrue(confirmation.startedOnItsOwn(playing = true))
    }

    @Test
    fun `only the first start after a pause while loading is Twitch's own`() {
        confirmation.sent(loading = true)
        confirmation.ran()
        confirmation.startedOnItsOwn(playing = true)

        assertFalse(confirmation.startedOnItsOwn(playing = true))
    }

    @Test
    fun `a paused report is not a start`() {
        confirmation.sent(loading = true)
        confirmation.ran()

        assertFalse(confirmation.startedOnItsOwn(playing = false))
        assertTrue(confirmation.startedOnItsOwn(playing = true))
    }

    @Test
    fun `the app's play after a pause while loading lets the start count`() {
        confirmation.sent(loading = true)
        confirmation.clear()

        assertFalse(confirmation.startedOnItsOwn(playing = true))
    }
}
