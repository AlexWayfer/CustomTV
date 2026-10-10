package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSearchEntryTest {
    @Test
    fun loggedInShowsTheButtonEvenWithoutRecents() {
        assertEquals(
            HomeSearchEntry.Button,
            homeSearchEntry(signedIn = true, recentsRestored = false, hasRecents = false),
        )
    }

    @Test
    fun loggedOutWithRecentsShowsTheButton() {
        assertEquals(
            HomeSearchEntry.Button,
            homeSearchEntry(signedIn = false, recentsRestored = true, hasRecents = true),
        )
    }

    @Test
    fun loggedOutWithNoRecentsShowsTheField() {
        assertEquals(
            HomeSearchEntry.Field,
            homeSearchEntry(signedIn = false, recentsRestored = true, hasRecents = false),
        )
    }

    @Test
    fun loggedOutBeforeTheRecentsLoadShowsNothingYet() {
        assertEquals(
            HomeSearchEntry.None,
            homeSearchEntry(signedIn = false, recentsRestored = false, hasRecents = false),
        )
    }
}
