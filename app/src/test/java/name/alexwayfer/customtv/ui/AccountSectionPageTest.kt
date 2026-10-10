package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSectionPageTest {
    @Test
    fun firstLoginShowsTheLoader() {
        assertEquals(AccountSectionPage.LoginLoader, accountSectionPage(loggingIn = true, signedIn = false))
    }

    @Test
    fun loginAgainOverASignedInAccountShowsTheLoaderInsteadOfTheProfile() {
        assertEquals(AccountSectionPage.LoginLoader, accountSectionPage(loggingIn = true, signedIn = true))
    }

    @Test
    fun signedInAccountWithoutALoginShowsTheProfile() {
        assertEquals(AccountSectionPage.Profile, accountSectionPage(loggingIn = false, signedIn = true))
    }

    @Test
    fun noAccountAndNoLoginShowsNothing() {
        assertEquals(AccountSectionPage.Empty, accountSectionPage(loggingIn = false, signedIn = false))
    }
}
