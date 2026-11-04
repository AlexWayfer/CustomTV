package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkTrackerTest {
    @Test
    fun theNetworkInUseAtStartIsNoSwitch() {
        assertFalse(DefaultNetworkTracker(initial = 1L).report(1L, validated = true))
    }

    @Test
    fun anotherValidatedNetworkIsASwitch() {
        assertTrue(DefaultNetworkTracker(initial = 1L).report(2L, validated = true))
    }

    @Test
    fun aNetworkNotValidatedYetIsNoSwitchUntilItIs() {
        val tracker = DefaultNetworkTracker(initial = 1L)
        assertFalse(tracker.report(2L, validated = false))
        assertTrue(tracker.report(2L, validated = true))
    }

    @Test
    fun theSameNetworkReportedAgainIsNoSwitch() {
        val tracker = DefaultNetworkTracker(initial = 1L)
        tracker.report(2L, validated = true)
        assertFalse(tracker.report(2L, validated = true))
    }

    @Test
    fun aNetworkAfterNoneAtStartIsASwitch() {
        assertTrue(DefaultNetworkTracker(initial = null).report(1L, validated = true))
    }

    @Test
    fun goingBackToTheFirstNetworkIsASwitch() {
        val tracker = DefaultNetworkTracker(initial = 1L)
        tracker.report(2L, validated = true)
        assertTrue(tracker.report(1L, validated = true))
    }

    @Test
    fun aSwitchWatchesALiveStreamForRecovery() {
        assertTrue(networkSwitchWatchesPlayer(live = true, userPaused = false))
    }

    @Test
    fun aSwitchLeavesARecordingAlone() {
        assertFalse(networkSwitchWatchesPlayer(live = false, userPaused = false))
    }

    @Test
    fun aSwitchLeavesAStreamTheViewerPausedAlone() {
        assertFalse(networkSwitchWatchesPlayer(live = true, userPaused = true))
    }
}
