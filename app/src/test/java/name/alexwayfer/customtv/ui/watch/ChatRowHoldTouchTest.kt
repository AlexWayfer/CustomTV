package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val LIST = "list"
private const val ROWS = 100

/** The list opens here, with rows above and below it. */
private const val FIRST_SHOWN = 50

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a held row does not need.
@Config(application = Application::class)
class ChatRowHoldTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val stickToBottom = mutableStateOf(true)

    /** The rows the list was told to keep in place, in order; null lets it follow again. */
    private val anchors = mutableListOf<String?>()
    private val hold = ChatRowHold(
        stickToBottom = stickToBottom,
        onStickToBottomChange = mutableStateOf({ stick: Boolean -> stickToBottom.value = stick }),
        onAnchorChange = { anchors += it },
    )
    private lateinit var list: LazyListState
    private var rowClicks = 0

    private fun showChat(following: Boolean = true) {
        stickToBottom.value = following
        compose.setContent {
            list = rememberLazyListState(initialFirstVisibleItemIndex = FIRST_SHOWN)
            LazyColumn(
                state = list,
                modifier = Modifier
                    .size(300.dp, 200.dp)
                    .testTag(LIST)
                    .pointerInput(hold) { hold.trackTouches(this) },
            ) {
                items(ROWS, key = { "m$it" }) {
                    Box(Modifier.fillMaxWidth().height(20.dp).clickable { rowClicks++ })
                }
            }
        }
    }

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(LIST).performTouchInput(block)
        compose.waitForIdle()
    }

    @Test
    fun `a finger on a row holds new rows until it lifts, without taking the tap`() {
        showChat()

        touch { down(center) }
        assertTrue(hold.touching)

        touch { up() }
        assertFalse(hold.touching)
        assertEquals(1, rowClicks)
    }

    @Test
    fun `the touch lasts until the last finger lifts`() {
        showChat()

        touch {
            down(0, center)
            down(1, centerLeft)
            up(0)
        }
        assertTrue(hold.touching)

        touch { up(1) }
        assertFalse(hold.touching)
    }

    @Test
    fun `a cancelled touch lets new rows in`() {
        showChat()

        touch {
            down(center)
            cancel()
        }

        assertFalse(hold.touching)
    }

    @Test
    fun `a swipe on a following chat stops it at its top row`() {
        showChat()

        compose.runOnIdle { hold.onSwipingChange(true, list) }

        assertEquals(listOf("m$FIRST_SHOWN"), anchors)
        assertFalse(stickToBottom.value)
    }

    @Test
    fun `a swipe let go with rows below keeps the chat still`() {
        showChat()

        compose.runOnIdle {
            hold.onSwipingChange(true, list)
            hold.onSwipingChange(false, list)
            hold.onSwipeEnd(ChatSwipeRelease.Back, list)
        }

        assertEquals(listOf("m$FIRST_SHOWN"), anchors)
        assertFalse(stickToBottom.value)
    }

    @Test
    fun `a copy from a held row's menu follows the chat again`() {
        showChat()

        compose.runOnIdle {
            hold.select("m55", list)
            hold.dismissSelection(ChatRowHoldEnd.Copy, list)
        }

        assertEquals(listOf("m$FIRST_SHOWN", null), anchors)
        assertTrue(stickToBottom.value)
    }

    @Test
    fun `a held row on a chat that stays put leaves it as it is`() {
        showChat(following = false)

        compose.runOnIdle {
            hold.select("m55", list)
            hold.dismissSelection(ChatRowHoldEnd.Dismiss, list)
        }

        assertEquals(emptyList<String?>(), anchors)
        assertFalse(stickToBottom.value)
    }
}
