package name.alexwayfer.customtv.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp

/**
 * Puts a finger on [node], moves it [steps] times by [dx] and [dy], one move a frame as a phone delivers them, so what
 * moves under the finger moves between the moves, and lifts it unless [lift] is false.
 */
internal fun ComposeContentTestRule.dragInSteps(
    node: SemanticsNodeInteraction,
    dx: Dp,
    dy: Dp,
    steps: Int,
    lift: Boolean = true,
) {
    node.performTouchInput { down(center) }
    waitForIdle()
    repeat(steps) {
        node.performTouchInput { moveBy(Offset(dx.toPx(), dy.toPx())) }
        waitForIdle()
    }
    if (lift) {
        node.performTouchInput { up() }
        waitForIdle()
    }
}
