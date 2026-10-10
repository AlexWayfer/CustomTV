package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class SmoothChatScrollTest {
    @Test
    fun missingBooleanDefaultsToDisabled() {
        assertEquals(false, smoothChatScrollFromStored(null))
        assertEquals(false, smoothChatScrollFromStored(false))
        assertEquals(true, smoothChatScrollFromStored(true))
    }
}
