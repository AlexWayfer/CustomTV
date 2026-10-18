package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTouchEndTest {
    @Test
    fun aTapOnTheChatHidesTheKeyboard() {
        assertEquals(
            ChatTouchEnd.Released,
            chatTouchEndBeforeLongPress(released = true, distancePx = 0f, touchSlopPx = 8f),
        )
        assertTrue(chatTouchClearsFocus(ChatTouchEnd.Released))
    }

    @Test
    fun aScrollOrSwipePastTheSlopKeepsTheKeyboard() {
        assertEquals(
            ChatTouchEnd.Moved,
            chatTouchEndBeforeLongPress(released = false, distancePx = 9f, touchSlopPx = 8f),
        )
        assertFalse(chatTouchClearsFocus(ChatTouchEnd.Moved))
    }

    @Test
    fun aFingerStillWithinTheSlopWaitsForTheLongPress() {
        assertNull(chatTouchEndBeforeLongPress(released = false, distancePx = 8f, touchSlopPx = 8f))
    }

    @Test
    fun aLongPressKeepsTheKeyboard() {
        assertFalse(chatTouchClearsFocus(ChatTouchEnd.Held))
    }
}
