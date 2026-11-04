package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterLabelAssignmentTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var checked by mutableStateOf(false)
    private var comment by mutableStateOf("saved")

    private fun show(enabled: Boolean) {
        compose.setContent {
            ChatterLabelAssignmentRow(
                label = ChatterLabelIcon("a", "Friend", byteArrayOf()),
                checked = checked,
                enabled = enabled,
                comment = comment,
                onCheckedChange = { checked = it },
                onCommentChange = { comment = it },
            )
        }
    }

    @Test
    fun `checking reveals the field and unchecking then checking retains its draft`() {
        show(enabled = true)
        compose.onNodeWithText(FIELD).assertDoesNotExist()

        compose.onNodeWithText("Friend").performClick()
        compose.onNodeWithText(FIELD).performTextReplacement("draft")
        compose.onNodeWithText("Friend").performClick()
        compose.onNodeWithText(FIELD).assertDoesNotExist()
        compose.onNodeWithText("Friend").performClick()

        compose.onNodeWithText(FIELD).assertTextContains("draft")
        assertEquals("draft", comment)
    }

    @Test
    fun `saving disables the checked label and its comment`() {
        checked = true
        show(enabled = false)

        compose.onNodeWithText("Friend").assertIsNotEnabled()
        compose.onNodeWithText(FIELD).assertIsNotEnabled()
    }

    private companion object {
        const val FIELD = "Comment (optional)"
    }
}
