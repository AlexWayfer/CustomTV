package name.alexwayfer.customtv.ui.watch

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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

private val MESSAGES = listOf("1", "2").map { id ->
    ChatMessage(
        id = id,
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.Red,
        rawText = "hi",
        parts = listOf(ChatPart.Text("hi")),
        timestampMillis = 0L,
    )
}

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which the compact chat does not need.
@Config(application = Application::class)
class FullscreenCompactChatTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var expanded = 0
    private var rowTaps = 0
    private var rowMenus = 0

    @Before
    fun showCompactChat() {
        // Both came just now, so both show.
        val shownAt = MESSAGES.associate { it.id to SystemClock.uptimeMillis() }
        compose.setContent {
            FullscreenCompactChat(
                messages = MESSAGES,
                shownAt = shownAt,
                onExpand = { expanded++ },
                modifier = Modifier.width(300.dp),
            ) { message ->
                // A row as the chat draws it, with a tap and a long press of its own.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("row ${message.id}")
                        .combinedClickable(onLongClick = { rowMenus++ }, onClick = { rowTaps++ }),
                )
            }
        }
    }

    private val row get() = compose.onNodeWithTag("row 2")

    @Test
    fun `a tap on a message expands the chat instead of reaching the message`() {
        row.performClick()

        assertEquals(1, expanded)
        assertEquals(0, rowTaps)
    }

    @Test
    fun `holding a message does not open its menu`() {
        row.performTouchInput { longClick() }

        assertEquals(0, rowMenus)
    }
}
