package name.alexwayfer.customtv.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CARD = "card"

/** Wide, so a fling over a few frames stays short of a third of it. */
private const val CARD_WIDTH_DP = 750

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a swipe does not need.
@Config(application = Application::class, qualifiers = "w800dp-h600dp")
class SwipeAwayTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var dismissed = 0

    @Before
    fun showCard() {
        compose.setContent {
            SwipeAway(onDismiss = { dismissed++ }) {
                Box(Modifier.size(CARD_WIDTH_DP.dp, 80.dp).testTag(CARD))
            }
        }
    }

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(CARD).performTouchInput(block)
        compose.waitForIdle()
    }

    /**
     * Moves a finger [steps] times by [step], one move a frame as a phone delivers them, so the card moves between
     * them; then lifts it.
     */
    private fun swipe(step: Dp, steps: Int) {
        touch { down(center) }
        repeat(steps) { touch { moveBy(Offset(step.toPx(), 0f)) } }
        touch { up() }
    }

    @Test
    fun `a swipe left past a third of the card dismisses it`() {
        swipe((-10).dp, steps = 30)

        assertEquals(1, dismissed)
    }

    @Test
    fun `a swipe right past a third of the card dismisses it`() {
        swipe(10.dp, steps = 30)

        assertEquals(1, dismissed)
    }

    @Test
    fun `a short slow swipe springs the card back`() {
        swipe((-10).dp, steps = 10)

        assertEquals(0, dismissed)
    }

    @Test
    fun `a short fling dismisses the card`() {
        swipe((-60).dp, steps = 3)

        assertEquals(1, dismissed)
    }
}
