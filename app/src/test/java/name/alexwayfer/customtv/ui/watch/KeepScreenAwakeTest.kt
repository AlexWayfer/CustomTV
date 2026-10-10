package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenAwakeTest {
    @Test
    fun theScreenStaysOnWhileARecordingIsOpen() {
        val open = screenAwakeAfterChange(holders = 0, acquire = true)

        assertEquals(1, open.holders)
        assertTrue(open.screenOn)
    }

    @Test
    fun closingTheRecordingLetsTheScreenTurnOff() {
        val closed = screenAwakeAfterChange(holders = 1, acquire = false)

        assertEquals(0, closed.holders)
        assertFalse(closed.screenOn)
    }

    @Test
    fun closingOnePlayerLeavesTheScreenOnForTheOther() {
        val stillOpen = screenAwakeAfterChange(holders = 2, acquire = false)

        assertEquals(1, stillOpen.holders)
        assertTrue(stillOpen.screenOn)
    }
}
