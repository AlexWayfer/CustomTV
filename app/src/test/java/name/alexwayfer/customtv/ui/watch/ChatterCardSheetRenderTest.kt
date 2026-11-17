package name.alexwayfer.customtv.ui.watch

import android.app.Application
import android.graphics.Insets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.renderAtEdgeSizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import kotlin.math.roundToInt
import android.view.WindowInsets as AndroidWindowInsets

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterCardSheetRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `the card accepts empty and narrow constraints with a keyboard taller than its limit`() {
        compose.renderAtEdgeSizes {
            val height = remember { mutableStateOf(100.dp) }
            Box(
                Modifier.chatterCardHeight(
                    height,
                    sheetInsets = WindowInsets(bottom = 20),
                    imeInsets = WindowInsets(bottom = 600),
                ).fillMaxSize(),
            )
        }
    }

    @Test
    fun `the visible content shrinks by keyboard height beyond the system bar and returns after closing`() {
        var keyboardHeight by mutableStateOf(0.dp)
        compose.setContent {
            val density = LocalDensity.current
            val height = remember { mutableStateOf(600.dp) }
            Box(Modifier.requiredSize(360.dp, 800.dp)) {
                Box(
                    Modifier
                        .testTag(CARD)
                        .chatterCardHeight(
                            height,
                            sheetInsets = WindowInsets(bottom = with(density) { 20.dp.roundToPx() }),
                            imeInsets = WindowInsets(bottom = with(density) { keyboardHeight.roundToPx() }),
                        )
                        .fillMaxSize(),
                )
            }
        }
        assertEquals(600.dp, cardHeight())

        compose.runOnIdle { keyboardHeight = 200.dp }
        assertEquals(420.dp, cardHeight())

        compose.runOnIdle { keyboardHeight = 0.dp }
        assertEquals(600.dp, cardHeight())
    }

    @Test
    @Config(sdk = [35], qualifiers = "w393dp-h800dp")
    fun `the labels save button moves above the keyboard and returns after closing`() {
        val history = MutableStateFlow(ChatterHistory())
        compose.setContent {
            ChatterCardSheet(
                card = ChatterCardRequest(
                    login = "",
                    displayName = "Chatter",
                    userId = "1",
                    badges = emptyList(),
                    badgeUrls = emptyMap(),
                    page = ChatterCardPage.Labels,
                ),
                selfLogin = "viewer",
                chatterHistory = history,
                renderMessage = {},
                renderLoggedMessage = {},
                onDismiss = {},
                onMention = null,
            )
        }
        val saveText = RuntimeEnvironment.getApplication().getString(R.string.chatter_card_labels_save)
        val save = compose.onNodeWithText(saveText)
        val closedBottom = save.getUnclippedBoundsInRoot().bottom

        dispatchKeyboardInset(240.dp)
        val openBottom = save.getUnclippedBoundsInRoot().bottom
        assertTrue("Save moved from $closedBottom to $openBottom", openBottom <= closedBottom - 180.dp)

        dispatchKeyboardInset(0.dp)
        assertEquals(closedBottom, save.getUnclippedBoundsInRoot().bottom)
    }

    private fun dispatchKeyboardInset(height: Dp) {
        compose.runOnIdle {
            val decor = requireNotNull(ShadowDialog.getLatestDialog().window).decorView
            val ime = AndroidWindowInsets.Type.ime()
            val bottom = (height.value * decor.resources.displayMetrics.density).roundToInt()
            decor.dispatchApplyWindowInsets(
                AndroidWindowInsets.Builder()
                    .setInsets(ime, Insets.of(0, 0, 0, bottom))
                    .setVisible(ime, bottom > 0)
                    .build(),
            )
        }
    }

    private fun cardHeight(): Dp {
        val bounds = compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        return bounds.bottom - bounds.top
    }

    private companion object {
        const val CARD = "card"
    }
}
