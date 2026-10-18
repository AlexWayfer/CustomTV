package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatThreadEntry
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private val SESSION = ChatReplySession(
    starterId = "s",
    targetId = "s",
    entries = listOf(
        ChatThreadEntry(id = "s", login = "starter", displayName = "Starter", body = "hi", timestampMillis = 0L),
    ),
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

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a thread panel does not need.
@Config(application = Application::class)
class ChatThreadPanelTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private var closed = 0

    @Before
    fun showPanel() {
        compose.setContent {
            ChatThreadPanel(
                session = SESSION,
                onChoose = {},
                onCancel = {},
                onClose = { closed++ },
                appearance = APPEARANCE,
            )
        }
    }

    private val title get() = compose.onNodeWithText(app.getString(R.string.chat_thread_with, "Starter"))

    @Test
    fun `dragging the title down past the slop closes the panel`() {
        compose.dragInSteps(title, 0.dp, 10.dp, steps = 12)

        assertEquals(1, closed)
    }

    @Test
    fun `a short drag springs the panel back`() {
        compose.dragInSteps(title, 0.dp, 10.dp, steps = 4)

        assertEquals(0, closed)
    }

    @Test
    fun `a drag up leaves the panel in place`() {
        compose.dragInSteps(title, 0.dp, (-10).dp, steps = 12)

        assertEquals(0, closed)
    }

    @Test
    fun `the close button closes the panel`() {
        compose.onNodeWithContentDescription(app.getString(R.string.chat_close_thread)).performClick()
        compose.waitForIdle()

        assertEquals(1, closed)
    }
}
