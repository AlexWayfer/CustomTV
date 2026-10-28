package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
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

/** One frame's move of a swipe toward the end, in LTR. */
private val STEP_MOVE = 10.dp

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

    private fun showRow(
        enabled: Boolean = true,
        canReply: Boolean = true,
        canDismiss: Boolean = false,
        canCopy: Boolean = false,
        stepCount: Int = 0,
    ) {
        compose.setContent {
            ChatReplySwipe(
                enabled = enabled,
                onReply = if (canReply) ({ events += "reply" }) else null,
                onSwipingChange = { events += if (it) "swiping" else "still" },
                onDismiss = if (canDismiss) ({ events += "dismiss" }) else null,
                onCopy = if (canCopy) ({ events += "copy" }) else null,
                steps = if (stepCount > 0) {
                    ChatSwipeSteps(
                        count = stepCount,
                        look = {
                            ChatSwipeStepLook(
                                ColorPainter(Color.White),
                                "step $it",
                                ChatSwipeColors(background = Color.Black, content = Color.White),
                            )
                        },
                        onStep = { events += "step $it" },
                    )
                } else {
                    null
                },
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
    fun `a swipe toward the end past the threshold copies`() {
        showRow(canCopy = true)

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Copy", "copy"), events)
    }

    @Test
    fun `a copy swipe short of the threshold springs back without a copy`() {
        showRow(canCopy = true)

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-SHORT_OF_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a cancelled copy swipe does not copy`() {
        showRow(canCopy = true)

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-PAST_THRESHOLD)
            cancel()
        }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a row that only copies still swipes toward the end`() {
        showRow(canReply = false, canCopy = true)

        touch {
            down(center)
            moveLeft(-SWIPE_START)
            moveLeft(-PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Copy", "copy"), events)
    }

    @Test
    fun `a swipe toward the end without an action there stays at rest`() {
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

    /**
     * Swipes the row toward the end in 10 dp moves, one a frame: the first two start the swipe and do not count, and
     * [countedDp] of travel follows them.
     */
    private fun swipeTowardEndInSteps(countedDp: Int, lift: Boolean = true) {
        compose.dragInSteps(
            compose.onNodeWithTag(ROW),
            dx = STEP_MOVE,
            dy = 0.dp,
            steps = 2 + countedDp / STEP_MOVE.value.toInt(),
            lift = lift,
        )
    }

    @Test
    fun `a swipe toward the end released past the threshold picks the first step`() {
        showRow(stepCount = 4)

        swipeTowardEndInSteps(countedDp = 90)

        assertEquals(listOf("swiping", "still", "end Step", "step 0"), events)
    }

    @Test
    fun `a swipe toward the end released between the third and the fourth step picks the third`() {
        showRow(stepCount = 4)

        swipeTowardEndInSteps(countedDp = 180)

        assertEquals(listOf("swiping", "still", "end Step", "step 2"), events)
    }

    @Test
    fun `a swipe toward the end past the last step picks the last`() {
        showRow(stepCount = 4)

        swipeTowardEndInSteps(countedDp = 300)

        assertEquals(listOf("swiping", "still", "end Step", "step 3"), events)
    }

    @Test
    fun `a swipe with steps short of the threshold springs back without a step`() {
        showRow(stepCount = 4)

        swipeTowardEndInSteps(countedDp = 30)

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `a cancelled swipe with steps picks none`() {
        showRow(stepCount = 4)

        swipeTowardEndInSteps(countedDp = 180, lift = false)
        touch { cancel() }

        assertEquals(listOf("swiping", "still", "end Back"), events)
    }

    @Test
    fun `steps take the swipe toward the end over copying`() {
        showRow(canCopy = true, stepCount = 4)

        swipeTowardEndInSteps(countedDp = 90)

        assertEquals(listOf("swiping", "still", "end Step", "step 0"), events)
    }

    @Test
    fun `a row with steps still replies toward the start`() {
        showRow(stepCount = 4)

        touch {
            down(center)
            moveLeft(SWIPE_START)
            moveLeft(PAST_THRESHOLD)
            up()
        }

        assertEquals(listOf("swiping", "still", "end Reply", "reply"), events)
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
