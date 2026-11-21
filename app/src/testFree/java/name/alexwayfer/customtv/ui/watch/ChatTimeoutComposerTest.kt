package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emptyFlow
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.ChatRoomModes
import name.alexwayfer.customtv.chat.ChatTimeoutCheck
import name.alexwayfer.customtv.chat.ChatTimeouts
import name.alexwayfer.customtv.chat.FakeIrcSocket
import name.alexwayfer.customtv.chat.RecentChatLoadResult
import name.alexwayfer.customtv.chat.TwitchIrcSender
import name.alexwayfer.customtv.ui.ComposeHost
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class)
class ChatTimeoutComposerTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val now = AtomicLong(1_000)
    private val timeouts = ChatTimeouts(now::get).apply {
        setUser(VIEWER_ID)
        onHistory(CHANNEL, RecentChatLoadResult())
    }
    private val draft = TextFieldState(DRAFT)
    private val sockets = mutableListOf<FakeIrcSocket>()
    private val sender = TwitchIrcSender(
        CHANNEL, "viewer",
        WebSocket.Factory { request, listener -> FakeIrcSocket(request, listener).also(sockets::add) },
        nowMillis = now::get,
    )

    @Test
    fun ircRefusalStartsTheCountdownAndLocksTheDraftWithNoHistoryEvent() {
        showComposer()
        field().assertIsEnabled()
        assertNull(timeouts.state.value.deadline(CHANNEL, VIEWER_ID))
        val app = RuntimeEnvironment.getApplication()
        val send = compose.onNodeWithContentDescription(app.getString(R.string.chat_send))
        val menu = compose.onNodeWithContentDescription(app.getString(R.string.chat_menu))
        val refusal = compose.onNodeWithText(app.getString(R.string.chat_timeout_send_refused))
        send.performClick()

        compose.runOnIdle {
            val socket = sockets.single()
            socket.register()
            assertEquals("PRIVMSG #$CHANNEL :$DRAFT", socket.sent.last())
            socket.receive(
                "@msg-id=msg_timedout :tmi.twitch.tv NOTICE #$CHANNEL " +
                    ":You are banned from talking in $CHANNEL for 34840 more seconds.",
            )
        }

        field().assertDoesNotExist()
        countdown("9:40:40").assertIsDisplayed()
        refusal.assertIsDisplayed()
        send.assertDoesNotExist()
        menu.assertIsEnabled()
        compose.runOnIdle { assertEquals(DRAFT, draft.text.toString()) }

        val deadline = requireNotNull(timeouts.state.value.deadline(CHANNEL, VIEWER_ID))
        now.addAndGet(1_000)
        compose.mainClock.advanceTimeBy(1_000)
        countdown("9:40:39").assertIsDisplayed()
        field().assertDoesNotExist()

        now.set(deadline)
        compose.mainClock.advanceTimeBy(1_000)
        field().assertIsEnabled()
        send.assertIsEnabled()
        countdown("9:40:39").assertDoesNotExist()
        refusal.assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(DRAFT, draft.text.toString())
            val checkedAt = now.get()
            val check = ChatTimeoutCheck.Until(checkedAt + 60_000)
            timeouts.applyCheck(CHANNEL, VIEWER_ID, timeouts.state.value, check, checkedAt)
        }
        countdown("1:00").assertIsDisplayed()
        refusal.assertDoesNotExist()
    }

    private fun field() = compose.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(DRAFT)),
        useUnmergedTree = true,
    )

    private fun countdown(time: String) = compose.onNodeWithContentDescription(
        RuntimeEnvironment.getApplication().getString(R.string.chat_send_timeout_countdown, time),
    )

    private fun draftOptions() = ChatCompose(
        draft = draft,
        emotes = ChatInputEmotes(false, false, false, emptyMap(), emptyMap(), emptyMap(), false),
        replyParentMessageId = null,
        replyFocusRequest = null,
        mentionRequest = null,
        sessionChatters = { emptyMap() },
        chatterLastSpokeAt = { emptyMap() },
        shareOffer = null,
        keepKeyboardAfterSend = true,
        historyEnabled = false,
        sentHistory = emptyList(),
        meMessageItalic = false,
        slowSendBlocked = false,
        onSent = { error("The refused message must not be marked as sent") },
        onOpenChatterCard = {},
        onOpenPrediction = null,
    )

    private fun inactiveMenu() = ChatMenu(
        settings = null,
        onReadableColorsChange = {},
        onTimestampsChange = {},
        onTextSizeChange = {},
        onSmoothChatScrollChange = {},
        onRefreshEmotes = { Job() },
        onRefreshLabels = { Job() },
        onOpenOwnProfile = null,
        recap = null,
        streamHide = StreamHideToggle(hidden = false, onChange = {}),
    )

    private fun showComposer() {
        val typed = draftOptions()
        val menu = inactiveMenu()
        compose.setContent {
            ChatInputBar(
                channel = ChatInputChannel(
                    login = CHANNEL,
                    broadcasterId = null,
                    owner = ChatNick(CHANNEL, CHANNEL),
                    rules = null,
                    liveMessages = emptyFlow(),
                    browserSheetHeightPx = { 0 },
                    roomModes = ChatRoomModes(),
                    modeControl = null,
                    timeouts = timeouts,
                ),
                user = ChatInputUser(
                    canSend = true,
                    userId = VIEWER_ID,
                    login = "viewer",
                    displayName = "Viewer",
                    accessToken = { null },
                    onLogIn = {},
                ),
                compose = typed,
                menu = menu,
                bar = ChatInputBarLook(
                    fullscreen = false,
                    overVideo = false,
                    plaques = emptyList(),
                    onFollow = {},
                    onSubscribe = {},
                    channelPoints = {},
                    onComposerOpenChange = {},
                    onPickerPanelPx = {},
                ),
                sendMessage = { message, reply -> sender.send("test-token", message, reply) },
            )
        }
    }

    private companion object {
        const val CHANNEL = "streamassistantbot"
        const val VIEWER_ID = "456"
        const val DRAFT = "CustomTV IRC timeout test"
    }
}
