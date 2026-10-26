package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val LIST = "list"
/** 600dp of items in a 200dp list: a quick fling from the top runs into the end. */
private const val ITEMS = 30

/** Writes down what the list leaves to the page around it: the rest of a drag, of a fling's frames, and of a fling. */
private class Page : NestedScrollConnection {
    var dragY = 0f
    var flingFramesY = 0f
    var flingY = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) dragY += available.y else flingFramesY += available.y
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        flingY += available.y
        return Velocity.Zero
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ListEndFlingStopTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val page = Page()
    private lateinit var state: LazyListState

    private fun showList(atEnd: Boolean) {
        compose.setContent {
            state = rememberLazyListState(initialFirstVisibleItemIndex = if (atEnd) ITEMS - 1 else 0)
            val stop = rememberListEndFlingStop(state)
            Box(Modifier.nestedScroll(page)) {
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .size(300.dp, 200.dp)
                        .testTag(LIST)
                        .nestedScroll(stop.connection),
                    flingBehavior = stop.flingBehavior,
                ) {
                    items(ITEMS) { Box(Modifier.fillMaxWidth().height(20.dp)) }
                }
            }
        }
    }

    private val list get() = compose.onNodeWithTag(LIST)

    @Test
    fun `a fling past the end stays with the list`() {
        showList(atEnd = true)

        compose.dragInSteps(list, 0.dp, (-60).dp, steps = 3)

        assertEquals(0f, page.flingFramesY)
        assertEquals(0f, page.flingY)
    }

    @Test
    fun `a fling that runs into the end stops there`() {
        showList(atEnd = false)

        compose.dragInSteps(list, 0.dp, (-60).dp, steps = 3)

        assertEquals(0f, page.flingFramesY)
        assertEquals(0f, page.flingY)
    }

    @Test
    fun `a fling ends as it runs into the end, so the next tap is not taken to stop it`() {
        showList(atEnd = false)
        compose.dragInSteps(list, 0.dp, (-60).dp, steps = 3, lift = false)

        compose.mainClock.autoAdvance = false
        list.performTouchInput { up() }
        // Far less than the fling would take to slow down on its own.
        compose.mainClock.advanceTimeBy(300)

        assertFalse(state.canScrollForward)
        assertFalse(state.isScrollInProgress)
    }

    @Test
    fun `a drag past the end still moves the page`() {
        showList(atEnd = true)

        compose.dragInSteps(list, 0.dp, (-10).dp, steps = 10, lift = false)

        assertTrue("page got ${page.dragY}", page.dragY < 0f)
    }
}
