package name.alexwayfer.customtv.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PILLS = "pills"

/** Writes down what the row of pills leaves to the pager around it: the rest of a drag, and of a fling. */
private class Pager : NestedScrollConnection {
    var dragX = 0f
    var flingX = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        dragX += available.x
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        flingX += available.x
        return Velocity.Zero
    }
}

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a row of pills does not need.
@Config(application = Application::class)
class HorizontalScrollEdgesTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val pager = Pager()

    @Before
    fun showPills() {
        compose.setContent {
            Box(Modifier.nestedScroll(pager)) {
                OneLineTagPills(Modifier.width(300.dp).testTag(PILLS)) {
                    repeat(6) { Box(Modifier.size(100.dp, 32.dp)) }
                }
            }
        }
    }

    private val pills get() = compose.onNodeWithTag(PILLS)

    @Test
    fun `a drag past the row's start does not swipe the pager`() {
        compose.dragInSteps(pills, 10.dp, 0.dp, steps = 10)

        assertEquals(0f, pager.dragX)
    }

    @Test
    fun `a drag past the row's end does not swipe the pager`() {
        compose.dragInSteps(pills, (-20).dp, 0.dp, steps = 40)

        assertEquals(0f, pager.dragX)
    }

    @Test
    fun `a fling past the row's start does not fling the pager`() {
        compose.dragInSteps(pills, 60.dp, 0.dp, steps = 3)

        assertEquals(0f, pager.flingX)
    }
}
