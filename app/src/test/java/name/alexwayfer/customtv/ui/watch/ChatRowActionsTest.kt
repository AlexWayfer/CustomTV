package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatRowActionsTest {
    private val onSelect = {}
    private val onDismissSelection = { _: ChatRowHoldEnd -> }
    private val onReplySwipingChange = { _: Boolean -> }
    private val onReplySwipeEnd = { _: ChatSwipeRelease -> }

    private fun actions(copySwipe: Boolean = true) =
        ChatRowActions(onSelect, onDismissSelection, null, onReplySwipingChange, onReplySwipeEnd, copySwipe)

    // A new message builds the actions of every shown row again; equal actions let those rows skip recomposing.
    @Test
    fun actionsBuiltAgainFromTheSameCallbacksAreEqual() {
        assertEquals(actions(), actions())
    }

    @Test
    fun actionsWithAnotherCallbackDiffer() {
        val otherSelect =
            ChatRowActions({}, onDismissSelection, null, onReplySwipingChange, onReplySwipeEnd, copySwipe = true)
        assertNotEquals(actions(), otherSelect)
        assertNotEquals(actions(copySwipe = true), actions(copySwipe = false))
    }
}
