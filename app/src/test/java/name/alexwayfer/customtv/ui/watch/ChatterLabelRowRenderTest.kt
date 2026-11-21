package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterLabelRowRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun `the saved comment appears between the label name and the assignment date`() {
        val label = ChatterLabelIcon("a", "Friend", byteArrayOf(), assignedAtMillis = 0L, comment = "Helpful chatter")
        val locale = ApplicationProvider.getApplicationContext<Application>().resources.configuration.locales[0]
        val date = formatChatterLabelAssignedAt(0L, locale)
        compose.setContent { ChatterLabelRow(label) }

        val nameBounds = compose.onNodeWithText(label.name).getUnclippedBoundsInRoot()
        val commentBounds = compose.onNodeWithText(label.comment).getUnclippedBoundsInRoot()
        val dateBounds = compose.onNodeWithText(date).getUnclippedBoundsInRoot()
        assertTrue(nameBounds.bottom <= commentBounds.top)
        assertTrue(commentBounds.bottom <= dateBounds.top)
    }

    @Test
    fun `a comment without an assignment date updates and disappears when cleared`() {
        var label by mutableStateOf(ChatterLabelIcon("a", "Friend", byteArrayOf(), comment = "  Original comment  "))
        compose.setContent { ChatterLabelRow(label) }

        compose.onNodeWithText("Original comment").assertExists()
        compose.runOnIdle { label = ChatterLabelIcon("a", "Friend", byteArrayOf(), comment = "Edited comment") }
        compose.onNodeWithText("Original comment").assertDoesNotExist()
        compose.onNodeWithText("Edited comment").assertExists()
        compose.runOnIdle { label = ChatterLabelIcon("a", "Friend", byteArrayOf(), comment = "  ") }
        compose.onNodeWithText("Edited comment").assertDoesNotExist()
        compose.onNodeWithText("  ").assertDoesNotExist()
    }

    @Test
    fun `the assignment editor shows its draft without repeating the saved comment`() {
        val label = ChatterLabelIcon("a", "Friend", byteArrayOf(), comment = "Saved comment")
        compose.setContent {
            ChatterLabelAssignmentRow(
                label = label,
                checked = true,
                enabled = true,
                comment = "Draft comment",
                onCheckedChange = {},
                onCommentChange = {},
            )
        }

        compose.onNodeWithText("Draft comment").assertExists()
        compose.onNodeWithText(label.comment).assertDoesNotExist()
    }
}
