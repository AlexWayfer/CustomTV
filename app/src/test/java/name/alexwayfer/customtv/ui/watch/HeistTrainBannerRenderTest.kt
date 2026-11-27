package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.ChannelHypeTrain
import name.alexwayfer.customtv.chat.HypeTrainPhase
import name.alexwayfer.customtv.chat.HypeTrainTheme
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = "en")
class HeistTrainBannerRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun aQuarterFullTrainNamesTheCampaignAndPaintsTheWholeHeader() {
        val train = ChannelHypeTrain(
            "train", HypeTrainPhase.Active, 1, 25, 100, 0L, "00C7AC", emptyList(), emptyList(),
            theme = HypeTrainTheme.Heist,
        )
        compose.setContent { ChatHypeTrainBanner(train, false, {}, 14, {}, 0.dp) }
        compose.onNode(hasContentDescription("Heist Train", substring = true), useUnmergedTree = true).assertExists()
        val pixels = compose.onNode(hasClickAction()).captureToImage().toPixelMap()
        // Inside the top padding, away from text and the clipped outer corners.
        val middle = pixels[(pixels.width * .55f).toInt(), 3]
        assertEquals(0xfc / 255f, middle.red, .02f)
        assertEquals(0x52 / 255f, middle.green, .02f)
        val left = pixels[pixels.width / 10, 3]
        val right = pixels[pixels.width * 9 / 10, 3]
        assertTrue(left.blue > left.red && right.red > right.blue)
    }
}
