package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CONTENT = "content"

/** Writes down what the content leaves to the sheet around it: the rest of a drag, and of a fling. */
private class Sheet : NestedScrollConnection {
    var dragY = 0f
    var flingY = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        dragY += available.y
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        flingY += available.y
        return Velocity.Zero
    }
}

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a sheet's scroll does not need.
@Config(application = Application::class)
class SheetScrollStopTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val sheet = Sheet()
    private lateinit var scroll: ScrollState

    private fun showContent(scrolledPx: Int = 0) {
        compose.setContent {
            scroll = rememberScrollState(scrolledPx)
            Box(Modifier.nestedScroll(sheet)) {
                Column(
                    Modifier
                        .size(300.dp, 200.dp)
                        .testTag(CONTENT)
                        .nestedScroll(rememberStopAtScrollTop(scroll, overscroll = null))
                        .verticalScroll(scroll),
                ) {
                    Box(Modifier.fillMaxWidth().height(1000.dp))
                }
            }
        }
    }

    private val content get() = compose.onNodeWithTag(CONTENT)

    @Test
    fun `a drag down from the top goes to the sheet`() {
        showContent()

        compose.dragInSteps(content, 0.dp, 10.dp, steps = 10)

        assertTrue("sheet got ${sheet.dragY}", sheet.dragY > 0f)
    }

    @Test
    fun `a drag that scrolls the content back to its top stops there`() {
        showContent(scrolledPx = 50)

        compose.dragInSteps(content, 0.dp, 10.dp, steps = 15)

        assertEquals(0, scroll.value)
        assertEquals(0f, sheet.dragY)
    }

    @Test
    fun `a new drag down from the top after the stop goes to the sheet`() {
        showContent(scrolledPx = 50)
        compose.dragInSteps(content, 0.dp, 10.dp, steps = 15)

        compose.dragInSteps(content, 0.dp, 10.dp, steps = 10)

        assertTrue("sheet got ${sheet.dragY}", sheet.dragY > 0f)
    }

    @Test
    fun `a drag that scrolled the content on its way stops at the top too`() {
        showContent()

        content.performTouchInput { down(center) }
        // Up first, which scrolls the content, then back down past its top, in one touch.
        listOf(-10f, -10f, -10f, -10f, -10f).plus(List(15) { 10f }).forEach { dy ->
            content.performTouchInput { moveBy(Offset(0f, dy.dp.toPx())) }
            compose.waitForIdle()
        }
        content.performTouchInput { up() }
        compose.waitForIdle()

        assertEquals(0, scroll.value)
        assertEquals(0f, sheet.dragY)
    }

    @Test
    fun `an upward fling past the end stays with the content`() {
        showContent(scrolledPx = Int.MAX_VALUE)

        compose.dragInSteps(content, 0.dp, (-60).dp, steps = 3)

        assertEquals(0f, sheet.flingY)
    }
}
