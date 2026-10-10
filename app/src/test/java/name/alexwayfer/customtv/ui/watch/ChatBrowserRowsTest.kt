package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatBrowserRowsTest {
    @Test
    fun withoutTheExperimentalLoginOnlyTheSiteIsOffered() =
        assertEquals(
            ChatBrowserRows(openInBrowser = true, experimentalLogin = false),
            chatBrowserRows(ExperimentalLoginOffer.Unavailable, loggedIn = true),
        )

    @Test
    fun aLoggedInViewerIsOfferedTheExperimentalLoginNextToTheSite() =
        assertEquals(
            ChatBrowserRows(openInBrowser = true, experimentalLogin = true),
            chatBrowserRows(ExperimentalLoginOffer.CanLogIn, loggedIn = true),
        )

    @Test
    fun aLoggedOutViewerIsNotOfferedTheExperimentalLogin() =
        assertEquals(
            ChatBrowserRows(openInBrowser = true, experimentalLogin = false),
            chatBrowserRows(ExperimentalLoginOffer.CanLogIn, loggedIn = false),
        )

    @Test
    fun aLoginInProgressIsNotOfferedAgain() =
        assertEquals(
            ChatBrowserRows(openInBrowser = true, experimentalLogin = false),
            chatBrowserRows(ExperimentalLoginOffer.InProgress, loggedIn = true),
        )

    @Test
    fun theExperimentalLoginHidesTheSite() =
        assertEquals(
            ChatBrowserRows(openInBrowser = false, experimentalLogin = false),
            chatBrowserRows(ExperimentalLoginOffer.LoggedIn, loggedIn = true),
        )
}
