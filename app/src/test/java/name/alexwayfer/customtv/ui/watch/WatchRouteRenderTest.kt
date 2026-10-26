package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.components.ChannelPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which navigation does not need.
@Config(application = Application::class)
class WatchRouteRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun opened(channel: ChannelPreview): ChannelPreview {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable(route = "home") {}
                composable(route = WATCH_CHANNEL_ROUTE, arguments = watchChannelArguments) {}
            }
        }
        compose.runOnIdle { navController.navigate(watchRoute(channel)) }
        return compose.runOnIdle { watchChannelPreview(navController.currentBackStackEntry?.arguments) }
    }

    @Test
    fun theStreamGetsTheNameAndAvatarItOpenedWith() {
        val channel = ChannelPreview("anna", "Анна & co?", "https://example.com/a.png?w=70&h=70")

        assertEquals(channel, opened(channel))
    }

    @Test
    fun aStreamOpenedByItsLoginAloneGetsNoNameOrAvatar() {
        assertEquals(ChannelPreview("anna"), opened(ChannelPreview("anna", " ", "")))
    }
}
