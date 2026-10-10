package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatParticipantColorsTest {
    @Test
    fun remembersNormalizedLoginAndDisplayName() {
        val color = Color(0xff123456)

        val result = rememberParticipantColor(
            colors = emptyMap(),
            message = message(" Viewer ", "Display Name", color),
            maxColors = 10,
        )

        assertTrue(color == result["viewer"])
        assertTrue(color == result["display name"])
    }

    @Test
    fun refreshesExistingNamesBeforeDiscardingOldestColor() {
        val original = linkedMapOf(
            "oldest" to Color.Red,
            "viewer" to Color.Blue,
            "newer" to Color.Green,
        )

        val result = rememberParticipantColor(
            colors = original,
            message = message("viewer", "Viewer", Color.Yellow),
            maxColors = 2,
        )

        assertEquals(listOf("newer", "viewer"), result.keys.toList())
        assertTrue(Color.Yellow == result["viewer"])
    }

    @Test
    fun sameParticipantAndColorKeepsThePreviousMap() {
        val colors = mapOf(
            "viewer" to Color.Red,
            "display" to Color.Red,
        )

        assertSame(
            colors,
            rememberParticipantColor(colors, message("viewer", "Display", Color.Red), 10),
        )
    }

    @Test
    fun ignoresNoticesAndEmoteChanges() {
        val colors = mapOf("viewer" to Color.Red)
        val notice = message("viewer", "Viewer", Color.Blue).copy(notice = ChatNotice.Welcome)

        assertSame(colors, rememberParticipantColor(colors, notice, 10))
    }

    @Test
    fun resolvesActorBeforeLoginAndIgnoresUnspecifiedColor() {
        val colors = mapOf(
            "actor" to Color.Unspecified,
            "login" to Color.Green,
        )

        assertTrue(Color.Green == knownParticipantColor(colors, "ACTOR", "LOGIN"))
        assertNull(knownParticipantColor(colors, "missing", ""))
    }

    private fun message(login: String, displayName: String, color: Color) = ChatMessage(
        id = "message",
        userLogin = login,
        displayName = displayName,
        color = color,
        rawText = "text",
        parts = listOf(ChatPart.Text("text")),
        timestampMillis = 1L,
    )
}
