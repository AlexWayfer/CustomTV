package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val INFO = "info"

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a layout does not need.
@Config(application = Application::class)
class StreamRoomRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun renderRoom(hiddenFraction: Float) {
        compose.renderAtEdgeSizes {
            Column {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .collapsingHeight { hiddenFraction }
                        .aspectRatio(VIDEO_ASPECT_RATIO),
                )
                Box(Modifier.fillMaxWidth().weight(1f).testTag(INFO))
            }
        }
        compose.onNodeWithTag(INFO).assertExists()
    }

    @Test
    fun `the player's room lays out at any size as it closes`() {
        renderRoom(hiddenFraction = 0.6f)
    }

    @Test
    fun `the closed room lays out at any size`() {
        renderRoom(hiddenFraction = 1f)
    }

    @Test
    fun `a room closed past its height stays at zero`() {
        renderRoom(hiddenFraction = 1.5f)
    }
}
