package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMinimizeSectionTest {
    @Test
    fun backOnHomeWithAMiniStreamKeepsItInPip() {
        assertTrue(keepsMiniPlayer(AppSection.Home, liveMiniPlayer = true))
    }

    @Test
    fun backOnHomeWithAMiniRecordingKeepsItInPip() {
        assertTrue(keepsMiniPlayer(AppSection.Home, videoMiniPlayer = true))
    }

    @Test
    fun backOnHomeWithoutAMiniPlayerLeavesTheAppAsUsual() {
        assertFalse(keepsMiniPlayer(AppSection.Home))
    }

    @Test
    fun backOnAnotherSectionIsLeftToThatSection() {
        assertFalse(keepsMiniPlayer(AppSection.Account, liveMiniPlayer = true, videoMiniPlayer = true))
    }

    @Test
    fun pipIsOfferedForALiveStreamAndForAnOpenRecording() {
        assertTrue(pipAllowed(watching = true, channelLive = true))
        assertTrue(pipAllowed(recordingOpen = true))
    }

    @Test
    fun pipIsNotOfferedForAnOfflineOrDismissedStream() {
        assertFalse(pipAllowed(watching = true))
        assertFalse(pipAllowed(watching = true, channelLive = true, liveClosedWithPip = true))
    }

    /** [homeBackKeepsMiniPlayer] outside picture-in-picture. */
    private fun keepsMiniPlayer(
        section: AppSection,
        liveMiniPlayer: Boolean = false,
        videoMiniPlayer: Boolean = false,
    ) = homeBackKeepsMiniPlayer(
        section,
        liveMiniPlayer = liveMiniPlayer,
        videoMiniPlayer = videoMiniPlayer,
        inPictureInPicture = false,
    )

    private fun pipAllowed(
        watching: Boolean = false,
        channelLive: Boolean = false,
        liveClosedWithPip: Boolean = false,
        recordingOpen: Boolean = false,
    ) = pictureInPictureAllowed(
        watching = watching,
        channelLive = channelLive,
        liveClosedWithPip = liveClosedWithPip,
        recordingOpen = recordingOpen,
    )

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
