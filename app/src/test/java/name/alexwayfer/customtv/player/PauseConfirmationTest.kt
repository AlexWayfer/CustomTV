package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PauseConfirmationTest {
    private val confirmation = PauseConfirmation()

    @Test
    fun `a playing report with no pause sent counts`() {
        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `a playing report right after the app's pause is stale`() {
        confirmation.sent(loading = false)

        assertEquals(PauseReport.Ignore, confirmation.report(playing = true))
    }

    @Test
    fun `playing reports stay stale until the page reports the pause`() {
        confirmation.sent(loading = false)
        confirmation.report(playing = true)

        assertEquals(PauseReport.Ignore, confirmation.report(playing = true))
    }

    @Test
    fun `the page's paused report counts`() {
        confirmation.sent(loading = false)

        assertEquals(PauseReport.Accept, confirmation.report(playing = false))
    }

    @Test
    fun `playing after the confirmed pause counts, as when the viewer plays on the page`() {
        confirmation.sent(loading = false)
        confirmation.report(playing = false)

        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `the app's play ends the wait for the pause`() {
        confirmation.sent(loading = false)
        confirmation.clear()

        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `the page running the pause ends the wait for it`() {
        confirmation.sent(loading = false)
        confirmation.ran()

        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `a stream paused while loading that Twitch starts once loaded started on its own`() {
        confirmation.sent(loading = true)
        confirmation.ran()
        confirmation.report(playing = false)

        assertEquals(PauseReport.Retry, confirmation.report(playing = true))
    }

    @Test
    fun `only the first start after a pause while loading is Twitch's own`() {
        confirmation.sent(loading = true)
        confirmation.ran()
        confirmation.report(playing = true)
        confirmation.ran()

        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `a paused report is not a start`() {
        confirmation.sent(loading = true)
        confirmation.ran()

        assertEquals(PauseReport.Accept, confirmation.report(playing = false))
        assertEquals(PauseReport.Retry, confirmation.report(playing = true))
    }

    @Test
    fun `the app's play after a pause while loading lets the start count`() {
        confirmation.sent(loading = true)
        confirmation.clear()

        assertEquals(PauseReport.Accept, confirmation.report(playing = true))
    }

    @Test
    fun `another pause command keeps the chat-only pause in force`() {
        confirmation.sent(loading = false, holdUntilPlay = true)
        confirmation.sent(loading = false)
        confirmation.ran()

        assertEquals(PauseReport.Retry, confirmation.report(playing = true))
    }
}
