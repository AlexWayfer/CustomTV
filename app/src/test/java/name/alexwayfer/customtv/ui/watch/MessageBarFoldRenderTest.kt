package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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

private const val BAR = "bar"
private const val FIELD = "field"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class MessageBarFoldRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun showBar(shown: Float) {
        compose.setContent {
            Box(Modifier.testTag(BAR).foldedAway { shown }) {
                Box(Modifier.size(100.dp, 40.dp).testTag(FIELD))
            }
        }
    }

    @Test
    fun `an open bar keeps its height`() {
        showBar(1f)

        compose.onNodeWithTag(BAR).assertHeightIsEqualTo(40.dp)
        compose.onNodeWithTag(FIELD).assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `a half folded bar is cut from its top`() {
        showBar(0.5f)

        compose.onNodeWithTag(BAR).assertHeightIsEqualTo(20.dp)
        compose.onNodeWithTag(FIELD).assertTopPositionInRootIsEqualTo((-20).dp)
    }

    @Test
    fun `a folded bar takes no height`() {
        showBar(0f)

        compose.onNodeWithTag(BAR).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun `a folding bar lays out at any size`() {
        compose.renderAtEdgeSizes {
            listOf(0f, 0.5f, 1f).forEach { shown ->
                Box(Modifier.foldedAway { shown }) {
                    Box(Modifier.fillMaxWidth().height(40.dp))
                }
            }
        }
    }
}
