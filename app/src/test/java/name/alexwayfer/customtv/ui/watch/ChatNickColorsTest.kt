package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatNickColorsTest {
    private val known = mapOf(
        "alexwayfer" to Color.Red,
        "somebody" to Color.Blue,
        "quiet" to Color.Green,
    )

    @Test
    fun aRowKeepsOnlyTheColorsOfMentionedAndBareNicks() {
        val message = message(ChatPart.Text("hi @AlexWayfer and somebody"))
        assertEquals(
            mapOf("alexwayfer" to Color.Red, "somebody" to Color.Blue),
            chatNickColorsFor(message, known),
        )
    }

    @Test
    fun aRowWithoutNicksDependsOnNoColors() {
        assertEquals(emptyMap<String, Color>(), chatNickColorsFor(message(ChatPart.Text("just chatting")), known))
    }

    @Test
    fun aNewChatterNobodyMentionsLeavesTheRowColorsEqual() {
        val message = message(ChatPart.Text("hi @AlexWayfer"))
        assertEquals(
            chatNickColorsFor(message, known),
            chatNickColorsFor(message, known + ("newcomer" to Color.Yellow)),
        )
    }

    @Test
    fun nicksInEveryTextPartAreFound() {
        val message = message(
            ChatPart.Text("@somebody "),
            ChatPart.Emote(name = "Kappa", url = "https://example.com/kappa"),
            ChatPart.Text(" quiet"),
        )
        assertEquals(mapOf("somebody" to Color.Blue, "quiet" to Color.Green), chatNickColorsFor(message, known))
    }

    private fun message(vararg parts: ChatPart) = ChatMessage(
        id = "id",
        userLogin = "author",
        displayName = "Author",
        color = Color.White,
        rawText = "",
        parts = parts.toList(),
        timestampMillis = 0L,
    )
}
