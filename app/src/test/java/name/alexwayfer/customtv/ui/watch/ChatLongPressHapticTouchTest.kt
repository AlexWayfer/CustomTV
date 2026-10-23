package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatThreadEntry
import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ROW = "row"

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

private val APPEARANCE = ChatAppearance(
    readableColors = false,
    textSize = 14,
    meMessageItalic = false,
    badgeUrls = emptyMap(),
    thirdPartyEmotes = emptyMap(),
    modifierPlatforms = emptySet(),
    platformLogos = emptyMap(),
)

private val SETTINGS = ChatListSettings(
    appearance = APPEARANCE,
    showTimestamps = false,
    smoothScroll = false,
    highlightColor = Color.Yellow,
    channelColor = Color.Magenta,
    highlightRewardCost = 0,
    channelPointsIconUrl = null,
    highlightFirstMessages = false,
    linkPreviewMode = LinkPreviewMode.None,
    mentions = ChatMentions(highlightMentions = false, selfLogin = "", selfDisplayName = "", emptyList()),
    pinHighlights = false,
    highlightPinSeconds = 0,
)

private val THREAD = ChatReplySession(
    starterId = "s",
    targetId = "s",
    entries = listOf(
        ChatThreadEntry(id = "s", login = "starter", displayName = "Starter", body = "good game", timestampMillis = 0L),
    ),
)

/** Each long press in the chat vibrates once, as the touch target gives it. */
@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a chat row does not need.
@Config(application = Application::class)
class ChatLongPressHapticTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var vibrations = 0
    private var selected = 0
    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            vibrations++
        }
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content) }
    }

    @Test
    fun `holding a message in the chat vibrates once`() {
        show { Row() }

        // Past the text's end on the first line, on the row itself.
        compose.onNodeWithTag(ROW).performTouchInput { longClick(Offset(width - 8.dp.toPx(), 8.dp.toPx())) }

        assertEquals(1, selected)
        assertEquals(1, vibrations)
    }

    @Test
    fun `holding an emote vibrates once`() {
        show { Row() }

        compose.onNodeWithContentDescription(EMOTE.name).performTouchInput { longClick() }

        assertEquals(1, selected)
        assertEquals(1, vibrations)
    }

    @Test
    fun `holding an emote in a message's text vibrates once`() {
        show { MessageText() }

        compose.onNodeWithContentDescription(EMOTE.name).performTouchInput { longClick() }

        assertEquals(1, selected)
        assertEquals(1, vibrations)
    }

    @Test
    fun `holding a message in a thread vibrates once`() {
        show {
            ChatThreadPanel(session = THREAD, onChoose = {}, onCancel = {}, onClose = {}, appearance = APPEARANCE)
        }

        compose.onNodeWithText("good game", substring = true).performTouchInput { longClick() }

        assertEquals(1, vibrations)
    }

    @Composable
    private fun Row() {
        val list = rememberLazyListState()
        LazyColumn(state = list, modifier = Modifier.size(300.dp, 200.dp).testTag(ROW)) {
            item {
                ChatRow(
                    message = MESSAGE,
                    settings = SETTINGS,
                    place = ChatRowPlace(list, bottomOverlayPx = 0),
                    selected = false,
                    actions = ChatRowActions(
                        onSelect = { selected++ },
                        onDismissSelection = {},
                        onReply = null,
                        onReplySwipingChange = {},
                        onReplySwipeEnd = {},
                        // The copy swipe waits for the finger to move, so it leaves a long press alone.
                        copySwipe = true,
                    ),
                )
            }
        }
    }

    @Composable
    private fun MessageText() {
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
        val actions = ChatMessageTextActions(
            openAuthor = {},
            longClick = { selected++ },
            openLink = {},
            openEmote = {},
            openGif = {},
            closePreview = {},
        )
        val inlineContent = linkedMapOf<String, InlineTextContent>()
        val text = chatMessageText(MESSAGE, look, deletedNote = null, inlineContent, actions)
        Text(text = text, inlineContent = inlineContent, modifier = Modifier.width(300.dp))
    }
}
