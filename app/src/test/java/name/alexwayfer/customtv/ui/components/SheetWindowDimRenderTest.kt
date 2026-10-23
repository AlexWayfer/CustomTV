package name.alexwayfer.customtv.ui.components

import android.app.Application
import android.view.Window
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.window.DialogWindowProvider
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A sheet's own window dims what is under it, until [NoSheetWindowDim] takes the dim off. */
@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a sheet does not need.
@Config(application = Application::class)
class SheetWindowDimRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var window: Window? = null

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showSheet(content: @Composable () -> Unit) {
        compose.setContent {
            ModalBottomSheet(onDismissRequest = {}) {
                val view = LocalView.current
                // Robolectric's dialog theme does not dim as the phone's does: dim the sheet's window the same way.
                SideEffect {
                    window = (view.parent as? DialogWindowProvider)?.window
                    window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
                content()
                Text("Sheet")
            }
        }
        compose.waitForIdle()
    }

    private fun dims(): Boolean = window!!.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0

    @Test
    fun aDimmedSheetWindowStaysDimmedWithoutTheHelper() {
        showSheet {}

        assertTrue(dims())
    }

    @Test
    fun theHelperTakesTheDimOffTheSheetWindow() {
        showSheet { NoSheetWindowDim() }

        assertFalse(dims())
    }
}
