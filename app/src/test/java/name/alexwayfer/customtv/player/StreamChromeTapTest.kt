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
        assertEquals(
            PlayerControlsSync.Tap,
            playerControlsSync(recording = false, wanted = true, controlsVisible = false),
        )
    }

    @Test
    fun visibleStreamControlsAreTappedToHide() {
        assertEquals(
            PlayerControlsSync.Tap,
            playerControlsSync(recording = false, wanted = false, controlsVisible = true),
        )
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
        assertEquals(
            PlayerControlsSync.None,
            playerControlsSync(recording = false, wanted = true, controlsVisible = true),
        )
        assertEquals(
            PlayerControlsSync.None,
            playerControlsSync(recording = true, wanted = false, controlsVisible = false),
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
            headerCollapseTimerRuns(
                headerExpanded = false,
                minimizeControl = false,
                held = false,
                videoLoading = false,
            ),
        )
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

    /** [controlsShowChrome] for controls that appear over a collapsed header in the full player. */
    private fun showsChrome(
        controlsVisible: Boolean = true,
        headerExpanded: Boolean = false,
        minimizeControl: Boolean = false,
        minimized: Boolean = false,
    ) = controlsShowChrome(
        controlsVisible = controlsVisible,
        headerExpanded = headerExpanded,
        minimizeControl = minimizeControl,
        minimized = minimized,
    )

    @Test
    fun visiblePlayerControlsKeepTheHeaderExpandedAfterTheTimer() {
        assertFalse(headerCollapsesAfterTimer(playerControlsVisible = true))
    }

    @Test
    fun theHeaderCollapsesAfterTheTimerOnceControlsAreHidden() {
        assertTrue(headerCollapsesAfterTimer(playerControlsVisible = false))
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
