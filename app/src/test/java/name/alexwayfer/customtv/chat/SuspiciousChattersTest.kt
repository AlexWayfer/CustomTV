package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SuspiciousChattersTest {
    @Test
    fun aStatusMarksTheChatter() {
        assertEquals(
            mapOf("1" to LowTrustStatus.Restricted),
            suspiciousChattersAfter(emptyMap(), "1", LowTrustStatus.Restricted),
        )
    }

    @Test
    fun noStatusRemovesTheMark() {
        assertEquals(emptyMap<String, LowTrustStatus>(), suspiciousChattersAfter(mapOf("1" to LowTrustStatus.Monitored), "1", null))
    }

    @Test
    fun theSameStatusOrABlankUserKeepsTheMap() {
        val current = mapOf("1" to LowTrustStatus.Monitored)
        assertSame(current, suspiciousChattersAfter(current, "1", LowTrustStatus.Monitored))
        assertSame(current, suspiciousChattersAfter(current, " ", LowTrustStatus.Restricted))
        assertSame(current, suspiciousChattersAfter(current, "2", null))
    }
}
