package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHAT = "chat"

/** Wide, so a fling over a few frames stays short of a third of it. */
private const val CHAT_WIDTH_DP = 750

/** How far the chat travels to the other side of the video. */
private const val TRAVEL_PX = 500

/** Past a third of the chat's width. */
private const val FAR_DP = 300f

/** Short of that third. */
private const val SHORT_DP = 100f

/** Short of that third too, but fast over three frames: a fling. */
private const val FLING_DP = 200f
private const val SLOW_MILLIS = 1000L
private const val FLING_MILLIS = 48L

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a swipe of the chat does not need.
@Config(application = Application::class, qualifiers = "w800dp-h600dp")
class FullscreenChatSwipeTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** What the swipe did with the chat, in order. */
    private val outcomes = mutableListOf<String>()
    private lateinit var swipe: FullscreenChatSwipeState

    private fun showChat(chatOnLeft: Boolean) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            swipe = remember { FullscreenChatSwipeState(scope) }
            val widthPx = with(LocalDensity.current) { CHAT_WIDTH_DP.dp.roundToPx() }
            Box(
                Modifier
                    .size(CHAT_WIDTH_DP.dp, 300.dp)
                    .testTag(CHAT)
                    .fullscreenChatSwipe(
                        swipe = swipe,
                        track = FullscreenChatSwipeTrack(chatOnLeft, widthPx, TRAVEL_PX),
                        onHide = { outcomes += "hide" },
                        onMoveAcross = { outcomes += "across" },
                    ),
            )
        }
    }

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(CHAT).performTouchInput(block)
        compose.waitForIdle()
    }

    @Test
    fun `a swipe toward the chat's own edge past a third hides it`() {
        showChat(chatOnLeft = true)

        touch { swipeLeft(centerX, centerX - FAR_DP.dp.toPx(), SLOW_MILLIS) }

        assertEquals(listOf("hide"), outcomes)
        assertEquals(0f, swipe.offsetX)
    }

    @Test
    fun `a swipe toward the other side past a third moves the chat across`() {
        showChat(chatOnLeft = true)

        touch { swipeRight(centerX, centerX + FAR_DP.dp.toPx(), SLOW_MILLIS) }

        assertEquals(listOf("across"), outcomes)
        assertEquals(0f, swipe.offsetX)
    }

    @Test
    fun `a chat on the right hides with a swipe right`() {
        showChat(chatOnLeft = false)

        touch { swipeRight(centerX, centerX + FAR_DP.dp.toPx(), SLOW_MILLIS) }

        assertEquals(listOf("hide"), outcomes)
    }

    @Test
    fun `a short slow swipe springs the chat back`() {
        showChat(chatOnLeft = true)

        touch { swipeLeft(centerX, centerX - SHORT_DP.dp.toPx(), SLOW_MILLIS) }

        assertEquals(emptyList<String>(), outcomes)
        assertEquals(0f, swipe.offsetX)
    }

    @Test
    fun `a short fling toward the edge hides the chat`() {
        showChat(chatOnLeft = true)

        touch { swipeLeft(centerX, centerX - FLING_DP.dp.toPx(), FLING_MILLIS) }

        assertEquals(listOf("hide"), outcomes)
    }

    @Test
    fun `a cancelled swipe springs the chat back`() {
        showChat(chatOnLeft = true)

        touch {
            down(center)
            moveBy(Offset(-FAR_DP.dp.toPx(), 0f))
            cancel()
        }

        assertEquals(emptyList<String>(), outcomes)
        assertEquals(0f, swipe.offsetX)
    }
}
