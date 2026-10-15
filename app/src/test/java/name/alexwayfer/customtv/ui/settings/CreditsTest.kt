package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditsTest {
    @Test
    fun everySharedLibraryIsUnderTheApacheLicenseWhoseTextThePageCarries() {
        assertTrue(sharedCredits.all { it.license == License.Apache2 })
    }

    @Test
    fun theAppComesFirstAndTheEditionCreditsFollowTheSharedOnes() {
        assertEquals(listOfNotNull(appCredit) + sharedCredits + editionCredits, softwareCredits)
    }

    @Test
    fun everyLinkIsHttps() {
        val links = softwareCredits.map { it.url }

        assertTrue(links.toString(), links.all { it.startsWith("https://") })
    }
}
