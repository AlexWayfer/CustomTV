package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.ComposeHost
import name.alexwayfer.customtv.ui.dragInSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val COUNT = "Assigned to 2 chatters"
private const val ANNA = "Анна (anna)"

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterLabelShownInChatTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var shown by mutableStateOf(true)
    /** The user IDs of the chatters tapped. */
    private val tapped = mutableListOf<String>()

    /** The switch row and the chatters' row under it, as a label's card shows them. */
    private fun show(assignees: List<ChatterLabelAssignee>, searching: Boolean = false) {
        compose.setContent {
            Column {
                ChatterLabelShownInChat(shown = shown, enabled = true, onShownChange = { shown = it })
                ChatterLabelAssignees(
                    assignees = assignees,
                    searching = searching,
                    enabled = true,
                    onChatterTap = { tapped += it.userId },
                    onChatterDelete = {},
                )
            }
        }
    }

    private val twoChatters = listOf(
        ChatterLabelAssignee("1", login = "anna", displayName = "Анна", assignedAtMillis = null),
        ChatterLabelAssignee("2", login = "bob", displayName = "Bob", assignedAtMillis = null),
    )

    @Test
    fun `a tap on the count lists the chatters and another hides them`() {
        show(twoChatters)
        compose.onNodeWithText(ANNA).assertDoesNotExist()

        compose.onNodeWithText(COUNT).performClick()
        compose.onNodeWithText(ANNA).assertExists()
        compose.onNodeWithText("Bob").assertExists()

        compose.onNodeWithText(COUNT).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(ANNA).assertDoesNotExist()
    }

    @Test
    fun `a tap on the count does not switch the label`() {
        show(twoChatters)

        compose.onNodeWithText(COUNT).performClick()

        assertEquals(true, shown)
    }

    @Test
    fun `a tap elsewhere on the row switches the label and keeps the list closed`() {
        show(twoChatters)

        compose.onNodeWithText("Show in chat").performTouchInput { click(percentOffset(0.9f, 0.5f)) }
        compose.waitForIdle()

        assertFalse(shown)
        compose.onNodeWithText(ANNA).assertDoesNotExist()
    }

    @Test
    fun `a tap on a chatter opens their profile by login`() {
        show(twoChatters)
        compose.onNodeWithText(COUNT).performClick()

        compose.onNodeWithText("Bob").performClick()

        assertEquals(listOf("2"), tapped)
    }

    @Test
    fun `a tap on a chatter without a login is reported too, so the page can tell why nothing opens`() {
        show(listOf(ChatterLabelAssignee("3", login = "", displayName = "Mia", assignedAtMillis = null)))
        compose.onNodeWithText("Assigned to 1 chatter").performClick()

        compose.onNodeWithText("Mia").performClick()

        assertEquals(listOf("3"), tapped)
    }

    private val manyChatters = List(40) { index ->
        val login = "user" + index.toString().padStart(2, '0')
        ChatterLabelAssignee("$index", login = login, displayName = login, assignedAtMillis = null)
    }

    @Test
    fun `a tap on the last chatter after a fling to the end opens their profile`() {
        show(manyChatters)
        compose.onNodeWithText("Assigned to 40 chatters").performClick()

        compose.dragInSteps(compose.onNode(hasScrollAction()), 0.dp, (-80).dp, steps = 3)
        compose.onNodeWithText("user39").performClick()

        assertEquals(listOf("39"), tapped)
    }

    @Test
    fun `a tap on the last chatter after a drag to the end opens their profile`() {
        show(manyChatters)
        compose.onNodeWithText("Assigned to 40 chatters").performClick()

        repeat(5) { compose.dragInSteps(compose.onNode(hasScrollAction()), 0.dp, (-20).dp, steps = 15) }
        compose.onNodeWithText("user39").performClick()

        assertEquals(listOf("39"), tapped)
    }

    @Test
    fun `a label without chatters shows no count`() {
        show(emptyList())

        compose.onNodeWithText(COUNT).assertDoesNotExist()
        compose.onNodeWithText("Show in chat").assertExists()
    }

    @Test
    fun `while searching the matching chatters show at once, and a tap on their count does not hide them`() {
        show(twoChatters, searching = true)
        compose.onNodeWithText(ANNA).assertExists()

        compose.onNodeWithText("2 matching chatters").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(ANNA).assertExists()
        assertEquals(true, shown)
    }
}
