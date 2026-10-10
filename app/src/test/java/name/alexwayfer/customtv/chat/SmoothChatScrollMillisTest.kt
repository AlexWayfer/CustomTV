package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmoothChatScrollMillisTest {
    @Test
    fun enabledUses300MillisAndDisabledDoesNotAnimate() {
        assertEquals(300, smoothChatScrollMillis(true))
        assertNull(smoothChatScrollMillis(false))
    }
}
