package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPageReloadsTest {
    @Test
    fun theFirstReloadWaitsTenSeconds() {
        assertEquals(10_000L, PlayerPageReloads.delayMs(0))
    }

    @Test
    fun theSecondReloadWaitsTwentySeconds() {
        assertEquals(20_000L, PlayerPageReloads.delayMs(1))
    }

    @Test
    fun noReloadIsLeftAfterTheLastDelay() {
        assertEquals(2, PlayerPageReloads.limit)
        assertNull(PlayerPageReloads.delayMs(PlayerPageReloads.limit))
    }

    @Test
    fun hiddenVideoAfterTheErrorReloadReloadsThePage() {
        assertTrue(PlayerPageReloads.reloadsAfterErrorReload(videoShown = false, userPaused = false))
    }

    @Test
    fun videoThatCameBackIsNotReloaded() {
        assertFalse(PlayerPageReloads.reloadsAfterErrorReload(videoShown = true, userPaused = false))
    }

    @Test
    fun aPausedPlayerIsNotReloaded() {
        assertFalse(PlayerPageReloads.reloadsAfterErrorReload(videoShown = false, userPaused = true))
    }
}
