package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamChromeTapTest {
    /** [chromeChangeSyncsControls] on a player in sight unless [playerOutOfSight]. */
    private fun syncs(
        headerExpanded: Boolean,
        minimizeControl: Boolean,
        visible: Boolean,
        playerOutOfSight: Boolean = false,
    ) = chromeChangeSyncsControls(headerExpanded, minimizeControl, visible, playerOutOfSight)

    @Test
    fun openingTheCollapsedPlaqueSyncsControls() {
        assertTrue(syncs(headerExpanded = false, minimizeControl = false, visible = true))
    }

    @Test
    fun closingTheExpandedPlaqueSyncsControls() {
        assertTrue(syncs(headerExpanded = true, minimizeControl = true, visible = false))
    }

    @Test
    fun expandingFromTheMiniPlayerWithTheHeaderOpenSyncsControls() {
        assertTrue(syncs(headerExpanded = true, minimizeControl = false, visible = true))
    }

    @Test
    fun aRepeatedShowWithTheSameStateDoesNotSyncAgain() {
        assertFalse(syncs(headerExpanded = true, minimizeControl = true, visible = true))
        assertFalse(syncs(headerExpanded = false, minimizeControl = false, visible = false))
    }

    @Test
    fun theStreamInfoOverAHiddenStreamOpensWithoutTappingThePlayer() {
        assertFalse(syncs(headerExpanded = false, minimizeControl = false, visible = true, playerOutOfSight = true))
    }

    @Test
    fun theStreamInfoOverAHiddenStreamClosesWithoutTappingThePlayer() {
        assertFalse(syncs(headerExpanded = true, minimizeControl = true, visible = false, playerOutOfSight = true))
    }

    /** [playerControlsSync] on a playing stream unless [recording] or [paused]. */
    private fun controlsSync(
        wanted: Boolean,
        controlsVisible: Boolean,
        recording: Boolean = false,
        paused: Boolean = false,
    ) = playerControlsSync(recording, paused, wanted, controlsVisible)

    @Test
    fun hiddenStreamControlsAreTappedToShow() {
        assertEquals(PlayerControlsSync.Tap, controlsSync(wanted = true, controlsVisible = false))
    }

    @Test
    fun visibleStreamControlsAreTappedToHide() {
        assertEquals(PlayerControlsSync.Tap, controlsSync(wanted = false, controlsVisible = true))
    }

    @Test
    fun hiddenRecordingControlsShowWithoutATapThatWouldPause() {
        assertEquals(
            PlayerControlsSync.ShowWithoutTap,
            controlsSync(wanted = true, controlsVisible = false, recording = true),
        )
    }

    @Test
    fun visibleRecordingControlsHideWithoutATapThatWouldPause() {
        assertEquals(
            PlayerControlsSync.HideWithoutTap,
            controlsSync(wanted = false, controlsVisible = true, recording = true),
        )
    }

    @Test
    fun aPausedStreamsControlsHideWithoutATapThatWouldPlayIt() {
        assertEquals(
            PlayerControlsSync.HideWithoutTap,
            controlsSync(wanted = false, controlsVisible = true, paused = true),
        )
    }

    @Test
    fun aPausedStreamsControlsShowWithoutATapThatWouldPlayIt() {
        assertEquals(
            PlayerControlsSync.ShowWithoutTap,
            controlsSync(wanted = true, controlsVisible = false, paused = true),
        )
    }

    @Test
    fun controlsAlreadyInTheWantedStateAreLeftAlone() {
        assertEquals(PlayerControlsSync.None, controlsSync(wanted = true, controlsVisible = true))
        assertEquals(
            PlayerControlsSync.None,
            controlsSync(wanted = false, controlsVisible = false, recording = true),
        )
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
    fun aTapWhereOnlyTheAppTakesTapsIsTheApps() {
        assertEquals(PlayerTouchEnd.AppTap, playerTouchEnd(withinTapSlop = true, offPage = true, onDisclosure = false))
    }

    @Test
    fun aSmearedTouchOnARecordingsVideoIsDroppedSoItDoesNotPause() {
        assertEquals(
            PlayerTouchEnd.Dropped,
            playerTouchEnd(withinTapSlop = false, offPage = true, onDisclosure = false),
        )
    }

    @Test
    fun aTapOnAButtonReachesThePageAndTheChromeFollows() {
        assertEquals(
            PlayerTouchEnd.PageTap,
            playerTouchEnd(withinTapSlop = true, offPage = false, onDisclosure = false),
        )
    }

    @Test
    fun aTapOnTheAudiencePlaqueIsThePagesAloneAndKeepsTheChrome() {
        assertEquals(
            PlayerTouchEnd.DisclosureTap,
            playerTouchEnd(withinTapSlop = true, offPage = false, onDisclosure = true),
        )
    }

    @Test
    fun aSmearedTouchOffTheVideoReachesThePageAsItIs() {
        assertEquals(
            PlayerTouchEnd.PageTouch,
            playerTouchEnd(withinTapSlop = false, offPage = false, onDisclosure = false),
        )
        assertEquals(
            PlayerTouchEnd.PageTouch,
            playerTouchEnd(withinTapSlop = false, offPage = false, onDisclosure = true),
        )
    }

    @Test
    fun aTapOnThePlaqueOfTheMiniPlayerExpandsIt() {
        assertEquals(PlayerTouchEnd.AppTap, playerTouchEnd(withinTapSlop = true, offPage = true, onDisclosure = true))
    }

    /** [headerCollapseTimerRuns] for an expanded header over a player in sight that has started. */
    private fun timerRuns(
        headerExpanded: Boolean = true,
        minimizeControl: Boolean = true,
        held: Boolean = false,
        videoLoading: Boolean = false,
        playerOutOfSight: Boolean = false,
    ) = headerCollapseTimerRuns(headerExpanded, minimizeControl, held, videoLoading, playerOutOfSight)

    @Test
    fun anExpandedHeaderCollapsesOnTheTimer() {
        assertTrue(timerRuns())
        assertTrue(timerRuns(minimizeControl = false))
    }

    @Test
    fun anOpenMenuKeepsTheHeaderExpanded() {
        assertFalse(timerRuns(held = true))
    }

    @Test
    fun aLoadingVideoKeepsTheHeaderExpanded() {
        assertFalse(timerRuns(videoLoading = true))
    }

    @Test
    fun aLoadingVideoOutOfSightLetsTheHeaderCollapse() {
        assertTrue(timerRuns(videoLoading = true, playerOutOfSight = true))
    }

    @Test
    fun anOpenMenuKeepsTheHeaderOverAHiddenStreamExpanded() {
        assertFalse(timerRuns(held = true, playerOutOfSight = true))
    }

    @Test
    fun aCollapsedHeaderHasNoTimer() {
        assertFalse(timerRuns(headerExpanded = false, minimizeControl = false))
    }

    @Test
    fun aShrinkingPlayerHidesTheChrome() {
        assertEquals(ShrinkChrome.Hide, chromeOnPlayerShrink(shrunk = true, wasShrunk = false))
    }

    @Test
    fun aPlayerBackToFullSizeShowsTheWholeChrome() {
        assertEquals(ShrinkChrome.Show, chromeOnPlayerShrink(shrunk = false, wasShrunk = true))
    }

    @Test
    fun aPlayerMovingBetweenTheCornerAndPictureInPictureKeepsTheChromeHidden() {
        assertEquals(ShrinkChrome.Hide, chromeOnPlayerShrink(shrunk = true, wasShrunk = true))
    }

    @Test
    fun aFullPlayerThatStaysFullLeavesTheChromeAlone() {
        assertEquals(ShrinkChrome.Keep, chromeOnPlayerShrink(shrunk = false, wasShrunk = false))
    }

    @Test
    fun controlsAppearingOnTheirOwnExpandTheCollapsedHeader() {
        assertTrue(showsChrome())
    }

    @Test
    fun controlsAppearingAddTheMinimizeButtonToARevealedHeader() {
        assertTrue(showsChrome(headerExpanded = true))
    }

    @Test
    fun controlsAppearingLeaveTheShownChromeAlone() {
        assertFalse(showsChrome(headerExpanded = true, minimizeControl = true))
    }

    @Test
    fun controlsAppearingInTheMiniPlayerDoNotExpandTheHeader() {
        assertFalse(showsChrome(minimized = true))
    }

    @Test
    fun hidingControlsDoNotExpandTheHeader() {
        assertFalse(showsChrome(controlsVisible = false))
    }

    @Test
    fun controlsOfAPausedPlayerOutOfSightDoNotExpandTheHeader() {
        assertFalse(showsChrome(playerOutOfSight = true))
    }

    /** [controlsShowChrome] for controls that appear over a collapsed header in the full player. */
    private fun showsChrome(
        controlsVisible: Boolean = true,
        headerExpanded: Boolean = false,
        minimizeControl: Boolean = false,
        minimized: Boolean = false,
        playerOutOfSight: Boolean = false,
    ) = controlsShowChrome(
        controlsVisible = controlsVisible,
        headerExpanded = headerExpanded,
        minimizeControl = minimizeControl,
        minimized = minimized,
        playerOutOfSight = playerOutOfSight,
    )

    @Test
    fun visiblePlayerControlsKeepTheHeaderExpandedAfterTheTimer() {
        assertFalse(headerCollapsesAfterTimer(playerControlsVisible = true, playerOutOfSight = false))
    }

    @Test
    fun theHeaderCollapsesAfterTheTimerOnceControlsAreHidden() {
        assertTrue(headerCollapsesAfterTimer(playerControlsVisible = false, playerOutOfSight = false))
    }

    @Test
    fun theControlsOfAPausedPlayerOutOfSightDoNotHoldTheHeader() {
        assertTrue(headerCollapsesAfterTimer(playerControlsVisible = true, playerOutOfSight = true))
    }

    @Test
    fun anOpenComposerHidesTheStreamInfo() {
        assertFalse(streamInfoShownUnderPlayer(fullscreen = false, composerOpen = true))
    }

    @Test
    fun theStreamInfoShowsUnderThePlayerWhileTheComposerIsClosed() {
        assertTrue(streamInfoShownUnderPlayer(fullscreen = false, composerOpen = false))
    }

    @Test
    fun aFullScreenPlayerHidesTheStreamInfo() {
        assertFalse(streamInfoShownUnderPlayer(fullscreen = true, composerOpen = false))
        assertFalse(streamInfoShownUnderPlayer(fullscreen = true, composerOpen = true))
    }
}
