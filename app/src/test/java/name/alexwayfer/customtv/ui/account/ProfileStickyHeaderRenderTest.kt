package name.alexwayfer.customtv.ui.account

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scrolled far past the header, so it has folded into the bar. */
private const val FOLDED_SCROLL_PX = 10_000

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a header does not need.
@Config(application = Application::class)
class ProfileStickyHeaderRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `the header lays out open and folded at any size, narrower than its avatar too`() {
        compose.renderAtEdgeSizes {
            listOf(0, FOLDED_SCROLL_PX).forEach { scroll ->
                ProfileStickyHeader(
                    headerTop = 80.dp,
                    barNameScale = 0.8f,
                    roomForMenu = true,
                    scroll = { scroll },
                    avatar = { Box(Modifier.size(80.dp)) },
                    name = { Text("A display name long enough to wrap") },
                    details = { Text("12K followers · Last live 2 days ago") },
                )
            }
        }
    }
}
