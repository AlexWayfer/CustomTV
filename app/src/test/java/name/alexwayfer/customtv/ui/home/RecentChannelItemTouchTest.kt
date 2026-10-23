package name.alexwayfer.customtv.ui.home

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private const val ITEM = "item"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a recent channel does not need.
@Config(application = Application::class)
class RecentChannelItemTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private var watched = 0
    private var removed = 0
    private var vibrations = 0
    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            vibrations++
        }
    }

    @Before
    fun showItem() {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) { Item() }
        }
    }

    @Composable
    private fun Item() {
        RecentChannelItem(
            channel = "streamer",
            // No avatar, so nothing loads.
            refreshedProfile = ChannelProfile(login = "streamer", displayName = "Streamer", avatarUrl = null),
            onWatch = { watched++ },
            onRemove = { removed++ },
            modifier = Modifier.width(320.dp).testTag(ITEM),
        )
    }

    private val item get() = compose.onNodeWithTag(ITEM)

    /** Delete, under the row's end: a tap there reaches it only once a swipe uncovers it. */
    private fun tapDelete() {
        compose.onNodeWithContentDescription(app.getString(R.string.remove_recent)).performClick()
        compose.waitForIdle()
    }

    @Test
    fun `a swipe to the start uncovers delete`() {
        compose.dragInSteps(item, (-10).dp, 0.dp, steps = 8)

        tapDelete()

        assertEquals(1, removed)
        assertEquals(0, watched)
    }

    @Test
    fun `a short swipe springs the row back over delete`() {
        // Past the touch slop, so it is a swipe and not a tap, and short of a third of delete's width past it.
        compose.dragInSteps(item, (-10).dp, 0.dp, steps = 3)

        tapDelete()

        assertEquals(0, removed)
        assertEquals(1, watched)
    }

    @Test
    fun `a short fling to the start uncovers delete`() {
        // Short of a third of delete's width past the touch slop, but quick, sampled at 120 Hz.
        item.performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(-8.dp.toPx(), 0f), delayMillis = 8) }
            up()
        }
        compose.waitForIdle()

        tapDelete()

        assertEquals(1, removed)
    }

    @Test
    fun `a long press vibrates once`() {
        item.performTouchInput { longClick() }

        assertEquals(1, vibrations)
    }

    @Test
    fun `a swipe back to the end covers delete again`() {
        compose.dragInSteps(item, (-10).dp, 0.dp, steps = 8)

        compose.dragInSteps(item, 10.dp, 0.dp, steps = 8)
        tapDelete()

        assertEquals(0, removed)
        assertEquals(1, watched)
    }
}
