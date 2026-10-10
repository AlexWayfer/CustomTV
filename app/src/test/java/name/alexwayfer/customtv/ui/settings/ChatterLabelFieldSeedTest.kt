package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterLabelFieldSeedTest {
    @Test
    fun savedCredentialsFillBothFieldsOnTheFirstFrame() {
        val seed = chatterLabelFieldSeed(token = "ghp_saved", gistId = "a".repeat(32))

        assertEquals("ghp_saved", seed.token)
        assertEquals("a".repeat(32), seed.gistId)
    }

    @Test
    fun missingCredentialsLeaveBothFieldsEmpty() {
        val seed = chatterLabelFieldSeed(token = null, gistId = null)

        assertEquals("", seed.token)
        assertEquals("", seed.gistId)
    }
}
