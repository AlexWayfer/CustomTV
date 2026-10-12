package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsoTimeTest {
    private val evening = 1_760_212_800_000L

    @Test
    fun utcTimeReadsAsMillis() {
        assertEquals(evening,instantMillisOrNull("2025-10-11T20:00:00Z"))
    }

    @Test
    fun textThatIsNoTimeIsNull() {
        assertNull(instantMillisOrNull(""))
        assertNull(instantMillisOrNull("null"))
        assertNull(offsetTimeMillisOrNull("yesterday"))
    }

    @Test
    fun offsetTimeReadsAsMillis() {
        assertEquals(evening, offsetTimeMillisOrNull("2025-10-11T23:00:00+03:00"))
        assertEquals(evening, offsetTimeMillisOrNull("2025-10-11T20:00:00Z"))
    }
}
