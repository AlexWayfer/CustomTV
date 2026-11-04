package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerPipLayoutRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun thePageKeepsItsSizeWhileTheWindowShrinksAndExpands() {
        var window by mutableStateOf(DpSize(320.dp, 180.dp))
        var pageSize = IntSize.Zero
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.requiredSize(window), propagateMinConstraints = true) {
                    Box(
                        Modifier.playerScaledFromFullWidth(640)
                            .onSizeChanged { pageSize = it },
                    )
                }
            }
        }
        listOf(DpSize(320.dp, 180.dp), DpSize(160.dp, 90.dp), DpSize(240.dp, 135.dp), DpSize(320.dp, 180.dp))
            .forEach { next ->
                compose.runOnIdle { window = next }
                compose.waitForIdle()
                compose.runOnIdle { assertEquals(IntSize(640, 360), pageSize) }
            }
    }

    @Test
    fun theScaledPageLaysOutAtEmptyAndNarrowWindowSizes() {
        compose.renderAtEdgeSizes {
            Box(Modifier.playerScaledFromFullWidth(640))
        }
    }
}
