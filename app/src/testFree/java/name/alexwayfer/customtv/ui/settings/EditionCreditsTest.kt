package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class EditionCreditsTest {
    @Test
    fun theAppLinksToItsPublicSourceUnderTheGpl() {
        val app = appCredit!!

        assertEquals("https://github.com/AlexWayfer/CustomTV", app.url)
        assertEquals(License.Gpl3, app.license)
    }
}
