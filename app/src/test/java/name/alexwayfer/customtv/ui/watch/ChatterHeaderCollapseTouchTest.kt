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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
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

private const val PROFILE = "profile"

/** A banner of 112 px that folds to a 40 px avatar row with 8 px around it: 56 px of fold. */
private const val RANGE_PX = 56f

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a card's scroll does not need.
@Config(application = Application::class)
class ChatterHeaderCollapseTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var scroll: ScrollState
    private lateinit var collapse: ChatterHeaderCollapse

    private fun showProfile(contentHeight: Dp = 1000.dp) {
        compose.setContent {
            scroll = rememberScrollState()
            collapse = remember {
                ChatterHeaderCollapse(expandedPx = 112, foldedAvatarPx = 40, foldedMarginPx = 8, scrollState = scroll)
            }
            Column(
                Modifier
                    .size(300.dp, 200.dp)
                    .testTag(PROFILE)
                    .nestedScroll(collapse)
                    .verticalScroll(scroll),
            ) {
                Box(Modifier.fillMaxWidth().height(contentHeight))
            }
        }
    }

    private val profile get() = compose.onNodeWithTag(PROFILE)

    @Test
    fun `a short scroll up folds the banner before the profile moves`() {
        showProfile()

        compose.dragInSteps(profile, 0.dp, (-10).dp, steps = 4, lift = false)

        assertTrue("folded ${collapse.collapsedPx}", collapse.collapsedPx > 0f)
        assertEquals(0, scroll.value)
    }

    @Test
    fun `a long scroll up folds the banner all the way, then moves the profile`() {
        showProfile()

        compose.dragInSteps(profile, 0.dp, (-10).dp, steps = 15, lift = false)

        assertEquals(RANGE_PX, collapse.collapsedPx)
        assertTrue("scrolled ${scroll.value}", scroll.value > 0)
    }

    @Test
    fun `a scroll down brings the profile to its top, then unfolds the banner`() {
        showProfile()
        compose.dragInSteps(profile, 0.dp, (-10).dp, steps = 15)

        // Far enough to cover the fling the first drag left too.
        compose.dragInSteps(profile, 0.dp, 10.dp, steps = 50, lift = false)

        assertEquals(0, scroll.value)
        assertEquals(0f, collapse.collapsedPx)
    }

    @Test
    fun `a profile that fits keeps its banner`() {
        showProfile(contentHeight = 100.dp)

        compose.dragInSteps(profile, 0.dp, (-10).dp, steps = 15, lift = false)

        assertEquals(0f, collapse.collapsedPx)
    }
}
