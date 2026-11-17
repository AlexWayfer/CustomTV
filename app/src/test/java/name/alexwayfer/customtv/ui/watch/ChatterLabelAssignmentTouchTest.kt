package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.data.LabeledChatter
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

    @Test
    fun `browser login checks show a disabled button and explain why`() {
        compose.setContent {
            ChatterLabelCheck(
                script = "return document.cookie",
                user = LabeledChatter("123", "chatter", "Chatter"),
                channel = null,
                enabled = true,
            )
        }

        compose.onNodeWithText("Check").assertIsNotEnabled()
        compose.onNodeWithText("This check is only available in the Chatter Labels browser extension.").assertExists()
    }

    @Test
    fun `Twitch helper shows the edition specific explanation without experimental login`() {
        compose.setContent {
            ChatterLabelCheck(
                script = "return await twitch.gql(query)",
                user = LabeledChatter("123", "chatter", "Chatter"),
                channel = null,
                enabled = true,
            )
        }

        val message = ApplicationProvider.getApplicationContext<Application>()
            .getString(R.string.chatter_check_twitch_session_required)
        compose.onNodeWithText("Check").assertIsNotEnabled()
        compose.onNodeWithText(message).assertExists()
    }

    @Test
    fun `archive check can be started without Twitch web login`() {
        compose.setContent {
            ChatterLabelCheck(
                script = "return await bestLogs.countMessages(['twitch'])",
                user = LabeledChatter("123", "chatter", "Chatter"),
                channel = null,
                enabled = true,
            )
        }

        compose.onNodeWithText("Check").assertIsEnabled()
    }

    private companion object {
        const val FIELD = "Comment (optional)"
    }
}
