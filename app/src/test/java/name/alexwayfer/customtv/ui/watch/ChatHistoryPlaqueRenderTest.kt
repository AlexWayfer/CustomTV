package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The plain application: the app's own starts its stores and workers, which the plaque does not need.
@Config(application = Application::class)
class ChatHistoryPlaqueRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun groundAfterPage(from: ChatSentMessage, to: ChatSentMessage): String {
        var shown by mutableStateOf(from)
        compose.setContent {
            ChatHistoryPlaque(
                shown = shown,
                appearance = historyAppearance(),
                canOlder = true,
                canNewer = true,
                onStep = {},
                onPick = {},
                modifier = Modifier.width(320.dp),
            )
        }
        compose.waitForIdle()
        shown = to
        compose.waitForIdle()
        val pixels = compose.onNodeWithText(to.text).captureToImage().toPixelMap()
        // In the top padding, past the rounded corner, where no text is drawn.
        return pixels[pixels.width / 2, 3].toString()
    }

    @Test
    fun `the ground stays under a two-line message after a page`() {
        val ground = groundAfterPage(
            from = sentMessage("1", "short"),
            to = sentMessage(
                "2",
                "@anusral \"a message long enough to take a second line on the plaque\" and then some more words",
            ),
        )

        assertEquals(TwitchSurfaceAlt.toString(), ground)
    }

    @Test
    fun `the text's own color, which an ellipsis takes in an emote's place, is the message's color`() {
        compose.setContent {
            ChatHistoryPlaque(
                shown = sentMessage("1", "hi"),
                appearance = historyAppearance(),
                canOlder = true,
                canNewer = true,
                onStep = {},
                onPick = {},
                modifier = Modifier.width(320.dp),
            )
        }

        val text = compose.onNodeWithText("hi", useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        text.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals(TwitchText, layouts.single().layoutInput.style.color)
    }

    @Test
    fun `a 7TV emote in the message shows as the emote, as in the chat`() {
        compose.setContent {
            ChatHistoryPlaque(
                shown = sentMessage("1", "hi KEKW"),
                appearance = historyAppearance(sevenTv = mapOf("KEKW" to SevenTvEmote("file:///missing/kekw.webp"))),
                canOlder = true,
                canNewer = true,
                onStep = {},
                onPick = {},
                modifier = Modifier.width(320.dp),
            )
        }

        compose.onNodeWithContentDescription("KEKW", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the ground stays under a short message after a page`() {
        val ground = groundAfterPage(from = sentMessage("1", "short"), to = sentMessage("2", "another short one"))

        assertEquals(TwitchSurfaceAlt.toString(), ground)
    }
}
