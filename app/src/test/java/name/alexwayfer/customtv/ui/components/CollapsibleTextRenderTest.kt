package name.alexwayfer.customtv.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
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

private const val TEXT = "text"

/** Where the first of the text's lines ends. */
private val FIRST_LINE = 20.dp

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class CollapsibleTextRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun showText(progress: Float) {
        compose.setContent {
            val firstLinePx = with(LocalDensity.current) { FIRST_LINE.roundToPx() }
            Box(Modifier.testTag(TEXT).revealLines({ firstLinePx }, { progress })) {
                Box(Modifier.size(100.dp, 100.dp))
            }
        }
    }

    @Test
    fun `a collapsed text shows its first line`() {
        showText(0f)

        compose.onNodeWithTag(TEXT).assertHeightIsEqualTo(FIRST_LINE)
    }

    @Test
    fun `a half open text shows half of the rest`() {
        showText(0.5f)

        compose.onNodeWithTag(TEXT).assertHeightIsEqualTo(60.dp)
    }

    @Test
    fun `an open text shows all its lines`() {
        showText(1f)

        compose.onNodeWithTag(TEXT).assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun `a text whose first line is taller than the room it gets lays out at any size`() {
        compose.renderAtEdgeSizes {
            val firstLinePx = with(LocalDensity.current) { FIRST_LINE.roundToPx() }
            listOf(0f, 0.5f, 1f).forEach { progress ->
                Box(Modifier.revealLines({ firstLinePx }, { progress })) {
                    Box(Modifier.fillMaxWidth().height(100.dp))
                }
            }
        }
    }
}
