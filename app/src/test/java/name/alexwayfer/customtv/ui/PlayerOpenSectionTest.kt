package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOpenSectionTest {
    @Test
    fun backFromAProfileOpenedFromHomeReturnsHomeEvenOverAMinimizedStream() {
        assertEquals(
            ProfileBack.Show(AppSection.Home),
            profileBack(AppSection.Home, watching = true, minimized = true),
        )
    }

    @Test
    fun backFromAProfileOpenedFromTheStreamExpandsTheStream() {
        assertEquals(ProfileBack.ExpandPlayer, profileBack(null, watching = true, minimized = true))
    }

    @Test
    fun aStreamExpandedByBackFromTheProfileClosesTheProfileAndMinimizesOntoHome() {
        val behind = sectionAfterPlayerExpands(AppSection.ChannelProfile, profileClosing = true)
        assertEquals(AppSection.Home, behind)
        assertEquals(AppSection.Home, sectionAfterPlayerMinimizes(behind, watching = true, minimized = false))
    }

    @Test
    fun aStreamExpandedFromTheMiniPlayerKeepsTheProfileUnderIt() {
        assertEquals(
            AppSection.ChannelProfile,
            sectionAfterPlayerExpands(AppSection.ChannelProfile, profileClosing = false),
        )
    }

    @Test
    fun backFromAProfileWithoutAStreamGoesHome() {
        assertEquals(ProfileBack.Show(AppSection.Home), profileBack(null, watching = false, minimized = false))
    }

    @Test
    fun aFullStreamOverTheProfileTakesBackFromTheProfile() {
        assertFalse(profileHandlesBack(watching = true, liveMinimized = false))
    }

    @Test
    fun theProfileHandlesBackUnderAMiniPlayerOrWithoutAStream() {
        assertTrue(profileHandlesBack(watching = true, liveMinimized = true))
        assertTrue(profileHandlesBack(watching = false, liveMinimized = false))
    }

    @Test
    fun anOpeningPlayerHidesSettingsImmediately() {
        assertEquals(
            AppSection.Settings,
            sectionBehindOpenPlayer(section = AppSection.Settings, playerCovering = false),
        )
        assertFalse(
            showAppShell(
                inPictureInPicture = false,
                watching = true,
                minimized = false,
                section = AppSection.Settings,
            ),
        )
    }

    @Test
    fun aCoveringPlayerKeepsTheOwnProfileBehindIt() {
        assertEquals(
            AppSection.Account,
            sectionBehindOpenPlayer(section = AppSection.Account, playerCovering = true),
        )
        assertTrue(
            showAppShell(
                inPictureInPicture = false,
                watching = true,
                minimized = false,
                section = AppSection.Account,
            ),
        )
    }

    @Test
    fun aCoveringPlayerPutsHomeBehindIt() {
        assertEquals(
            AppSection.Home,
            sectionBehindOpenPlayer(section = AppSection.Settings, playerCovering = true),
        )
    }

    @Test
    fun aCoveringPlayerKeepsChannelProfileBehindIt() {
        assertEquals(
            AppSection.ChannelProfile,
            sectionBehindOpenPlayer(section = AppSection.ChannelProfile, playerCovering = true),
        )
    }

    @Test
    fun homeStaysHomeBeforeThePlayerCovers() {
        assertEquals(
            AppSection.Home,
            sectionBehindOpenPlayer(section = AppSection.Home, playerCovering = false),
        )
    }

    @Test
    fun aMinimizedPlayerShowsTheShell() {
        assertTrue(
            showAppShell(
                inPictureInPicture = false,
                watching = true,
                minimized = true,
                section = AppSection.Account,
            ),
        )
    }

    @Test
    fun pictureInPictureHidesTheShell() {
        assertFalse(
            showAppShell(
                inPictureInPicture = true,
                watching = true,
                minimized = false,
                section = AppSection.Home,
            ),
        )
    }

    @Test
    fun channelProfileIsVisibleWhileThePlayerCollapsesOverIt() {
        assertTrue(
            showAppShell(
                inPictureInPicture = false,
                watching = true,
                minimized = false,
                section = AppSection.ChannelProfile,
            ),
        )
    }

    @Test
    fun backFromProfileExpandsAnActiveMiniPlayer() {
        assertTrue(profileBackExpandsPlayer(watching = true, minimized = true))
    }

    @Test
    fun backFromProfileLeavesWhenThereIsNoMiniPlayer() {
        assertFalse(profileBackExpandsPlayer(watching = false, minimized = false))
        assertFalse(profileBackExpandsPlayer(watching = true, minimized = false))
    }

    @Test
    fun openingAVideoKeepsTheChannelProfileAndClosesTheLivePlayer() {
        assertEquals(
            ProfileVideoOpenDecision(
                section = AppSection.ChannelProfile,
                minimized = true,
                closeLivePlayer = true,
            ),
            profileVideoOpenDecision(
                section = AppSection.ChannelProfile,
                minimized = true,
                watching = true,
            ),
        )
    }

    @Test
    fun openingAVideoFromTheAccountKeepsTheAccountWithoutClosingAPlayer() {
        assertEquals(
            ProfileVideoOpenDecision(
                section = AppSection.Account,
                minimized = false,
                closeLivePlayer = false,
            ),
            profileVideoOpenDecision(
                section = AppSection.Account,
                minimized = false,
                watching = false,
            ),
        )
    }
}
