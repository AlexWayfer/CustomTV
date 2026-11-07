package name.alexwayfer.customtv.player

import android.app.Application
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TwitchPlayerScriptsTest {
    @Test
    fun qualityAssetsReceiveTheirControllerBeforeUsingIt() {
        val scripts = TwitchPlayerScripts(RuntimeEnvironment.getApplication().assets)
        val qualityScripts = listOf(
            scripts.quality("user"), scripts.soundOnly, scripts.videoQuality, scripts.hideTopOverlay,
        )
        for (script in qualityScripts) {
            val controller = script.indexOf("var customTvQualityFlow =")
            assertTrue(controller >= 0)
            assertTrue(controller < script.lastIndexOf("customTvQuality"))
        }
    }
}
