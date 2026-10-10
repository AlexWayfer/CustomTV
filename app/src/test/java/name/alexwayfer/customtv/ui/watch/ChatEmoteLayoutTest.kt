package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEmoteLayoutTest {
    private val left = ChatPart.Emote(name = "left", url = "left")
    private val right = ChatPart.Emote(name = "right", url = "right")

    @Test
    fun recognizesWhitespaceBetweenTwoEmotes() {
        val parts = listOf(left, ChatPart.Text(" \t"), right)

        assertTrue(isWhitespaceBetweenEmotes(parts, 1))
    }

    @Test
    fun rejectsVisibleTextBetweenEmotes() {
        val parts = listOf(left, ChatPart.Text(" + "), right)

        assertFalse(isWhitespaceBetweenEmotes(parts, 1))
    }

    @Test
    fun rejectsWhitespaceWithoutEmotesOnBothSides() {
        assertFalse(isWhitespaceBetweenEmotes(listOf(ChatPart.Text(" "), right), 0))
        assertFalse(isWhitespaceBetweenEmotes(listOf(left, ChatPart.Text(" ")), 1))
    }

    @Test
    fun wideBaseDoesNotMultiplyOverlayWidth() {
        val width = stackedEmoteSlotAspect(
            baseAspect = 1f,
            baseWidthMultiplier = 2f,
            overlays = listOf(2f to 1f),
        )

        assertEquals(2f, width, 0.001f)
    }

    @Test
    fun wideOverlayStillGetsItsOwnSpace() {
        val width = stackedEmoteSlotAspect(
            baseAspect = 1f,
            baseWidthMultiplier = 1f,
            overlays = listOf(1.5f to 2f),
        )

        assertEquals(3f, width, 0.001f)
    }
}
