package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHAT = "chat"
private const val VOTE = "vote"
private const val NOTE = "Which game next?"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a panel does not need.
@Config(application = Application::class)
class ChatNoticePagesTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var chatTaps = 0
    private var votes = 0

    @Before
    fun showPanelOverChat() {
        compose.setContent {
            Box(Modifier.size(300.dp, 400.dp)) {
                // A chat row under the panel, which opens a chatter or an emote on a tap.
                Box(Modifier.size(300.dp, 400.dp).testTag(CHAT).clickable { chatTaps++ })
                Column(Modifier.fillMaxWidth().blockChatTouches()) {
                    Text(NOTE, Modifier.fillMaxWidth().height(40.dp))
                    Box(Modifier.size(100.dp, 40.dp).testTag(VOTE).clickable { votes++ })
                }
            }
        }
    }

    @Test
    fun `a tap on the panel's text does not reach the chat under it`() {
        compose.onNodeWithText(NOTE).performClick()

        assertEquals(0, chatTaps)
    }

    @Test
    fun `the panel's buttons still take their taps`() {
        compose.onNodeWithTag(VOTE).performClick()

        assertEquals(1, votes)
        assertEquals(0, chatTaps)
    }

    @Test
    fun `a tap on the chat beside the panel reaches it`() {
        compose.onNodeWithTag(CHAT).performTouchInput { click(bottomCenter.copy(y = bottom - 20f)) }

        assertEquals(1, chatTaps)
    }
}
