package name.alexwayfer.customtv.links

import org.junit.Assert.assertEquals
import org.junit.Test

class OutsideLinkRouteTest {
    private val own = "name.alexwayfer.customtv"
    private val channel = TwitchLinkTarget.Channel("lirik")

    @Test
    fun aChannelLinkAndroidHandsToTheAppOpensInTheApp() {
        assertEquals(OutsideLinkRoute.App, outsideLinkRoute(own, own, channel))
        assertEquals(OutsideLinkRoute.App, outsideLinkRoute(own, own, TwitchLinkTarget.Video("1")))
    }

    @Test
    fun aPageTheAppDoesNotShowGoesToTheBrowserInsteadOfBackToTheApp() {
        assertEquals(OutsideLinkRoute.Browser, outsideLinkRoute(own, own, target = null))
    }

    @Test
    fun withoutTheLinksAddedEveryLinkGoesWhereItAlwaysWent() {
        assertEquals(OutsideLinkRoute.System, outsideLinkRoute("com.android.chrome", own, channel))
        assertEquals(OutsideLinkRoute.System, outsideLinkRoute("tv.twitch.android.app", own, channel))
        assertEquals(OutsideLinkRoute.System, outsideLinkRoute(handlerPackage = null, own, channel))
        assertEquals(OutsideLinkRoute.System, outsideLinkRoute("com.android.chrome", own, target = null))
    }
}
