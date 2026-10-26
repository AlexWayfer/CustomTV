package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.ChatShoutoutNotice
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
// Real text layout, so a tap lands on the letters it aims at.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The plain application: the app's own starts its stores and workers, which a chat line does not need.
@Config(application = Application::class)
class ChatShoutoutLineTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val opened = mutableListOf<ChatterCardRequest>()

    private fun showLine(sourceName: String?) {
        compose.setContent {
            CompositionLocalProvider(LocalOpenChatterCard provides { opened += it }) {
                ChatShoutoutLine(
                    notice = ChatShoutoutNotice(
                        targetLogin = "target",
                        targetId = "200",
                        targetName = "Target",
                        sourceName = sourceName,
                    ),
                    timestampText = null,
                    textSize = 14.sp,
                    lineHeight = 18.sp,
                    badgeUrls = mapOf("moderator/1" to "file:///missing/moderator.png"),
                )
            }
        }
    }

    @Test
    fun aTapOnTheChannelsNameOpensItsCard() {
        showLine(sourceName = "Helper")

        compose.onNodeWithText("Helper gives a shoutout to Target").performTouchInput { click(nameEnd()) }

        val card = opened.single()
        assertEquals("target", card.login)
        assertEquals("Target", card.displayName)
        assertEquals("200", card.userId)
        assertEquals(false, card.badgesKnown)
        assertEquals(mapOf("moderator/1" to "file:///missing/moderator.png"), card.badgeUrls)
    }

    @Test
    fun aShoutoutWithoutItsSourceStillNamesTheChannel() {
        showLine(sourceName = null)

        compose.onNodeWithText("Shoutout to Target").performTouchInput { click(nameEnd()) }

        assertEquals("target", opened.single().login)
    }

    @Test
    fun aTapOnTheRestOfTheLineOpensNothing() {
        showLine(sourceName = "Helper")

        compose.onNodeWithText("Helper gives a shoutout to Target")
            .performTouchInput { click(centerLeft + Offset(4f, 0f)) }

        assertEquals(emptyList<ChatterCardRequest>(), opened)
    }

    /** A point on the channel's name, which ends the line. */
    private fun TouchInjectionScope.nameEnd(): Offset = centerRight - Offset(4f, 0f)
}
