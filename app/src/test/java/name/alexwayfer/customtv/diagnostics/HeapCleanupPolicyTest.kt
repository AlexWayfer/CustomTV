package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeapCleanupPolicyTest {
    @Test
    fun `a dump created in the current run is left until restart`() {
        assertFalse(heapCleanupDue(null, 20L, hasWork = true))
        assertFalse(heapCleanupDue(10L, 20L, hasWork = true))
    }

    @Test
    fun `only the same pending dump from startup is prepared`() {
        assertTrue(heapCleanupDue(10L, 10L, hasWork = true))
        assertFalse(heapCleanupDue(10L, 10L, hasWork = false))
        assertFalse(heapCleanupDue(10L, null, hasWork = true))
        assertFalse(heapCleanupDue(null, null, hasWork = false))
    }
}
