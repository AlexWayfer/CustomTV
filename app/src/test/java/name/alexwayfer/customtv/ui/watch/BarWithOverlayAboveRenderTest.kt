package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHAT = "chat"
private const val BAR = "bar"
private const val SUGGESTIONS = "suggestions"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class BarWithOverlayAboveRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `suggestions taller than the room above the bar shrink to it and leave the chat in place`() {
        compose.setContent {
            Column(Modifier.height(300.dp)) {
                Spacer(Modifier.weight(1f).fillMaxWidth().testTag(CHAT))
                BarWithOverlayAbove(
                    overlay = { Box(Modifier.fillMaxWidth().height(500.dp).testTag(SUGGESTIONS)) },
                ) {
                    Box(Modifier.fillMaxWidth().height(50.dp).testTag(BAR))
                }
            }
        }

        compose.onNodeWithTag(CHAT).assertHeightIsEqualTo(250.dp)
        compose.onNodeWithTag(BAR).assertTopPositionInRootIsEqualTo(250.dp)
        compose.onNodeWithTag(SUGGESTIONS).assertHeightIsEqualTo(250.dp)
        compose.onNodeWithTag(SUGGESTIONS).assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `short suggestions sit right above the bar`() {
        compose.setContent {
            Column(Modifier.height(300.dp)) {
                Spacer(Modifier.weight(1f))
                BarWithOverlayAbove(
                    overlay = { Box(Modifier.fillMaxWidth().height(80.dp).testTag(SUGGESTIONS)) },
                ) {
                    Box(Modifier.fillMaxWidth().height(50.dp))
                }
            }
        }

        compose.onNodeWithTag(SUGGESTIONS).assertTopPositionInRootIsEqualTo(170.dp)
    }

    @Test
    fun `the bar and its suggestions lay out at any size`() {
        compose.renderAtEdgeSizes {
            BarWithOverlayAbove(overlay = { Box(Modifier.fillMaxWidth().height(500.dp)) }) {
                Box(Modifier.fillMaxWidth().height(50.dp))
            }
        }
    }
}
