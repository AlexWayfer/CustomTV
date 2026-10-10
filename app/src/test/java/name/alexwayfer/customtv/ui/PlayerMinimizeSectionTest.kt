package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMinimizeSectionTest {
    @Test
    fun backOnHomeWithAMiniStreamKeepsItInPip() {
        assertTrue(homeBackKeepsMiniPlayer(AppSection.Home, liveMiniPlayer = true, videoMiniPlayer = false, inPictureInPicture = false))
    }

    @Test
    fun backOnHomeWithAMiniRecordingKeepsItInPip() {
        assertTrue(homeBackKeepsMiniPlayer(AppSection.Home, liveMiniPlayer = false, videoMiniPlayer = true, inPictureInPicture = false))
    }

    @Test
    fun backOnHomeWithoutAMiniPlayerLeavesTheAppAsUsual() {
        assertFalse(homeBackKeepsMiniPlayer(AppSection.Home, liveMiniPlayer = false, videoMiniPlayer = false, inPictureInPicture = false))
    }

    @Test
    fun backOnAnotherSectionIsLeftToThatSection() {
        assertFalse(homeBackKeepsMiniPlayer(AppSection.Account, liveMiniPlayer = true, videoMiniPlayer = true, inPictureInPicture = false))
    }

    @Test
    fun pipIsOfferedForALiveStreamAndForAnOpenRecording() {
        assertTrue(pictureInPictureAllowed(watching = true, channelLive = true, liveClosedWithPip = false, recordingOpen = false))
        assertTrue(pictureInPictureAllowed(watching = false, channelLive = false, liveClosedWithPip = false, recordingOpen = true))
    }

    @Test
    fun pipIsNotOfferedForAnOfflineOrDismissedStream() {
        assertFalse(pictureInPictureAllowed(watching = true, channelLive = false, liveClosedWithPip = false, recordingOpen = false))
        assertFalse(pictureInPictureAllowed(watching = true, channelLive = true, liveClosedWithPip = true, recordingOpen = false))
    }

    @Test
    fun theMiniPlayerHidesOnSettings() {
        assertTrue(miniPlayerHidden(AppSection.Settings, minimized = true, inPictureInPicture = false))
    }

    @Test
    fun theMiniPlayerHidesOnWhispers() {
        assertTrue(miniPlayerHidden(AppSection.Whispers, minimized = true, inPictureInPicture = false))
    }

    @Test
    fun theMiniPlayerStaysOnOtherSections() {
        assertFalse(miniPlayerHidden(AppSection.Home, minimized = true, inPictureInPicture = false))
        assertFalse(miniPlayerHidden(AppSection.Account, minimized = true, inPictureInPicture = false))
    }

    @Test
    fun aFullPlayerOnSettingsStaysOnScreen() {
        assertFalse(miniPlayerHidden(AppSection.Settings, minimized = false, inPictureInPicture = false))
    }

    @Test
    fun pictureInPictureOnSettingsStaysOnScreen() {
        assertFalse(miniPlayerHidden(AppSection.Settings, minimized = true, inPictureInPicture = true))
    }

    @Test
    fun aFullPlayerOpenedFromTheOwnProfileMinimizesOntoIt() {
        assertEquals(
            AppSection.Account,
            sectionAfterPlayerMinimizes(
                section = AppSection.Account,
                watching = true,
                minimized = false,
            ),
        )
    }

    @Test
    fun aFullPlayerOpenedFromSettingsStaysOnHome() {
        assertEquals(
            AppSection.Home,
            sectionAfterPlayerMinimizes(
                section = AppSection.Settings,
                watching = true,
                minimized = false,
            ),
        )
    }

    @Test
    fun homeStaysHome() {
        assertEquals(
            AppSection.Home,
            sectionAfterPlayerMinimizes(
                section = AppSection.Home,
                watching = true,
                minimized = false,
            ),
        )
    }

    @Test
    fun aFullPlayerOpenedFromChannelProfileKeepsProfileBehindMiniPlayer() {
        assertEquals(
            AppSection.ChannelProfile,
            sectionAfterPlayerMinimizes(
                section = AppSection.ChannelProfile,
                watching = true,
                minimized = false,
            ),
        )
    }

    @Test
    fun anAlreadyMinimizedPlayerKeepsProfile() {
        assertEquals(
            AppSection.Account,
            sectionAfterPlayerMinimizes(
                section = AppSection.Account,
                watching = true,
                minimized = true,
            ),
        )
    }
}
