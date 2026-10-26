package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterLabelAssigneesTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** The user IDs of the chatters tapped. */
    private val tapped = mutableListOf<String>()

    /** The user IDs of the chatters whose Delete was chosen. */
    private val deleted = mutableListOf<String>()

    private fun showList(enabled: Boolean) {
        val assignees = listOf(
            ChatterLabelAssignee("1", login = "anna", displayName = "Anna", assignedAtMillis = null),
            ChatterLabelAssignee("2", login = "bob", displayName = "Bob", assignedAtMillis = null),
        )
        compose.setContent {
            // While searching, the list shows at once.
            ChatterLabelAssignees(
                assignees = assignees,
                searching = true,
                enabled = enabled,
                onChatterTap = { tapped += it.userId },
                onChatterDelete = { deleted += it.userId },
            )
        }
    }

    @Test
    fun `a long press on a chatter offers to delete them`() {
        showList(enabled = true)

        compose.onNodeWithText("Bob").performTouchInput { longClick() }
        compose.onNodeWithText("Delete").performClick()

        assertEquals(listOf("2"), deleted)
        assertEquals(emptyList<String>(), tapped)
    }

    @Test
    fun `a tap on a chatter opens them without the menu`() {
        showList(enabled = true)

        compose.onNodeWithText("Bob").performClick()

        assertEquals(listOf("2"), tapped)
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun `a long press while the page saves offers nothing`() {
        showList(enabled = false)

        compose.onNodeWithText("Bob").performTouchInput { longClick() }

        compose.onNodeWithText("Delete").assertDoesNotExist()
    }
}
