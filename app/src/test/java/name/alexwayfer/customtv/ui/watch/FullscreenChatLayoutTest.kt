package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenChatLayoutTest {
    private val overlayRight = fullscreenChatBounds(FullscreenChatMode.Overlay, FullscreenChatSide.Right, 2000)
    private val overlayLeft = fullscreenChatBounds(FullscreenChatMode.Overlay, FullscreenChatSide.Left, 2000)
    private val columnRight = fullscreenChatBounds(FullscreenChatMode.Column, FullscreenChatSide.Right, 2000)

    @Test
    fun noChatLeavesTheWholeAreaToThePlayer() {
        assertNull(fullscreenChatBounds(FullscreenChatMode.Hidden, FullscreenChatSide.Right, 2000))
    }

    @Test
    fun anOverlaidChatOnTheRightLiesOverTheRightOfTheVideo() {
        assertEquals(
            FullscreenChatBounds(
                playerX = 0,
                playerWidth = 2000,
                chatX = 1300,
                chatWidth = 700,
                overPlayer = true,
                chatOnLeft = false,
            ),
            overlayRight,
        )
    }

    @Test
    fun aColumnChatOnTheLeftPushesThePlayerRight() {
        assertEquals(
            FullscreenChatBounds(
                playerX = 700,
                playerWidth = 1300,
                chatX = 0,
                chatWidth = 700,
                overPlayer = false,
                chatOnLeft = true,
            ),
            fullscreenChatBounds(FullscreenChatMode.Column, FullscreenChatSide.Left, 2000),
        )
    }

    @Test
    fun anEmptyAreaGivesNoWidthToEither() {
        val bounds = fullscreenChatBounds(FullscreenChatMode.Column, FullscreenChatSide.Right, -10)!!
        assertEquals(0, bounds.playerWidth)
        assertEquals(0, bounds.chatWidth)
    }

    @Test
    fun atRestTheVideoIsCenteredAndAnOverlaidChatStopsAboveTheControlBar() {
        assertEquals(
            FullscreenPlacement(
                videoX = 200,
                videoY = 0,
                videoWidth = 1600,
                videoHeight = 900,
                videoScale = 1f,
                chatX = 1300,
                chatWidth = 700,
                chatBottomInset = 115,
            ),
            placement(overlayRight, keyboardHeightPx = 0, keyboardProgress = 0f, controlBarPx = 115),
        )
    }

    @Test
    fun anOverlaidChatTakesTheFullHeightWhileTheControlBarIsHidden() {
        val placement = atRest(overlayRight, FullscreenChatInsets(controlBarPx = 115, controlBarShown = 0f))
        assertEquals(0, placement.chatBottomInset)
    }

    @Test
    fun anOverlaidChatRisesWithTheControlBarAsItShows() {
        val placement = atRest(overlayRight, FullscreenChatInsets(controlBarPx = 115, controlBarShown = 0.5f))
        assertEquals(58, placement.chatBottomInset)
    }

    @Test
    fun anOverlaidChatKeepsItsEdgeGapAboveAndBelowWithoutTheControlBar() {
        val placement = atRest(
            overlayRight,
            FullscreenChatInsets(controlBarPx = 115, controlBarShown = 0f, chatEdgePx = 40),
        )
        assertEquals(40, placement.chatBottomInset)
        assertEquals(40, placement.chatTopInset)
    }

    @Test
    fun anOverlaidChatRestsOnTheShownControlBarAndKeepsItsTopGap() {
        val placement = atRest(
            overlayRight,
            FullscreenChatInsets(controlBarPx = 115, controlBarShown = 1f, chatEdgePx = 40),
        )
        assertEquals(115, placement.chatBottomInset)
        assertEquals(40, placement.chatTopInset)
    }

    @Test
    fun anOverlaidChatMovesFromItsEdgeGapToTheControlBarAsItShows() {
        val placement = atRest(
            overlayRight,
            FullscreenChatInsets(controlBarPx = 115, controlBarShown = 0.5f, chatEdgePx = 40),
        )
        assertEquals(78, placement.chatBottomInset)
    }

    @Test
    fun withTheKeyboardUpTheOverlaidChatDropsItsEdgeGaps() {
        val placement = fullscreenPlacement(
            overlayRight,
            2000,
            900,
            FullscreenKeyboard(heightPx = 400, progress = 1f),
            FullscreenChatInsets(controlBarPx = 115, chatEdgePx = 40),
        )
        assertEquals(0, placement.chatBottomInset)
        assertEquals(0, placement.chatTopInset)
    }

    @Test
    fun aColumnChatHasNoEdgeGaps() {
        val placement = atRest(columnRight, FullscreenChatInsets(controlBarPx = 115, chatEdgePx = 40))
        assertEquals(0, placement.chatBottomInset)
        assertEquals(0, placement.chatTopInset)
    }

    @Test
    fun atRestAColumnChatReachesTheBottomAndTheNarrowVideoCentersVertically() {
        val placement = atRest(columnRight, FullscreenChatInsets(controlBarPx = 115))
        assertEquals(0, placement.videoX)
        assertEquals(84, placement.videoY)
        assertEquals(1300, placement.videoWidth)
        assertEquals(731, placement.videoHeight)
        assertEquals(0, placement.chatBottomInset)
    }

    @Test
    fun withTheKeyboardUpTheVideoShrinksIntoTheCornerAwayFromTheChat() {
        assertEquals(
            FullscreenPlacement(
                videoX = 0,
                videoY = 0,
                videoWidth = 1600,
                videoHeight = 900,
                videoScale = 0.5f,
                chatX = 800,
                chatWidth = 1200,
                chatBottomInset = 0,
            ),
            placement(overlayRight, keyboardHeightPx = 450, keyboardProgress = 1f, controlBarPx = 115),
        )
    }

    @Test
    fun withTheKeyboardUpAChatOnTheLeftPutsTheVideoInTheRightCorner() {
        val placement = placement(overlayLeft, keyboardHeightPx = 450, keyboardProgress = 1f, controlBarPx = 0)
        assertEquals(1200, placement.videoX)
        assertEquals(0, placement.chatX)
        assertEquals(1200, placement.chatWidth)
    }

    @Test
    fun aShortKeyboardStillLeavesTheChatItsUsualWidth() {
        val placement = placement(overlayRight, keyboardHeightPx = 100, keyboardProgress = 1f, controlBarPx = 0)
        assertEquals(0.8125f, placement.videoScale, 0.0001f)
        assertEquals(700, placement.chatWidth)
    }

    @Test
    fun halfwayTheVideoAndTheChatAreHalfwayToo() {
        assertEquals(
            FullscreenPlacement(
                videoX = 100,
                videoY = 0,
                videoWidth = 1600,
                videoHeight = 900,
                videoScale = 0.75f,
                chatX = 1050,
                chatWidth = 950,
                chatBottomInset = 58,
            ),
            placement(overlayRight, keyboardHeightPx = 450, keyboardProgress = 0.5f, controlBarPx = 115),
        )
    }

    @Test
    fun withoutAChatTheKeyboardMovesNothing() {
        val placement = placement(null, keyboardHeightPx = 450, keyboardProgress = 1f, controlBarPx = 115)
        assertEquals(200, placement.videoX)
        assertEquals(1f, placement.videoScale, 0.0001f)
        assertEquals(0, placement.chatWidth)
    }

    @Test
    fun aZeroAreaPlacesAnEmptyVideo() {
        val placement = fullscreenPlacement(
            overlayRight,
            0,
            0,
            FullscreenKeyboard(heightPx = 450, progress = 1f),
            FullscreenChatInsets(controlBarPx = 0),
        )
        assertEquals(0, placement.videoWidth)
        assertEquals(0, placement.videoHeight)
    }

    @Test
    fun anOpeningKeyboardIsAsTallAsItsTarget() {
        assertEquals(800, fullscreenKeyboardHeightPx(imePx = 200, targetPx = 800, rememberedPx = 0))
    }

    @Test
    fun aClosingKeyboardKeepsItsRememberedHeight() {
        assertEquals(800, fullscreenKeyboardHeightPx(imePx = 300, targetPx = 0, rememberedPx = 800))
    }

    @Test
    fun aClosedKeyboardHasNoHeight() {
        assertEquals(0, fullscreenKeyboardHeightPx(imePx = 0, targetPx = 0, rememberedPx = 800))
    }

    @Test
    fun theEmotePickerStandsInForTheKeyboardWithItsOwnHeight() {
        assertEquals(600 to 600, fullscreenComposerHeightsPx(imePx = 0, imeTargetPx = 0, pickerPx = 600))
    }

    @Test
    fun thePickerFillsTheKeyboardsPlaceUnderTheVideo() {
        assertEquals(300, fullscreenPickerFillTopPx(areaHeightPx = 900, pickerPx = 600))
        assertEquals(0, fullscreenPickerFillTopPx(areaHeightPx = 400, pickerPx = 600))
        assertEquals(null, fullscreenPickerFillTopPx(areaHeightPx = 900, pickerPx = 0))
    }

    @Test
    fun withoutThePickerTheKeyboardCountsAsItIs() {
        assertEquals(200 to 600, fullscreenComposerHeightsPx(imePx = 200, imeTargetPx = 600, pickerPx = 0))
    }

    @Test
    fun aPickerAfterTheKeyboardKeepsTheKeyboardLayout() {
        val memory = FullscreenKeyboardMemory()
        memory.measure(imePx = 600, imeTargetPx = 600, pickerPx = 0)
        assertEquals(600 to 1f, memory.measure(imePx = 300, imeTargetPx = 0, pickerPx = 600))
    }

    @Test
    fun theClosedMessageBarIsSeeThroughOverTheVideoOnly() {
        assertEquals(FULLSCREEN_OVERLAY_CHAT_ALPHA, chatInputBarAlpha(overVideo = true, composerOpen = false), 0.0001f)
        assertEquals(1f, chatInputBarAlpha(overVideo = true, composerOpen = true), 0.0001f)
        assertEquals(1f, chatInputBarAlpha(overVideo = false, composerOpen = false), 0.0001f)
    }

    @Test
    fun theKeyboardMemoryReportsHowFarTheKeyboardIsOpen() {
        val memory = FullscreenKeyboardMemory()
        assertEquals(800 to 0.25f, memory.measure(imePx = 200, imeTargetPx = 800, pickerPx = 0))
        assertEquals(800 to 0.5f, memory.measure(imePx = 400, imeTargetPx = 0, pickerPx = 0))
        assertEquals(0 to 0f, memory.measure(imePx = 0, imeTargetPx = 0, pickerPx = 0))
    }

    @Test
    fun thePlayerButtonsMoveClearOfAChatOverTheRightOfTheVideo() {
        val rest = atRest(overlayRight, FullscreenChatInsets(controlBarPx = 0))
        assertEquals(500, fullscreenPlayerButtonsEndPx(overlayRight, rest))
    }

    @Test
    fun thePlayerButtonsStayInTheCornerOtherwise() {
        assertEquals(0, fullscreenPlayerButtonsEndPx(overlayLeft, placement(overlayLeft, 0, 0f, 0)))
        assertEquals(0, fullscreenPlayerButtonsEndPx(columnRight, placement(columnRight, 0, 0f, 0)))
        assertEquals(0, fullscreenPlayerButtonsEndPx(null, placement(null, 0, 0f, 0)))
    }

    @Test
    fun theStreamInfoBelowShrinksTheVideoToTheRoomAboveIt() {
        val placement = atRest(null, FullscreenChatInsets(controlBarPx = 0, infoBarPx = 180))
        assertEquals(1600, placement.videoWidth)
        assertEquals(900, placement.videoHeight)
        assertEquals(0.8f, placement.videoScale, 0.0001f)
        assertEquals(360, placement.videoX)
        assertEquals(0, placement.videoY)
    }

    @Test
    fun aVideoNarrowerThanTheRoomAboveTheInfoKeepsItsSizeAndCentersInTheRoom() {
        val placement = atRest(columnRight, FullscreenChatInsets(controlBarPx = 0, infoBarPx = 100))
        assertEquals(1f, placement.videoScale, 0.0001f)
        assertEquals(0, placement.videoX)
        // 731 px of video centered in the 800 px above the stream info.
        assertEquals(34, placement.videoY)
    }

    @Test
    fun anOverlaidChatRisesAboveTheStreamInfoAndTheControlBar() {
        val placement = atRest(overlayRight, FullscreenChatInsets(controlBarPx = 115, infoBarPx = 180))
        assertEquals(295, placement.chatBottomInset)
    }

    @Test
    fun aColumnChatTakesTheFullHeightBesideTheStreamInfo() {
        val placement = atRest(columnRight, FullscreenChatInsets(controlBarPx = 115, infoBarPx = 180))
        assertEquals(0, placement.chatBottomInset)
    }

    @Test
    fun theKeyboardShrinksTheVideoOnFromWhereTheStreamInfoLeftIt() {
        val placement = fullscreenPlacement(
            overlayRight,
            2000,
            900,
            FullscreenKeyboard(heightPx = 450, progress = 0.5f),
            FullscreenChatInsets(controlBarPx = 0, infoBarPx = 180),
        )
        assertEquals(0.65f, placement.videoScale, 0.0001f)
    }

    @Test
    fun theStreamInfoRunsUnderTheVideoColumnBesideAColumnChat() {
        assertEquals(FullscreenInfoBarSpan(x = 0, width = 1300), fullscreenInfoBarSpan(columnRight, 2000))
    }

    @Test
    fun theStreamInfoRunsTheWholeWidthUnderAnOverlaidChat() {
        assertEquals(FullscreenInfoBarSpan(x = 0, width = 2000), fullscreenInfoBarSpan(overlayLeft, 2000))
    }

    @Test
    fun theStreamInfoRunsTheWholeWidthWithTheChatOff() {
        assertEquals(FullscreenInfoBarSpan(x = 0, width = 2000), fullscreenInfoBarSpan(null, 2000))
    }

    @Test
    fun theStreamInfoRestsOnTheBottomWhileShown() {
        assertEquals(720, fullscreenInfoBarTopPx(areaHeightPx = 900, barHeightPx = 180, shown = 1f, hidden = 0f))
    }

    @Test
    fun theHiddenStreamInfoWaitsUnderTheBottomEdge() {
        assertEquals(900, fullscreenInfoBarTopPx(areaHeightPx = 900, barHeightPx = 180, shown = 0f, hidden = 0f))
    }

    @Test
    fun theStreamInfoRisesWithTheStreamAsItHides() {
        assertEquals(360, fullscreenInfoBarTopPx(areaHeightPx = 900, barHeightPx = 180, shown = 1f, hidden = 0.5f))
    }

    @Test
    fun theStreamInfoReachesTheTopWithTheStreamAllHidden() {
        assertEquals(0, fullscreenInfoBarTopPx(areaHeightPx = 900, barHeightPx = 180, shown = 1f, hidden = 1f))
    }

    @Test
    fun aStreamInfoTallerThanTheAreaStaysAtTheTop() {
        assertEquals(0, fullscreenInfoBarTopPx(areaHeightPx = 100, barHeightPx = 180, shown = 1f, hidden = 0f))
    }

    @Test
    fun theStreamInfoShowsWithTheControlsInFullScreen() {
        assertTrue(fullscreenInfoBarShown(controls = true, composerOpen = false, fullscreen = true, covers = false))
    }

    @Test
    fun theStreamInfoHidesWithTheControls() {
        assertFalse(fullscreenInfoBarShown(controls = false, composerOpen = false, fullscreen = true, covers = false))
    }

    @Test
    fun theStreamInfoMakesWayForTheKeyboard() {
        assertFalse(fullscreenInfoBarShown(controls = true, composerOpen = true, fullscreen = true, covers = false))
    }

    @Test
    fun theStreamInfoKeepsItsRoomWhileTheStreamSlidesOutOfFullScreen() {
        assertTrue(fullscreenInfoBarShown(controls = true, composerOpen = false, fullscreen = false, covers = true))
    }

    @Test
    fun outOfFullScreenThePlayerHasNoStreamInfoBelowIt() {
        assertFalse(fullscreenInfoBarShown(controls = true, composerOpen = false, fullscreen = false, covers = false))
    }

    /** [fullscreenPlacement] in a 2000 × 900 area. */
    private fun placement(
        bounds: FullscreenChatBounds?,
        keyboardHeightPx: Int,
        keyboardProgress: Float,
        controlBarPx: Int,
    ) = fullscreenPlacement(
        bounds,
        2000,
        900,
        FullscreenKeyboard(keyboardHeightPx, keyboardProgress),
        FullscreenChatInsets(controlBarPx),
    )

    /** [fullscreenPlacement] in a 2000 × 900 area without the keyboard. */
    private fun atRest(bounds: FullscreenChatBounds?, insets: FullscreenChatInsets) =
        fullscreenPlacement(bounds, 2000, 900, FullscreenKeyboard(), insets)
}
