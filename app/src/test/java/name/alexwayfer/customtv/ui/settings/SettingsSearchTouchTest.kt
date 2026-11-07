package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.theme.CustomTvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsSearchTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun selectingASettingHighlightsItWithoutTogglingAndBackKeepsTheQuery() {
        var toggles = 0
        var searchOpen = false
        compose.setContent {
            CustomTvTheme {
                var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
                SettingsSearchScaffold(
                    page, accountName = null, leaveGuard = remember { SettingsLeaveGuard() },
                    onBack = {}, onOpenPage = { page = it },
                    reselected = null, scrollTo = null, onReselect = {},
                    onSearchOpenChange = { searchOpen = it },
                ) { padding ->
                    SettingsPageColumn(modifier = Modifier.padding(padding)) {
                        SettingsToggle(
                            label = stringResource(R.string.smooth_sound),
                            hint = stringResource(R.string.smooth_sound_hint),
                            checked = false,
                            onCheckedChange = { toggles++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription(text(R.string.settings_search)).performClick()
        compose.runOnIdle { assertEquals(true, searchOpen) }
        compose.onNode(hasSetTextAction()).performTextInput("smooth")
        val result = hasText(text(R.string.smooth_sound)) and hasAnyAncestor(isDialog())
        compose.onNode(result).performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).assertIsOff()
        compose.runOnIdle {
            assertEquals(0, toggles)
            assertEquals(false, searchOpen)
        }
        compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("smooth")
        compose.onNode(result).assertExists()
        compose.onNodeWithContentDescription(text(R.string.search_clear)).performClick()
        compose.onNodeWithText(text(R.string.settings_search_hint)).assertExists()
    }

    private fun text(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    @Test
    fun fullScreenSearchKeepsTheSettingsPageComposedUnderItsOwnWindow() {
        var disposed = 0
        compose.setContent {
            CustomTvTheme {
                var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
                SettingsSearchScaffold(
                    page, accountName = null, leaveGuard = remember { SettingsLeaveGuard() },
                    onBack = {}, onOpenPage = { page = it },
                    reselected = null, scrollTo = null, onReselect = {},
                    onSearchOpenChange = {},
                ) {
                    DisposableEffect(Unit) { onDispose { disposed++ } }
                    Text(stringResource(R.string.settings_page_chat))
                }
            }
        }
        compose.onNodeWithContentDescription(text(R.string.settings_search)).performClick()
        compose.onNode(isDialog()).assertExists()
        compose.runOnIdle { assertEquals(0, disposed) }
    }
}
