package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
private const val COVER = "cover"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class FullscreenInfoBarRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun renderBar(shown: Float, hidden: Float) {
        compose.renderAtEdgeSizes {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fullscreenInfoBar(
                            span = { width -> FullscreenInfoBarSpan(x = width / 4, width = width / 2) },
                            shown = { shown },
                            hidden = { hidden },
                            onHeight = {},
                        )
                        .height(80.dp)
                        .testTag(BAR),
                )
            }
        }
        compose.onNodeWithTag(BAR).assertExists()
    }

    @Test
    fun `the shown stream info lays out at any size`() {
        renderBar(shown = 1f, hidden = 0f)
    }

    @Test
    fun `the stream info on its way in lays out at any size`() {
        renderBar(shown = 0.4f, hidden = 0f)
    }

    @Test
    fun `the stream info rising with the hiding stream lays out at any size`() {
        renderBar(shown = 1f, hidden = 0.7f)
    }

    @Test
    fun `the video over the content lays out at any size, even with more room than it has`() {
        compose.renderAtEdgeSizes {
            BoxWithConstraints {
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                Box(Modifier.streamCover(width, height) { 100 }.testTag(COVER))
            }
        }
        compose.onNodeWithTag(COVER).assertExists()
    }
}
