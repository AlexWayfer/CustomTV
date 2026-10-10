package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamChromeTapTest {
    @Test
    fun openingTheCollapsedPlaqueSyncsControls() {
        assertTrue(chromeChangeSyncsControls(headerExpanded = false, minimizeControl = false, visible = true))
    }

    @Test
    fun closingTheExpandedPlaqueSyncsControls() {
        assertTrue(chromeChangeSyncsControls(headerExpanded = true, minimizeControl = true, visible = false))
    }

    @Test
    fun expandingFromTheMiniPlayerWithTheHeaderOpenSyncsControls() {
        assertTrue(chromeChangeSyncsControls(headerExpanded = true, minimizeControl = false, visible = true))
    }

    @Test
    fun aRepeatedShowWithTheSameStateDoesNotSyncAgain() {
        assertFalse(chromeChangeSyncsControls(headerExpanded = true, minimizeControl = true, visible = true))
        assertFalse(chromeChangeSyncsControls(headerExpanded = false, minimizeControl = false, visible = false))
    }

    @Test
    fun hiddenStreamControlsAreTappedToShow() {
        assertEquals(PlayerControlsSync.Tap, playerControlsSync(recording = false, wanted = true, controlsVisible = false))
    }

    @Test
    fun visibleStreamControlsAreTappedToHide() {
        assertEquals(PlayerControlsSync.Tap, playerControlsSync(recording = false, wanted = false, controlsVisible = true))
    }

    @Test
    fun hiddenRecordingControlsShowWithoutATapThatWouldPause() {
        assertEquals(
            PlayerControlsSync.ShowWithoutTap,
            playerControlsSync(recording = true, wanted = true, controlsVisible = false),
        )
    }

    @Test
    fun visibleRecordingControlsHideWithoutATapThatWouldPause() {
        assertEquals(
            PlayerControlsSync.HideWithoutTap,
            playerControlsSync(recording = true, wanted = false, controlsVisible = true),
        )
    }

    @Test
    fun controlsAlreadyInTheWantedStateAreLeftAlone() {
        assertEquals(PlayerControlsSync.None, playerControlsSync(recording = false, wanted = true, controlsVisible = true))
        assertEquals(PlayerControlsSync.None, playerControlsSync(recording = true, wanted = false, controlsVisible = false))
    }

    @Test
    fun playerTapShowsChromeOnlyWhenControlsAreHidden() {
        assertTrue(playerTapShowsChrome(controlsVisible = false))
        assertFalse(playerTapShowsChrome(controlsVisible = true))
    }

    @Test
    fun aTapOnARecordingsVideoStaysOffThePageSoItDoesNotPause() {
        assertTrue(playerTapStaysOffPage(recording = true, touchOnVideo = true))
    }

    @Test
    fun aTapOnARecordingsButtonOrNoteReachesThePage() {
        assertFalse(playerTapStaysOffPage(recording = true, touchOnVideo = false))
    }

    @Test
    fun aStreamTapAlwaysReachesThePage() {
        assertFalse(playerTapStaysOffPage(recording = false, touchOnVideo = true))
        assertFalse(playerTapStaysOffPage(recording = false, touchOnVideo = false))
    }

    @Test
    fun anExpandedHeaderCollapsesOnTheTimer() {
        assertTrue(
            headerCollapseTimerRuns(headerExpanded = true, minimizeControl = true, held = false, videoLoading = false),
        )
        assertTrue(
            headerCollapseTimerRuns(headerExpanded = true, minimizeControl = false, held = false, videoLoading = false),
        )
    }

    @Test
    fun anOpenMenuKeepsTheHeaderExpanded() {
        assertFalse(
            headerCollapseTimerRuns(headerExpanded = true, minimizeControl = true, held = true, videoLoading = false),
        )
    }

    @Test
    fun aLoadingVideoKeepsTheHeaderExpanded() {
        assertFalse(
            headerCollapseTimerRuns(headerExpanded = true, minimizeControl = true, held = false, videoLoading = true),
        )
    }

    @Test
    fun aCollapsedHeaderHasNoTimer() {
        assertFalse(
            headerCollapseTimerRuns(headerExpanded = false, minimizeControl = false, held = false, videoLoading = false),
        )
    }

    @Test
    fun controlsAppearingOnTheirOwnExpandTheCollapsedHeader() {
        assertTrue(
            controlsShowChrome(controlsVisible = true, headerExpanded = false, minimizeControl = false, minimized = false),
        )
    }

    @Test
    fun controlsAppearingAddTheMinimizeButtonToARevealedHeader() {
        assertTrue(
            controlsShowChrome(controlsVisible = true, headerExpanded = true, minimizeControl = false, minimized = false),
        )
    }

    @Test
    fun controlsAppearingLeaveTheShownChromeAlone() {
        assertFalse(
            controlsShowChrome(controlsVisible = true, headerExpanded = true, minimizeControl = true, minimized = false),
        )
    }

    @Test
    fun controlsAppearingInTheMiniPlayerDoNotExpandTheHeader() {
        assertFalse(
            controlsShowChrome(controlsVisible = true, headerExpanded = false, minimizeControl = false, minimized = true),
        )
    }

    @Test
    fun hidingControlsDoNotExpandTheHeader() {
        assertFalse(
            controlsShowChrome(controlsVisible = false, headerExpanded = false, minimizeControl = false, minimized = false),
        )
    }

    @Test
    fun visiblePlayerControlsKeepTheHeaderExpandedAfterTheTimer() {
        assertFalse(headerCollapsesAfterTimer(playerControlsVisible = true))
    }

    @Test
    fun theHeaderCollapsesAfterTheTimerOnceControlsAreHidden() {
        assertTrue(headerCollapsesAfterTimer(playerControlsVisible = false))
    }

    @Test
    fun anOpenComposerKeepsTheExpandedStreamInfoCollapsed() {
        assertFalse(streamInfoExpandedUnderPlayer(headerExpanded = true, composerOpen = true))
    }

    @Test
    fun theStreamInfoExpandsWithTheChromeWhileTheComposerIsClosed() {
        assertTrue(streamInfoExpandedUnderPlayer(headerExpanded = true, composerOpen = false))
        assertFalse(streamInfoExpandedUnderPlayer(headerExpanded = false, composerOpen = false))
    }
}
