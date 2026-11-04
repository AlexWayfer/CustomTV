package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterAssignmentCommentTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val saved = mutableListOf<String>()
    private val tapped = mutableListOf<String>()
    private var comment by mutableStateOf("")
    private var failure: UiText? = null

    private fun show(enabled: Boolean = true) {
        compose.setContent {
            ChatterLabelAssignees(
                assignees = listOf(ChatterLabelAssignee("1", "anna", "Anna", null, comment)),
                searching = true,
                enabled = enabled,
                onChatterTap = { tapped += it.userId },
                onChatterDelete = {},
                onCommentSave = { assignee, value ->
                    assertEquals("1", assignee.userId)
                    saved += value
                    if (failure == null) comment = value
                    failure
                },
            )
        }
    }

    @Test
    fun `the comment button opens an empty field without opening the profile`() {
        show()
        compose.onNodeWithText(FIELD).assertDoesNotExist()

        compose.onNodeWithContentDescription(TOGGLE).performClick()

        compose.onNodeWithText(FIELD).assertExists()
        compose.onNodeWithContentDescription("Save").assertIsNotEnabled()
        assertEquals(emptyList<String>(), tapped)
    }

    @Test
    fun `an existing comment opens immediately and editing saves only that chatter`() {
        comment = "old"
        show()
        compose.onNodeWithText(FIELD).assertTextContains("old")

        compose.onNodeWithText(FIELD).performTextReplacement("new")
        compose.onNodeWithContentDescription("Save").assertIsEnabled().performClick()

        assertEquals(listOf("new"), saved)
        compose.onNodeWithText(FIELD).assertTextContains("new")
        compose.onNodeWithContentDescription("Save").assertIsNotEnabled()
    }

    @Test
    fun `clearing the comment saves an empty string and closes the field`() {
        comment = "old"
        show()

        compose.onNodeWithText(FIELD).performTextReplacement("")
        compose.onNodeWithContentDescription("Save").performClick()

        assertEquals(listOf(""), saved)
        compose.onNodeWithText(FIELD).assertDoesNotExist()
    }

    @Test
    fun `a failed save keeps the draft and offers another save`() {
        comment = "old"
        failure = UiText.Raw("Save failed")
        show()

        compose.onNodeWithText(FIELD).performTextReplacement("draft")
        compose.onNodeWithContentDescription("Save").performClick()

        compose.onNodeWithText("Save failed").assertExists()
        compose.onNodeWithText(FIELD).assertTextContains("draft")
        compose.onNodeWithContentDescription("Save").assertIsEnabled()
        assertEquals("old", comment)
    }

    @Test
    fun `closing and reopening the field keeps an unsaved comment`() {
        show()
        compose.onNodeWithContentDescription(TOGGLE).performClick()
        compose.onNodeWithText(FIELD).performTextReplacement("draft")

        compose.onNodeWithContentDescription(TOGGLE).performClick()
        compose.onNodeWithContentDescription(TOGGLE).performClick()

        compose.onNodeWithText(FIELD).assertTextContains("draft")
    }

    @Test
    fun `the comment cannot be opened while the page is busy`() {
        show(enabled = false)

        compose.onNodeWithContentDescription(TOGGLE).assertIsNotEnabled()
        compose.onNodeWithText(FIELD).assertDoesNotExist()
    }

    private companion object {
        const val FIELD = "Comment (optional)"
        const val TOGGLE = "Comment for Anna"
    }
}
