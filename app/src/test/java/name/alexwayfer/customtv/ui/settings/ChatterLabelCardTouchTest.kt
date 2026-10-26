package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sh.calvin.reorderable.ReorderableColumn

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatterLabelCardTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** The user IDs of the chatters tapped. */
    private val tapped = mutableListOf<String>()

    @Test
    fun `a tap on a chatter in a card on the page reaches the page`() {
        val label = EditableChatterLabel("a", "Friends", icon = "", enabled = true)
        val assignees = listOf(
            ChatterLabelAssignee("1", login = "anna", displayName = "Anna", assignedAtMillis = null),
            ChatterLabelAssignee("2", login = "bob", displayName = "Bob", assignedAtMillis = null),
        )
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReorderableColumn(list = listOf(label.id), onSettle = { _, _ -> }) { _, _, dragging ->
                    ReorderableItem {
                        ChatterLabelCard(
                            label = label,
                            assignees = assignees,
                            problems = emptySet(),
                            dragging = dragging,
                            enabled = true,
                            reorderable = true,
                            searching = false,
                            onMoveUp = null,
                            onMoveDown = null,
                            onChooseIcon = {},
                            onNameChange = {},
                            onEnabledChange = {},
                            onDelete = {},
                            onChatterTap = { tapped += it.userId },
                            onChatterDelete = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Assigned to 2 chatters").performClick()

        compose.onNodeWithText("Bob").performClick()

        assertEquals(listOf("2"), tapped)
    }

    /** The moves the list settled, from and to. */
    private val settled = mutableListOf<Pair<Int, Int>>()
    private var longPressMillis = 0L

    private fun showTwoCards() {
        val labels = listOf(
            EditableChatterLabel("a", "Friends", icon = "", enabled = true),
            EditableChatterLabel("b", "Foes", icon = "", enabled = true),
        )
        compose.setContent {
            longPressMillis = LocalViewConfiguration.current.longPressTimeoutMillis
            Column(Modifier.testTag(PAGE).verticalScroll(rememberScrollState())) {
                ReorderableColumn(
                    list = labels.map { it.id },
                    onSettle = { from, to -> settled += from to to },
                ) { _, id, dragging ->
                    ReorderableItem {
                        ChatterLabelCard(
                            label = labels.first { it.id == id },
                            assignees = emptyList(),
                            problems = emptySet(),
                            dragging = dragging,
                            enabled = true,
                            reorderable = true,
                            searching = false,
                            onMoveUp = null,
                            onMoveDown = null,
                            onChooseIcon = {},
                            onNameChange = {},
                            onEnabledChange = {},
                            onDelete = {},
                            onChatterTap = {},
                            onChatterDelete = {},
                        )
                    }
                }
            }
        }
    }

    /** Puts a finger on the first card's handle, holds it for [holdMillis], and drags it down past the second card. */
    private fun dragFirstHandle(holdMillis: Long) {
        val page = compose.onNodeWithTag(PAGE)
        page.performTouchInput { down(Offset(HANDLE_X.toPx(), HANDLE_Y.toPx())) }
        page.performTouchInput { advanceEventTime(holdMillis) }
        compose.mainClock.advanceTimeBy(holdMillis)
        repeat(DRAG_STEPS) {
            page.performTouchInput { moveBy(Offset(0f, DRAG_STEP.toPx())) }
            compose.waitForIdle()
        }
        page.performTouchInput { up() }
        compose.waitForIdle()
    }

    @Test
    fun `a drag on the handle without a hold leaves the label in place`() {
        showTwoCards()

        dragFirstHandle(holdMillis = 0)

        assertEquals(emptyList<Pair<Int, Int>>(), settled)
    }

    @Test
    fun `a drag on the handle after a hold moves the label`() {
        showTwoCards()

        dragFirstHandle(holdMillis = longPressMillis + HOLD_MARGIN_MS)

        assertEquals(listOf(0 to 1), settled)
    }

    private companion object {
        const val PAGE = "page"

        /** The middle of the handle in the card's top left corner. */
        val HANDLE_X = 24.dp
        val HANDLE_Y = 40.dp

        /** Far enough down to pass the second card's middle. */
        const val DRAG_STEPS = 20
        val DRAG_STEP = 10.dp

        /** Past the long press timeout, so the hold surely counts. */
        const val HOLD_MARGIN_MS = 100L
    }
}
