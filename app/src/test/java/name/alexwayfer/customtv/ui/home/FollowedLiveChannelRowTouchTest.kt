package name.alexwayfer.customtv.ui.home

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ROW = "row"
private const val LINE = "line"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a row does not need.
@Config(application = Application::class)
class FollowedLiveChannelRowTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val channel = FollowedChannel(
        id = "1",
        login = "streamer",
        displayName = "Streamer",
        isLive = true,
        categoryId = "29595",
        categoryName = "Dota 2",
    )
    private var watched = 0
    private var longPressed = 0
    private var toggled = 0

    private fun showRow(withLongPress: Boolean) {
        compose.setContent {
            FollowedLiveChannelRow(
                channel = channel,
                onWatch = { watched++ },
                onLongClick = if (withLongPress) ({ longPressed++ }) else null,
                onLongClickLabel = if (withLongPress) "Hide" else null,
                modifier = Modifier.width(320.dp).testTag(ROW),
            )
        }
    }

    @Test
    fun aTapWatchesTheStream() {
        showRow(withLongPress = true)
        compose.onNodeWithTag(ROW).performClick()

        assertEquals(1, watched)
        assertEquals(0, longPressed)
    }

    @Test
    fun aLongPressOpensTheCategoryMenu() {
        showRow(withLongPress = true)
        compose.onNodeWithTag(ROW).performTouchInput { longClick() }

        assertEquals(1, longPressed)
        assertEquals(0, watched)
    }

    @Test
    fun withoutAMenuALongPressWatchesAsATapDid() {
        showRow(withLongPress = false)
        compose.onNodeWithTag(ROW).performTouchInput { longClick() }

        assertEquals(1, watched)
        assertEquals(0, longPressed)
    }

    @Test
    fun aTapOnTheHiddenLineTogglesItsStreams() {
        compose.setContent {
            FollowedHiddenLine(
                count = 2,
                expanded = false,
                onToggle = { toggled++ },
                modifier = Modifier.width(320.dp).testTag(LINE),
            )
        }
        compose.onNodeWithTag(LINE).performClick()

        assertEquals(1, toggled)
    }
}
