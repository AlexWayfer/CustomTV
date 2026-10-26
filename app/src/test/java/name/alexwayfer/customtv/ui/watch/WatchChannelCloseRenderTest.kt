package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which navigation does not need.
@Config(application = Application::class)
class WatchChannelCloseRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun showWatchRoutes(): NavHostController {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable(route = "home") {}
                composable(route = WATCH_CHANNEL_ROUTE, arguments = watchChannelArguments) {}
            }
        }
        compose.waitForIdle()
        return navController
    }

    @Test
    fun closingARaidTargetClosesTheRaidedChannelToo() {
        val navController = showWatchRoutes()
        compose.runOnIdle {
            navController.navigate("watch/streamer")
            navController.navigate("watch/raidtarget")
        }
        compose.runOnIdle { navController.closeWatchedChannels() }
        compose.runOnIdle { assertEquals("home", navController.currentDestination?.route) }
    }

    @Test
    fun closingTheOnlyChannelReturnsHome() {
        val navController = showWatchRoutes()
        compose.runOnIdle { navController.navigate("watch/streamer") }
        compose.runOnIdle { navController.closeWatchedChannels() }
        compose.runOnIdle { assertEquals("home", navController.currentDestination?.route) }
    }
}
