package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A file that is not there: the emote's image fails at once, without the network.
private val EMOTE = ChatPart.Emote(name = "Kappa", url = "file:///emotes/kappa.png")

private val MESSAGE = ChatMessage(
    id = "1",
    userLogin = "viewer",
    displayName = "Viewer",
    color = Color.Red,
    rawText = "hi Kappa",
    parts = listOf(ChatPart.Text("hi "), EMOTE),
    timestampMillis = 0L,
)

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a message's text does not need.
@Config(application = Application::class)
class ChatMessageTextTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** Writes down what a touch of the message's text did, in order. */
    private val events = mutableListOf<String>()

    private val actions = ChatMessageTextActions(
        openAuthor = { events += "author" },
        longClick = { events += "select" },
        openLink = { events += "link $it" },
        openEmote = { events += "emote ${it.name}" },
        openGif = { events += "gif ${it.name}" },
        closePreview = { events += "close" },
    )

    @Before
    fun showMessage() {
        compose.setContent {
            val look = ChatMessageLook(
                nickColor = MESSAGE.color,
                actionItalic = false,
                actionColor = false,
                sizes = chatTextSizes(textSize = 14.sp, timestampSize = 12.sp, emoteSize = 28.dp, badgeSize = 18.dp),
                badgeUrls = emptyMap(),
                chatterLabels = emptyMap(),
                nickColors = emptyMap(),
                readableColors = false,
                linksClickable = true,
                includeAuthor = true,
                timestampText = null,
            )
            val inlineContent = linkedMapOf<String, InlineTextContent>()
            val text = chatMessageText(MESSAGE, look, deletedNote = null, inlineContent, actions)
            Text(text = text, inlineContent = inlineContent)
        }
    }

    @Test
    fun `the emote is announced by its name`() {
        compose.onNodeWithContentDescription(EMOTE.name).assertExists()
    }

    @Test
    fun `a tap on the emote previews it`() {
        compose.onNodeWithContentDescription(EMOTE.name).performClick()

        assertEquals(listOf("emote Kappa"), events)
    }

    @Test
    fun `holding the emote selects the message`() {
        compose.onNodeWithContentDescription(EMOTE.name).performTouchInput { longClick() }

        assertEquals(listOf("select"), events)
    }

    @Test
    fun `a tap on the author's name opens their card`() {
        compose.onNodeWithText(MESSAGE.displayName, substring = true)
            .performTouchInput { click(centerLeft + Offset(4.dp.toPx(), 0f)) }

        assertEquals(listOf("author"), events)
    }
}
