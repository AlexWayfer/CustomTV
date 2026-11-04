package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MemorySampleTest {
    @Test
    fun `memory history keeps a fixed number of newest numeric samples`() {
        val history = MemorySampleHistory(2)
        history.add("first")
        history.add("second")
        history.add("third")
        assertEquals(listOf("second", "third"), history.snapshot())
    }

    @Test
    fun `memory line contains sizes and counts without message contents`() {
        val line = MemorySample(1L, 2L, 3L, 4L, 5, 6L).line()
        assertEquals("1 heap=2/3 native=4 images=6 replayRows=5", line)
        assertFalse(line.contains("token"))
    }
}
