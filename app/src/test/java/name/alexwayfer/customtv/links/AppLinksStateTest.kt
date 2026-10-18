package name.alexwayfer.customtv.links

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLinksStateTest {
    private val claimed = setOf("twitch.tv", "www.twitch.tv")

    @Test
    fun linksAreOnOnceEveryClaimedHostIsAddedAndSupportedLinksAllowed() {
        assertTrue(appLinksApproved(linkHandlingAllowed = true, setOf("twitch.tv", "www.twitch.tv"), claimed))
    }

    @Test
    fun linksAreOffWhileAHostIsMissing() {
        assertFalse(appLinksApproved(linkHandlingAllowed = true, setOf("twitch.tv"), claimed))
        assertFalse(appLinksApproved(linkHandlingAllowed = true, setOf("www.twitch.tv"), claimed))
        assertFalse(appLinksApproved(linkHandlingAllowed = true, emptySet(), claimed))
    }

    @Test
    fun linksAreOffWhileTheClipsHostIsMissingWhereItIsClaimed() {
        val withClips = claimed + CLIPS_LINK_HOST
        assertFalse(appLinksApproved(linkHandlingAllowed = true, claimed, withClips))
        assertTrue(appLinksApproved(linkHandlingAllowed = true, withClips, withClips))
    }

    @Test
    fun linksAreOffWhileSupportedLinksAreTurnedOff() {
        assertFalse(appLinksApproved(linkHandlingAllowed = false, setOf("twitch.tv", "www.twitch.tv"), claimed))
    }

    @Test
    fun onlyAndroid12AndLaterNeedTheLinksSetUp() {
        assertTrue(appLinksNeedSetup(31))
        assertTrue(appLinksNeedSetup(36))
        assertFalse(appLinksNeedSetup(30))
        assertFalse(appLinksNeedSetup(28))
    }
}
