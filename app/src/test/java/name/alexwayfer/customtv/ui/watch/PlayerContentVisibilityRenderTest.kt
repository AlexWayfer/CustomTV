package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CONTENT = "content"
private const val CHAT = "chat"

/** The size the content had while it showed in full. */
private val FULL = 300.dp

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class PlayerContentVisibilityRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `hidden content is neither shown nor announced, and keeps its full size measured`() {
        var hidden by mutableStateOf(false)
        var measuredHeightPx = 0
        compose.setContent {
            val fullPx = with(LocalDensity.current) { FULL.roundToPx() }
            Box(Modifier.testTag(CONTENT)) {
                Box(Modifier.hiddenAtFullSize(hidden, Constraints.fixed(fullPx, fullPx))) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(CHAT)
                            .onSizeChanged { measuredHeightPx = it.height },
                    )
                }
            }
        }
        compose.onNodeWithTag(CHAT).assertIsDisplayed()

        compose.runOnIdle { hidden = true }

        compose.onNodeWithTag(CHAT).assertDoesNotExist()
        compose.onNodeWithTag(CONTENT).assertHeightIsEqualTo(0.dp)
        assertEquals(with(compose.density) { FULL.roundToPx() }, measuredHeightPx)

        compose.runOnIdle { hidden = false }

        compose.onNodeWithTag(CHAT).assertIsDisplayed()
    }

    @Test
    fun `hidden content lays out at any size`() {
        compose.renderAtEdgeSizes {
            Box(Modifier.hiddenAtFullSize(hidden = true, Constraints.fixed(300, 300))) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
