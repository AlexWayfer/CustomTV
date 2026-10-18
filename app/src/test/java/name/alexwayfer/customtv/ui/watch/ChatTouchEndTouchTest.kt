package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
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
private const val ROW = "row"

/** Counts how often the chat took the focus from the message field. */
private class FieldFocus : FocusManager {
    var cleared = 0

    override fun clearFocus(force: Boolean) {
        cleared++
    }

    override fun moveFocus(focusDirection: FocusDirection): Boolean = false
}

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a touch of the chat does not need.
@Config(application = Application::class)
class ChatTouchEndTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val focus = FieldFocus()
    private var rowClicks = 0

    @Before
    fun showChat() {
        compose.setContent {
            Box(Modifier.size(300.dp, 400.dp).testTag(CHAT).clearFocusOnTouch(focus)) {
                Box(Modifier.size(300.dp, 40.dp).testTag(ROW).clickable { rowClicks++ })
            }
        }
    }

    private fun touch(tag: String, block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(tag).performTouchInput(block)
        compose.waitForIdle()
    }

    @Test
    fun `a tap on the chat takes the focus from the field`() {
        touch(CHAT) {
            down(bottomCenter - Offset(0f, 20.dp.toPx()))
            up()
        }

        assertEquals(1, focus.cleared)
    }

    @Test
    fun `a tap on a row that takes it still takes the focus from the field`() {
        touch(ROW) {
            down(center)
            up()
        }

        assertEquals(1, rowClicks)
        assertEquals(1, focus.cleared)
    }

    @Test
    fun `a scroll keeps the focus in the field`() {
        touch(CHAT) {
            down(center)
            moveBy(Offset(0f, 60.dp.toPx()))
            up()
        }

        assertEquals(0, focus.cleared)
    }

    @Test
    fun `a long press keeps the focus in the field`() {
        touch(ROW) { longClick() }

        assertEquals(0, focus.cleared)
    }
}
