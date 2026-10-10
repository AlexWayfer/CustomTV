package name.alexwayfer.customtv.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGenerationTest {
    @Test
    fun restoreWithoutLogoutWritesSession() {
        assertTrue(sessionWriteAllowed(startedGeneration = 0, currentGeneration = 0))
    }

    @Test
    fun restoreStartedBeforeLogoutDoesNotWriteSession() {
        assertFalse(sessionWriteAllowed(startedGeneration = 0, currentGeneration = 1))
    }

    @Test
    fun loginAfterLogoutWritesTheNewSession() {
        assertTrue(sessionWriteAllowed(startedGeneration = 1, currentGeneration = 1))
    }
}
