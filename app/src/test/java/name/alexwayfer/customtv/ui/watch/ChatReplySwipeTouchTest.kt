package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ROW = "row"

/** Past the 64 dp threshold after the swipe has started. */
private val PAST_THRESHOLD = 80.dp

/** Short of the threshold. */
private val SHORT_OF_THRESHOLD = 30.dp

/** Far enough sideways to start a swipe, which itself does not count toward the threshold. */
private val SWIPE_START = 40.dp

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a touch of a row does not need.
@Config(application = Application::class)
class ChatReplySwipeTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** Writes down what a swipe of the row reported, in order. */
    private val events = mutableListOf<String>()

    private fun showRow(enabled: Boolean = true, canReply: Boolean = true, canDismiss: Boolean = false) {
        compose.setContent {
            ChatReplySwipe(
                enabled = enabled,
                onReply = if (canReply) ({ events += "reply" }) else null,
                onSwipingChange = { events += if (it) "swiping" else "still" },
                onDismiss = if (canDismiss) ({ events += "dismiss" }) else null,
                onSwipeEnd = { events += "end $it" },
            ) {
                Box(Modifier.width(320.dp).height(40.dp).testTag(ROW))
            }
        }
    }

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(ROW).performTouchInput(block)
        compose.waitForIdle()
    }

    /** Moves the finger [left] to the left, or to the right when negative. */
    private fun TouchInjectionScope.moveLeft(left: Dp) {
        moveBy(Offset(-left.toPx(), 0f))
    }

    @Test
    fun `a swipe toward the start past the threshold replies`() {
        showRow()

        touch {
            down(center)
            moveLeft(SWIPE_START)
            moveLeft(PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Reply", "reply"), events)
    }

    @Test
    fun `a swipe short of the threshold springs back without a reply`() {
        showRow()

        touch {
            down(center)
            moveLeft(SWIPE_START)
            moveLeft(SHORT_OF_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a swipe toward the end past the threshold dismisses once the row slides out`() {
        showRow(canDismiss = true)

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Dismiss", "dismiss"), events)
    }

    @Test
    fun `a swipe toward the end without a dismissal stays at rest`() {
        showRow()

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a touch that moves vertically first stays a scroll`() {
        showRow()

        touch {
            down(center)
            moveBy(Offset(0f, SWIPE_START.toPx()))
            moveLeft(PAST_THRESHOLD)
            up()
        }

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `a cancelled swipe gives the list its scroll back without a reply`() {
        showRow()

        touch {
            down(center)
            moveLeft(SWIPE_START)
            moveLeft(PAST_THRESHOLD)
            cancel()
        }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a disabled row does not swipe`() {
        showRow(enabled = false)

        touch {
            down(center)
            moveLeft(SWIPE_START)
            moveLeft(PAST_THRESHOLD)
            up()
        }

        assertEquals(emptyList<String>(), events)
    }
}
