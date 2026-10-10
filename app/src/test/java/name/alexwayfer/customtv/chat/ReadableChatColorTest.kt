package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableChatColorTest {
    private val chatBackground = Color(0xFF18181B)

    @Test
    fun blackNickBecomesReadableOnDarkChat() {
        val adjusted = ReadableChatColor.adjust(Color.Black, chatBackground)
        assertTrue(ReadableChatColor.contrastRatio(adjusted, chatBackground) >= ReadableChatColor.MIN_CONTRAST)
        assertTrue(adjusted.red + adjusted.green + adjusted.blue > 1.2f)
    }

    @Test
    fun alreadyBrightColorStaysPut() {
        val orange = Color(0xFFFF4500)
        val adjusted = ReadableChatColor.adjust(orange, chatBackground)
        assertTrue(ReadableChatColor.contrastRatio(adjusted, chatBackground) >= ReadableChatColor.MIN_CONTRAST)
    }
}
