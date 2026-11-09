package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.chat.PersonalEmotes
import name.alexwayfer.customtv.chat.SevenTvCosmetics
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatListRetentionRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val messages = mutableStateOf<List<ChatMessage>>(emptyList())
    private val oldMessages = mutableListOf<WeakReference<ChatMessage>>()
    private val personal = mutableStateOf(PersonalEmotes())
    private val cosmetics = mutableStateOf(SevenTvCosmetics())
    private val nickColors = mutableStateOf<Map<String, Color>>(emptyMap())
    private val oldMetadata = mutableListOf<WeakReference<Any>>()

    @Test
    fun `a running chat releases messages from old visible windows`() {
        messages.value = batch(0)
        showChat()
        repeat(128) { index ->
            compose.runOnIdle {
                updateMetadata(index + 1)
                messages.value = batch(index + 1)
            }
            compose.waitForIdle()
        }
        // Keep the current window and reuse slots out of the check; the first window is long gone.
        repeat(3) { System.gc() }
        assertEquals(0, oldMessages.take(200).count { it.get() != null })
        assertEquals(0, oldMetadata.dropLast(6).count { it.get() != null })
    }

    private fun showChat() {
        val feed = ChatFeed(
            messages = { messages.value },
            chatterHistory = MutableStateFlow(ChatterHistory()),
            listState = LazyListState(),
            highlightPins = TemporaryHighlightPins(),
            stickToBottom = true,
            onStickToBottomChange = {},
            timelineJumpGeneration = 0,
            replyingToMessageId = null,
            onReply = null,
            fieldCardOpen = null,
        )
        val settings = settings()
        compose.setContent {
            val colors = remember { derivedStateOf { nickColors.value } }
            CompositionLocalProvider(LocalChatNickColors provides colors) {
                ProvideChatCosmetics(AppSettings(), { cosmetics.value }, { emptyMap() }, loadSources = false) {
                    ChatList(
                        feed = feed,
                        settings = settings,
                        layout = ChatListLayout(topInset = 0.dp),
                        listBottom = {},
                        modifier = Modifier.size(360.dp, 240.dp),
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun settings() = ChatListSettings(
        appearance = historyAppearance().copy(personalEmotes = { personal.value }),
        showTimestamps = false,
        smoothScroll = false,
        highlightColor = Color.Magenta,
        channelColor = Color.Magenta,
        highlightRewardCost = 0,
        channelPointsIconUrl = null,
        highlightFirstMessages = false,
        linkPreviewMode = LinkPreviewMode.None,
        mentions = ChatMentions(false, "", "", emptyList()),
        pinHighlights = false,
        highlightPinSeconds = 0,
    )

    private fun updateMetadata(generation: Int) {
        personal.value = PersonalEmotes(
            sevenTvSets = mapOf("set-$generation" to mapOf("emote" to SevenTvEmote("file:///missing/$generation"))),
            sevenTvUsers = mapOf("viewer" to listOf("set-$generation")),
        )
        cosmetics.value = SevenTvCosmetics(userPaints = mapOf("viewer" to "paint-$generation"))
        nickColors.value = mapOf("viewer" to Color(generation.toLong()))
        oldMetadata.add(WeakReference(personal.value))
        oldMetadata.add(WeakReference(cosmetics.value))
        oldMetadata.add(WeakReference(nickColors.value))
    }

    private fun batch(generation: Int): List<ChatMessage> = List(200) { index ->
        val id = "message-$generation-$index"
        ChatMessage(
            id = id,
            userLogin = "viewer",
            userId = "viewer",
            displayName = "Viewer",
            color = Color.White,
            rawText = id,
            parts = listOf(ChatPart.Text(id)),
            timestampMillis = 0L,
        ).also { oldMessages.add(WeakReference(it)) }
    }
}
