package name.alexwayfer.customtv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The sizes a parent can give a part: none, a sliver, narrower than its paddings, each way alone, and a phone's.
 * An animated parent goes through them frame by frame.
 */
internal val EdgeSizes = listOf(
    DpSize(0.dp, 0.dp),
    DpSize(1.dp, 1.dp),
    DpSize(20.dp, 20.dp),
    DpSize(0.dp, 400.dp),
    DpSize(360.dp, 0.dp),
    DpSize(360.dp, 400.dp),
)

/**
 * Measures [content] at exactly each of [EdgeSizes] in turn; each part it shows gets that size. A size a part reports
 * below zero, or a value a Compose call refuses, throws here. It sets the test's content, so a test calls it once.
 */
internal fun ComposeContentTestRule.renderAtEdgeSizes(content: @Composable () -> Unit) {
    var size by mutableStateOf(EdgeSizes.first())
    setContent {
        Box(Modifier.requiredSize(size), propagateMinConstraints = true) { content() }
    }
    EdgeSizes.forEach { next ->
        runOnIdle { size = next }
        waitForIdle()
    }
}
