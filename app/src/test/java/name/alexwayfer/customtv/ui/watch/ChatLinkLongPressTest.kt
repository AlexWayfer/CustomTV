package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatLinkLongPressTest {
    @Test
    fun urlOpensTheLinkMenuWithTheFullUrl() {
        assertEquals(
            ChatLinkLongPress.LinkMenu("https://example.com/path"),
            chatLinkLongPress(LinkAnnotation.Url("https://example.com/path")),
        )
    }

    @Test
    fun authorNameSelectsTheMessage() {
        assertEquals(
            ChatLinkLongPress.SelectMessage,
            chatLinkLongPress(LinkAnnotation.Clickable(CHAT_AUTHOR_LINK_TAG, linkInteractionListener = null)),
        )
    }

    @Test
    fun otherClickableGetsNoTouchTarget() {
        assertNull(chatLinkLongPress(LinkAnnotation.Clickable("other", linkInteractionListener = null)))
    }
}
