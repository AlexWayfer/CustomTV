package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTextSizeTest {
    @Test
    fun unsetSizeUsesTheDefault() {
        assertEquals(DEFAULT_CHAT_TEXT_SIZE, chatTextSizeFromStored(null))
    }

    @Test
    fun sizeInsideTheRangeIsKept() {
        assertEquals(17, chatTextSizeFromStored(17))
    }

    @Test
    fun sizeOutsideTheRangeIsClampedToTheNearestBound() {
        assertEquals(MIN_CHAT_TEXT_SIZE, chatTextSizeFromStored(4))
        assertEquals(MAX_CHAT_TEXT_SIZE, chatTextSizeFromStored(40))
    }
}
