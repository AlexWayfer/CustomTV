package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A raid whose time has come, so its countdown stops at once. */
private val RAID = OutgoingRaid(
    id = "1",
    targetLogin = "target",
    targetDisplayName = "Target",
    viewerCount = 42,
    goAtMillis = 0L,
    leaving = false,
)

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a notice does not need.
@Config(application = Application::class)
class ChatRaidNoticeRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `the raid notice lays out at any size, narrower than its timer too`() {
        compose.renderAtEdgeSizes {
            ChatRaidNotice(RAID, chatTextSize = 14, dotInset = 6.dp)
        }

        compose.onNode(hasContentDescription(RAID.targetDisplayName, substring = true)).assertExists()
    }
}
