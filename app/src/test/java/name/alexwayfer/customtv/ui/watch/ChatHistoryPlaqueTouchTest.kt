package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.DEFAULT_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.chat.EmoteSources
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PLAQUE = "plaque"

/** One of the user's own messages as the plaque gets it, with [text] as its only part. */
internal fun sentMessage(id: String, text: String) = ChatSentMessage(
    message = ChatMessage(
        id = id,
        userLogin = "me",
        displayName = "Me",
        color = Color.White,
        rawText = text,
        parts = listOf(ChatPart.Text(text)),
        timestampMillis = 0L,
    ),
    text = text,
)

/** How the chat draws its rows by default, with [sevenTv] as the channel's 7TV emotes. */
internal fun historyAppearance(sevenTv: Map<String, SevenTvEmote> = emptyMap()) = chatAppearance(
    readableColors = true,
    textSize = DEFAULT_CHAT_TEXT_SIZE,
    meMessageItalic = true,
    badgeUrls = emptyMap(),
    emotes = ThirdPartyEmotes(
        sources = EmoteSources(sevenTv = true, ffz = true, bttv = true),
        sevenTv = sevenTv,
        ffz = emptyMap(),
        bttv = emptyMap(),
    ),
)

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a touch of the plaque does not need.
@Config(application = Application::class)
class ChatHistoryPlaqueTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** Writes down what the plaque reported, in order. */
    private val events = mutableListOf<String>()

    private fun showPlaque(shown: ChatSentMessage = sentMessage("2", "second")) {
        compose.setContent {
            ChatHistoryPlaque(
                shown = shown,
                appearance = historyAppearance(),
                canOlder = true,
                canNewer = true,
                onStep = { older -> events += if (older) "older" else "newer" },
                onPick = { events += "pick ${it.id}" },
                modifier = Modifier.width(320.dp).testTag(PLAQUE),
            )
        }
    }

    private val plaque get() = compose.onNodeWithTag(PLAQUE, useUnmergedTree = true)

    @Test
    fun `a tap puts the message in the field`() {
        showPlaque()

        plaque.performTouchInput { click(center) }
        compose.waitForIdle()

        assertEquals(listOf("pick 2"), events)
    }

    @Test
    fun `a tap on an emote puts the message in the field instead of its preview`() {
        val emote = ChatPart.Emote(name = "KEKW", url = "file:///missing/kekw.png")
        val sent = sentMessage("2", "KEKW")
        showPlaque(sent.copy(message = sent.message.copy(parts = listOf(emote))))

        compose.onNodeWithContentDescription("KEKW", useUnmergedTree = true).performTouchInput { click(center) }
        compose.waitForIdle()

        assertEquals(listOf("pick 2"), events)
    }

    @Test
    fun `a swipe down pages to the older message without picking it`() {
        showPlaque()

        compose.dragInSteps(plaque, dx = 0.dp, dy = 4.dp, steps = 8)

        assertEquals(listOf("older"), events)
    }

    @Test
    fun `a swipe up pages to the newer message`() {
        showPlaque()

        compose.dragInSteps(plaque, dx = 0.dp, dy = (-4).dp, steps = 8)

        assertEquals(listOf("newer"), events)
    }

    @Test
    fun `a sideways swipe neither pages nor picks`() {
        showPlaque()

        compose.dragInSteps(plaque, dx = 8.dp, dy = 1.dp, steps = 8)

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `a move short of the touch slop is still a tap`() {
        showPlaque()

        compose.dragInSteps(plaque, dx = 0.dp, dy = 1.dp, steps = 2)

        assertEquals(listOf("pick 2"), events)
    }

    @Test
    fun `a cancelled swipe pages nothing`() {
        showPlaque()

        compose.dragInSteps(plaque, dx = 0.dp, dy = 4.dp, steps = 8, lift = false)
        plaque.performTouchInput { cancel() }
        compose.waitForIdle()

        assertEquals(emptyList<String>(), events)
    }
}
