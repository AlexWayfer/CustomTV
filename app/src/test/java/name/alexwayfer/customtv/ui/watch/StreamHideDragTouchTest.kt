package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.player.PlayerPlayback
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val INFO = "info"

/**
 * The drag's travel, and the player's room over the info: the info moves as far as the finger, so a drag measured on
 * the info itself would not move at all.
 */
private const val TRAVEL_DP = 400

/** One step a frame; twelve go past the threshold of 0.42 of the travel, five stay short of it. */
private const val STEP_DP = -20
private const val FAR_STEPS = 12
private const val SHORT_STEPS = 5

/** Four frames at 120 Hz: short of the threshold, but faster than the minimize gesture's fling. */
private const val FLING_STEP_DP = -30f
private const val FLING_STEPS = 4
private const val FRAME_MILLIS = 8L

private const val DELTA = 0.001f
private const val POSITION_DELTA_DP = 0.5f

/** Halfway through the player's 460 ms slide. */
private const val HALF_SLIDE_MILLIS = 230L

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a drag on the info does not need.
@Config(application = Application::class, qualifiers = "w400dp-h800dp")
class StreamHideDragTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** What the hiding asked of the player and reported, in order. */
    private val events = mutableListOf<String>()
    private lateinit var hiding: StreamHiding

    private fun showInfo(overScreen: Boolean = false) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            hiding = remember { StreamHiding(scope, pause = { events += "pause" }, play = { events += "play" }) }
            SideEffect { hiding.overScreen = overScreen }
            val drag = rememberStreamHideDrag(
                travelPx = with(LocalDensity.current) { TRAVEL_DP.dp.toPx() },
                onSwitched = { events += "switched" },
            )
            // In landscape the content takes the screen, as tall as the drag travels.
            val height = if (overScreen) TRAVEL_DP.dp else 600.dp
            Column(Modifier.size(360.dp, height).hiddenStreamContentShift(hiding) { 0 }) {
                Spacer(Modifier.fillMaxWidth().streamRoom(hiding).height(TRAVEL_DP.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag(INFO)
                        .streamHideDrag(hiding, drag),
                )
            }
        }
    }

    private fun hideFirst() {
        compose.runOnIdle { hiding.set(true) }
        compose.waitForIdle()
        events.clear()
    }

    private fun drag(stepDp: Int, steps: Int, lift: Boolean = true) {
        compose.dragInSteps(compose.onNodeWithTag(INFO), 0.dp, stepDp.dp, steps, lift)
    }

    @Test
    fun `a drag up past the threshold hides and pauses the stream`() {
        showInfo()

        drag(STEP_DP, FAR_STEPS)

        assertEquals(listOf("pause", "switched"), events)
        assertTrue(hiding.hidden)
        assertEquals(1f, hiding.fraction, DELTA)
    }

    @Test
    fun `the info follows the finger up`() {
        showInfo()

        drag(STEP_DP, SHORT_STEPS, lift = false)

        assertTrue(hiding.fraction > 0f)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `a drag up short of the threshold slides the stream back`() {
        showInfo()

        drag(STEP_DP, SHORT_STEPS)

        assertEquals(emptyList<String>(), events)
        assertFalse(hiding.hidden)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `a short fast fling up hides the stream`() {
        showInfo()

        compose.onNodeWithTag(INFO).performTouchInput {
            down(center)
            repeat(FLING_STEPS) { moveBy(Offset(0f, FLING_STEP_DP.dp.toPx()), delayMillis = FRAME_MILLIS) }
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf("pause", "switched"), events)
        assertTrue(hiding.hidden)
    }

    @Test
    fun `a drag down on the shown stream does nothing`() {
        showInfo()

        drag(-STEP_DP, FAR_STEPS)

        assertEquals(emptyList<String>(), events)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `a drag down past the threshold brings the hidden stream back and plays it`() {
        showInfo()
        hideFirst()

        drag(-STEP_DP, FAR_STEPS)

        assertEquals(listOf("play", "switched"), events)
        assertFalse(hiding.hidden)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `a drag down short of the threshold keeps the stream hidden`() {
        showInfo()
        hideFirst()

        drag(-STEP_DP, SHORT_STEPS)

        assertEquals(emptyList<String>(), events)
        assertTrue(hiding.hidden)
        assertEquals(1f, hiding.fraction, DELTA)
    }

    @Test
    fun `a cancelled drag past the threshold leaves the stream shown`() {
        showInfo()

        drag(STEP_DP, FAR_STEPS, lift = false)
        compose.onNodeWithTag(INFO).performTouchInput { cancel() }
        compose.waitForIdle()

        assertEquals(emptyList<String>(), events)
        assertFalse(hiding.hidden)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `in landscape the info goes down under the finger with the stream over it`() {
        showInfo(overScreen = true)
        hideFirst()
        compose.onNodeWithTag(INFO).assertTopPositionInRootIsEqualTo(0.dp)

        drag(-STEP_DP, SHORT_STEPS, lift = false)

        assertTrue(hiding.fraction > 0f && hiding.fraction < 1f)
        // Right under the video's bottom edge, which is as far down as the stream is in sight.
        val top = compose.onNodeWithTag(INFO).getUnclippedBoundsInRoot().top
        assertEquals(TRAVEL_DP * (1f - hiding.fraction), top.value, POSITION_DELTA_DP)
        assertTrue(top > 0.dp)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `in landscape a drag down past the threshold brings the stream back`() {
        showInfo(overScreen = true)
        hideFirst()

        drag(-STEP_DP, FAR_STEPS)

        assertEquals(listOf("play", "switched"), events)
        assertFalse(hiding.hidden)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `in landscape the menu's switch slides the stream out`() {
        showInfo(overScreen = true)

        compose.mainClock.autoAdvance = false
        compose.runOnIdle { hiding.set(true) }
        compose.mainClock.advanceTimeBy(HALF_SLIDE_MILLIS)

        assertTrue(hiding.fraction > 0f && hiding.fraction < 1f)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(1f, hiding.fraction, DELTA)
    }

    @Test
    fun `the menu's switch hides and shows the stream`() {
        showInfo()

        compose.runOnIdle { hiding.set(true) }
        compose.waitForIdle()
        assertEquals(1f, hiding.fraction, DELTA)
        compose.runOnIdle { hiding.set(false) }
        compose.waitForIdle()

        assertEquals(listOf("pause", "play"), events)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `playing again elsewhere shows the hidden stream without playing it twice`() {
        showInfo()
        hideFirst()

        compose.runOnIdle { hiding.playbackChanged(PlayerPlayback.Playing) }
        compose.waitForIdle()

        assertEquals(emptyList<String>(), events)
        assertFalse(hiding.hidden)
        assertEquals(0f, hiding.fraction, DELTA)
    }

    @Test
    fun `the paused stream stays hidden`() {
        showInfo()
        hideFirst()

        compose.runOnIdle { hiding.playbackChanged(PlayerPlayback.Paused) }
        compose.waitForIdle()

        assertTrue(hiding.hidden)
        assertEquals(1f, hiding.fraction, DELTA)
    }
}
